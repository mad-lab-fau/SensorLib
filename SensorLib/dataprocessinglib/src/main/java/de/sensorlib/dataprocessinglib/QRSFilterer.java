package de.sensorlib.dataprocessinglib;

public class QRSFilterer {

    private QRSDetectorParameters qrsDetParas;

    private long lpfiltY1 = 0;
    private long lpfiltY2 = 0;
    private int[] lpfiltData;
    private int lpfiltPtr = 0;

    private long hpfiltY = 0;
    private int[] hpfiltData;
    private int hpfiltPtr = 0;

    private int[] deriv1DerBuff;
    private int deriv1DerI = 0;

    private int[] deriv2DerBuff;
    private int deriv2DerI = 0;

    private long mvwintSum = 0;
    private int[] mvwintData;
    private int mvwintPtr = 0;

    /**
     * Create a new filterer with the given parameters.
     *
     * @param qrsDetectorParameters The sampleRate-dependent parameters
     */
    public QRSFilterer(QRSDetectorParameters qrsDetectorParameters) {
        qrsDetParas = qrsDetectorParameters;
        lpfiltData = new int[qrsDetectorParameters.lpBufferLength];
        hpfiltData = new int[qrsDetectorParameters.hpBufferLength];
        deriv1DerBuff = new int[qrsDetectorParameters.derivLength];
        deriv2DerBuff = new int[qrsDetectorParameters.derivLength];
        mvwintData = new int[qrsDetectorParameters.windowWidth];
    }

    /**
     * QRSFilter() takes samples of an ECG signal as input and returns a sample of
     * a signal that is an estimate of the local energy in the QRS bandwidth.  In
     * other words, the signal has a lump in it whenever a QRS complex, or QRS
     * complex like artifact occurs.  The filters were originally designed for data
     * sampled at 200 samples per second, but they work nearly as well at sample
     * frequencies from 150 to 250 samples per second.
     *
     * @param datum sample of an ECG signal
     * @return a sample of a signal that is an estimate of the local energy in the QRS bandwidth
     */
    public int qrsFilter(int datum) {
        int fdatum;
        fdatum = lpfilt(datum);  // Low pass filter data.
        fdatum = hpfilt(fdatum); // High pass filter data.
        fdatum = deriv2(fdatum); // Take the derivative.
        fdatum = Math.abs(fdatum);    // Take the absolute value.
        fdatum = mvwInt(fdatum); // Average over an 80 ms window .
        return (fdatum);
    }

    public int ecgFilter(int datum) {
        int fdatum;
        fdatum = lpfilt(datum);
        fdatum = hpfilt(fdatum);
        return (fdatum);
    }

    /**
     * lpfilt() implements the digital filter represented by the difference equation:
     * <p>
     * y[n] = 2*y[n-1] - y[n-2] + x[n] - 2*x[t-24 ms] + x[t-48 ms]
     * <p>
     * Note that the filter delay is (LPBUFFER_LGTH/2)-1
     *
     * @param datum sample of an ECG signal
     * @return the result of the filtering
     */
    private int lpfilt(int datum) {
        long y0;
        int output;
        int halfPtr;

        halfPtr = lpfiltPtr - (qrsDetParas.lpBufferLength / 2); // Use halfPtr to index
        if (halfPtr < 0) // to x[n-6].
            halfPtr += qrsDetParas.lpBufferLength;
        y0 = (lpfiltY1 << 1) - lpfiltY2 + datum - (lpfiltData[halfPtr] << 1) + lpfiltData[lpfiltPtr];
        lpfiltY2 = lpfiltY1;
        lpfiltY1 = y0;
        output = (int) y0 / ((qrsDetParas.lpBufferLength * qrsDetParas.lpBufferLength) / 4);
        lpfiltData[lpfiltPtr] = datum; // Stick most recent sample into
        if (++lpfiltPtr == qrsDetParas.lpBufferLength) // the circular buffer and update
            lpfiltPtr = 0; // the buffer pointer.
        return (output);
    }

    /**
     * hpfilt() implements the high pass filter represented by the following difference equation:
     * <p>
     * y[n] = y[n-1] + x[n] - x[n-128 ms]
     * z[n] = x[n-64 ms] - y[n]
     * <p>
     * Filter delay is (HPBUFFER_LGTH-1)/2
     *
     * @param datum sample of an ECG signal
     * @return the result of the filtering
     */
    private int hpfilt(int datum) {
        int z;
        int halfPtr;

        hpfiltY += datum - hpfiltData[hpfiltPtr];
        halfPtr = hpfiltPtr - (qrsDetParas.hpBufferLength / 2);
        if (halfPtr < 0)
            halfPtr += qrsDetParas.hpBufferLength;
        z = (int) (hpfiltData[halfPtr] - (hpfiltY / qrsDetParas.hpBufferLength));
        hpfiltData[hpfiltPtr] = datum;
        if (++hpfiltPtr == qrsDetParas.hpBufferLength)
            hpfiltPtr = 0;
        return (z);
    }

    /**
     * deriv1 and deriv2 implement derivative approximations represented by the difference equation:
     * <p>
     * y[n] = x[n] - x[n - 10ms]
     * <p>
     * Filter delay is DERIV_LENGTH/2
     *
     * @param x sample of an ECG signal
     * @return the result of the derivative approximation
     */
    public int deriv1(int x) {
        int y;
        y = x - deriv1DerBuff[deriv1DerI];
        deriv1DerBuff[deriv1DerI] = x;
        if (++deriv1DerI == qrsDetParas.derivLength)
            deriv1DerI = 0;
        return (y);
    }

    /**
     * deriv1 and deriv2 implement derivative approximations represented by the difference equation:
     * <p>
     * y[n] = x[n] - x[n - 10ms]
     * <p>
     * Filter delay is DERIV_LENGTH/2
     *
     * @param x sample of an ECG signal
     * @return the result of the derivative approximation
     */
    private int deriv2(int x) {
        int y;
        y = x - deriv2DerBuff[deriv2DerI];
        deriv2DerBuff[deriv2DerI] = x;
        if (++deriv2DerI == qrsDetParas.derivLength)
            deriv2DerI = 0;
        return (y);
    }

    /**
     * mvwint() implements a moving window integrator.  Actually, mvwint() averages
     * the signal values over the last WINDOW_WIDTH samples.
     *
     * @param datum sample of an ECG signal
     * @return the average
     */
    private int mvwInt(int datum) {
        int output;
        mvwintSum += datum;
        mvwintSum -= mvwintData[mvwintPtr];
        mvwintData[mvwintPtr] = datum;
        if (++mvwintPtr == qrsDetParas.windowWidth)
            mvwintPtr = 0;
        if ((mvwintSum / qrsDetParas.windowWidth) > 32000)
            output = 32000;
        else
            output = (int) (mvwintSum / qrsDetParas.windowWidth);
        return (output);
    }

}

