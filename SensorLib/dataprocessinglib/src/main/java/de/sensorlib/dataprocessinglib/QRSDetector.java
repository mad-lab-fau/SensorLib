package de.sensorlib.dataprocessinglib;

import java.util.ArrayList;
import java.util.Arrays;

import de.fau.sensorlib.ProcessingEventGenerator;
import de.fau.sensorlib.ProcessingEventListener;

import static java.lang.Math.abs;

public class QRSDetector implements ProcessingEventGenerator {

    private QRSDetectorParameters qrsDetParas;
    private QRSDetectorParameters.PreBlankParameters preBlankParas;
    private QRSFilterer qrsFilterer;

    private int detThresh;
    private int qpkcnt = 0;
    private int[] qrsbuf = new int[8];
    private int[] noise = new int[8];
    private int[] rrbuf = new int[8];
    private int[] rsetBuff = new int[8];
    private int rsetCount = 0;
    private int nmedian;
    private int qmedian;
    private int rrmedian;
    private int count = 0;
    private int sbpeak = 0;
    private int sbloc;
    private int sbcount;
    private int maxder = 0;
    private int initBlank = 0;
    private int initMax = 0;
    private int preBlankCnt = 0;
    private int tempPeak;
    private int qrsDelay;
    /**
     * Buffer holding derivative data.
     */
    private int[] ddBuffer;
    private int ddPtr = 0;
    private double th = 0.475;
    private int memmovelen = 7;

    private int peakMax = 0;
    private int peakTimeSinceMax = 0;
    private int peakLastDatum;

    /**
     * variables and methods making QRSDet observable
     */
    private ArrayList<ProcessingEventListener> listeners = new ArrayList<ProcessingEventListener>();

    /**
     * This method adds new ProcessEventListeners to the list of listeners.
     *
     * @param listener This is the new ProcessEventListener to be added.
     */
    public void addProcessingEventListener(ProcessingEventListener listener) {
        listeners.add(listener);
    }

    /**
     * This method removes ProcessEventListeners from the list of listeners.
     *
     * @param listener This is the ProcessEventListener to be removed.
     */
    public void removeProcessingEventListener(ProcessingEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * This method is called when state changes. It calls the update method of all listeners of this class.
     */
    public void notifyListeners() {
        for (ProcessingEventListener listener : listeners) {
            listener.update(this);
        }
    }

    public int getQrsDelay() {
        return this.qrsDelay;
    }

    /**
     * Create a new detector with the given parameters.
     *
     * @param qrsDetectorParameters The sampleRate-dependent parameters
     */
    public QRSDetector(QRSDetectorParameters qrsDetectorParameters) {
        qrsDetParas = qrsDetectorParameters;
        preBlankParas = new QRSDetectorParameters.PreBlankParameters(qrsDetectorParameters, qrsDetectorParameters.ms200);

        sbcount = qrsDetectorParameters.ms1500;
        ddBuffer = new int[preBlankParas.derDelay];
        for (int i = 0; i < 8; ++i) {
            rrbuf[i] = qrsDetectorParameters.ms1000;/* Initialize R-to-R interval buffer. */
        }
    }

    /**
     * Injects the object.
     *
     * @param qrsFilterer The qrsFilterer
     */
    public void setObjects(QRSFilterer qrsFilterer) {
        this.qrsFilterer = qrsFilterer;
    }

    /**
     * QRSDet implements a QRS detection algorithm.
     * Consecutive ECG samples are passed to QRSDet.
     * When a QRS complex is detected QRSDet returns the detection delay.
     *
     * @param datum sample of an ECG signal
     * @return the detection delay if a QRS complex is detected
     */
    public int qrsDet(int datum) {
        int fdatum, qrsDelay = 0;
        int i, newPeak, aPeak;

        fdatum = qrsFilterer.qrsFilter(datum);    /* Filter data. */

        /* Wait until normal detector is ready before calling early detections. */

        aPeak = peak(fdatum);

        // Hold any peak that is detected for 200 ms
        // in case a bigger one comes along.  There
        // can only be one QRS complex in any 200 ms window.

        newPeak = 0;
        if (aPeak != 0 && preBlankCnt == 0)            // If there has been no peak for 200 ms
        {                                        // save this one and start counting.
            tempPeak = aPeak;
            preBlankCnt = preBlankParas.preBlank;            // MS200
        } else if (aPeak == 0 && preBlankCnt != 0)    // If we have held onto a peak for
        {                                        // 200 ms pass it on for evaluation.
            if (--preBlankCnt == 0) {
                newPeak = tempPeak;
            }
        } else if (aPeak != 0)                            // If we were holding a peak, but
        {                                        // this ones bigger, save it and
            if (aPeak > tempPeak)                // start counting to 200 ms again.
            {
                tempPeak = aPeak;
                preBlankCnt = preBlankParas.preBlank; // MS200
            } else {
                if (--preBlankCnt == 0) {
                    newPeak = tempPeak;
                }
            }
        }

      /* Save derivative of raw signal for T-wave and baseline
         shift discrimination. */

        ddBuffer[ddPtr] = qrsFilterer.deriv1(datum);
        if (++ddPtr == preBlankParas.derDelay) {
            ddPtr = 0;
        }

        /* Initialize the qrs peak buffer with the first eight     */
        /* local maximum peaks detected.                  */

        if (qpkcnt < 8) {
            ++count;
            if (newPeak > 0) {
                count = qrsDetParas.windowWidth;
            }
            if (++initBlank == qrsDetParas.ms1000) {
                initBlank = 0;
                qrsbuf[qpkcnt] = initMax;
                initMax = 0;
                ++qpkcnt;
                if (qpkcnt == 8) {
                    qmedian = median(qrsbuf, 8);
                    nmedian = 0;
                    rrmedian = qrsDetParas.ms1000;
                    sbcount = qrsDetParas.ms1500 + qrsDetParas.ms150;
                    detThresh = thresh(qmedian, nmedian);
                }
            }
            if (newPeak > initMax) {
                initMax = newPeak;
            }
            /* Else test for a qrs. */
        } else {
            ++count;
            /* Check for maximum derivative and matching minima and maxima
               for T-wave and baseline shift rejection.  Only consider this
               peak if it doesn't seem to be a base line shift. */
            if (newPeak > 0) {
                int[] maxderArray = new int[]{maxder};
                boolean result = blsCheck(ddBuffer, ddPtr, maxderArray);
                maxder = maxderArray[0];
                // Classify the beat as a QRS complex
                // if the peak is larger than the detection threshold.
                if (!result) {
                    if (newPeak > detThresh) {
                        System.arraycopy(qrsbuf, 0, qrsbuf, 1, memmovelen);
                        qrsbuf[0] = newPeak;
                        qmedian = median(qrsbuf, 8);
                        detThresh = thresh(qmedian, nmedian);
                        System.arraycopy(rrbuf, 0, rrbuf, 1, memmovelen);
                        rrbuf[0] = count - qrsDetParas.windowWidth;
                        rrmedian = median(rrbuf, 8);
                        sbcount = rrmedian + (rrmedian >> 1) + qrsDetParas.windowWidth;
                        count = qrsDetParas.windowWidth;

                        sbpeak = 0;
                        maxder = 0;
                        qrsDelay = qrsDetParas.windowWidth + preBlankParas.filterDelay;
                        initBlank = initMax = rsetCount = 0;
                    }

                    // If a peak isn't a QRS update noise buffer and estimate.
                    // Store the peak for possible search back.

                    else {
                        System.arraycopy(noise, 0, noise, 1, memmovelen);
                        noise[0] = newPeak;
                        nmedian = median(noise, 8);
                        detThresh = thresh(qmedian, nmedian);

                        // Don't include early peaks (which might be T-waves)
                        // in the search back process.  A T-wave can mask
                        // a small following QRS.

                        if ((newPeak > sbpeak) && ((count - qrsDetParas.windowWidth) >= qrsDetParas.ms360)) {
                            sbpeak = newPeak;
                            sbloc = count - qrsDetParas.windowWidth;
                        }
                    }
                }
            }

            /* Test for search back condition.  If a QRS is found in  */
            /* search back update the QRS buffer and det_thresh.      */

            if ((count > sbcount) && (sbpeak > (detThresh >> 1))) {
                System.arraycopy(qrsbuf, 0, qrsbuf, 1, memmovelen);
                qrsbuf[0] = sbpeak;
                qmedian = median(qrsbuf, 8);
                detThresh = thresh(qmedian, nmedian);
                System.arraycopy(rrbuf, 0, rrbuf, 1, memmovelen);
                rrbuf[0] = sbloc;
                rrmedian = median(rrbuf, 8);
                sbcount = rrmedian + (rrmedian >> 1) + qrsDetParas.windowWidth;
                qrsDelay = count = count - sbloc;
                qrsDelay += preBlankParas.filterDelay;
                sbpeak = 0;
                maxder = 0;
                initBlank = initMax = rsetCount = 0;
            }
        }

        // In the background estimate threshold to replace adaptive threshold
        // if eight seconds elapses without a QRS detection.

        if (qpkcnt == 8) {
            if (++initBlank == qrsDetParas.ms1000) {
                initBlank = 0;
                rsetBuff[rsetCount] = initMax;
                initMax = 0;
                ++rsetCount;

                // Reset threshold if it has been 8 seconds without
                // a detection.

                if (rsetCount == 8) {
                    for (i = 0; i < 8; ++i) {
                        qrsbuf[i] = rsetBuff[i];
                        noise[i] = 0;
                    }
                    qmedian = median(rsetBuff, 8);
                    nmedian = 0;
                    rrmedian = qrsDetParas.ms1000;
                    sbcount = qrsDetParas.ms1500 + qrsDetParas.ms150;
                    detThresh = thresh(qmedian, nmedian);
                    initBlank = initMax = rsetCount = 0;
                    sbpeak = 0;
                }
            }
            if (newPeak > initMax) {
                initMax = newPeak;
            }
        }

        // make QRSDet observable by calling notifyListeners when new QRS is detected.
        this.qrsDelay = qrsDelay;
        if (qrsDelay != 0) {
            notifyListeners();
        }

        return (qrsDelay);
    }

    /**
     * peak() takes a datum as input and returns a peak height
     * when the signal returns to half its peak height, or
     *
     * @param datum
     * @return The peak height
     */
    private int peak(int datum) {
        int pk = 0;

        if (peakTimeSinceMax > 0) {
            peakTimeSinceMax++;
        }

        if ((datum > peakLastDatum) && (datum > peakMax)) {
            peakMax = datum;
            if (peakMax > 2) {
                peakTimeSinceMax = 1;
            }
        } else if (datum < (peakMax >> 1)) {
            pk = peakMax;
            peakMax = 0;
            peakTimeSinceMax = 0;
        } else if (peakTimeSinceMax > qrsDetParas.ms95) {
            pk = peakMax;
            peakMax = 0;
            peakTimeSinceMax = 0;
        }
        peakLastDatum = datum;
        return (pk);
    }

    /**
     * median returns the median of an array of integers.  It uses a slow
     * sort algorithm, but these arrays are small, so it hardly matters.
     *
     * @param array
     * @param datnum
     * @return The median
     */
    private int median(int[] array, int datnum) {
        int i, j, k, temp;
        int[] sort = new int[20];
        for (i = 0; i < datnum; ++i)
            sort[i] = array[i];
        for (i = 0; i < datnum; ++i) {
            temp = sort[i];
            for (j = 0; (temp < sort[j]) && (j < i); ++j) ;
            for (k = i - 1; k >= j; --k)
                sort[k + 1] = sort[k];
            sort[j] = temp;
        }
        return (sort[datnum >> 1]);
    }

    /**
     * thresh() calculates the detection threshold from the qrs median and noise
     * median estimates.
     *
     * @param qmedian
     * @param nmedian
     * @return The detection threshold
     */
    private int thresh(int qmedian, int nmedian) {
        int thrsh, dmed;
        double temp;
        dmed = qmedian - nmedian;
        temp = dmed;
        temp *= th;
        dmed = (int) temp;
        thrsh = nmedian + dmed; /* dmed * THRESHOLD */
        return (thrsh);
    }

    /**
     * BLSCheck() reviews data to see if a baseline shift has occurred.
     * This is done by looking for both positive and negative slopes of
     * roughly the same magnitude in a 220 ms window.
     *
     * @param dBuf
     * @param dbPtr
     * @param maxder
     * @return If a baseline shift occured
     */
    private boolean blsCheck(int[] dBuf, int dbPtr, int[] maxder) {
        /** T-wave check**/
        int[] orderedArray = new int[dBuf.length];
        int c = 0;
        for (int i = dbPtr; i >= 0; i--) {
            orderedArray[c] = dBuf[i];
            c++;
        }
        for (int j = dBuf.length - 1; j > dbPtr; j--) {
            orderedArray[c] = dBuf[j];
            c++;
        }
        // save the samples of the last 360ms in maxder[]
        int[] orderedArray360 = new int[qrsDetParas.ms360];
        for (int k = 0; k < qrsDetParas.ms360; k++) {
            orderedArray360[k] = orderedArray[k];
        }
        // sort maxder[] in ascending order
        Arrays.sort(orderedArray360);

        boolean tWave;
        if (maxder[0] != orderedArray360[orderedArray360.length - 1]) { //time interval is larger than 360ms
            tWave = false;
            maxder[0] = orderedArray360[orderedArray360.length - 1];
        } else {
            //search for maximum in the last 220ms
            int max220 = orderedArray[0];
            for (int k = 0; k < qrsDetParas.ms220; k++) {
                if (orderedArray[k] > max220) {
                    max220 = orderedArray[k];
                }
            }
            if ((max220 / 2) < maxder[0]) {  //half of derivative smaller than max derivative of previous QRS ->TWave
                tWave = true;
            } else {
                tWave = false;
                maxder[0] = max220;
            }

        }

        /** BaseLineShift detection**/
        int max220 = orderedArray[0];
        int min220 = orderedArray[0];
        for (int k = 0; k < qrsDetParas.ms220; k++) {
            if (orderedArray[k] > max220) {
                max220 = orderedArray[k];
            }
            if (orderedArray[k] < min220) {
                min220 = orderedArray[k];
            }
        }
        boolean bls;
        //max and min derivative within the past 220ms is similar -> no BLS
        if (abs(min220 + max220) < 1000) {
            bls = false;
        } else {
            bls = true;
        }
        return (bls || tWave);  //QRS possible if no BLS and no TWave


    }

}
