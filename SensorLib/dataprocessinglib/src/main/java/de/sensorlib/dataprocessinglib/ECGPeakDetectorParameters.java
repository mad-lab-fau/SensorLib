package de.sensorlib.dataprocessinglib;

public class ECGPeakDetectorParameters {
    /**
     * Sample rate in Hz.
     */
    public int sampleRate;
    public double msPerSample;
    public int ms10;
    public int ms25;
    public int ms30;
    public int ms80;
    public int ms95;
    public int ms100;
    public int ms125;
    public int ms150;
    public int ms160;
    public int ms175;
    public int ms195;
    public int ms200;
    public int ms220;
    public int ms250;
    public int ms300;
    public int ms360;
    public int ms450;
    public int ms1000;
    public int ms1500;
    public int derivLength;
    public int lpBufferLength;
    public int hpBufferLength;
    /**
     * Moving window integration width.
     */
    public final int windowWidth; //

    public ECGPeakDetectorParameters(int sampleRate) {
        this.sampleRate = sampleRate;
        msPerSample = ((double) 1000 / (double) sampleRate);
        ms10 = ((int) (10 / msPerSample + 0.5));
        ms25 = ((int) (25 / msPerSample + 0.5));
        ms30 = ((int) (30 / msPerSample + 0.5));
        ms80 = ((int) (80 / msPerSample + 0.5));
        ms95 = ((int) (95 / msPerSample + 0.5));
        ms100 = ((int) (100 / msPerSample + 0.5));
        ms125 = ((int) (125 / msPerSample + 0.5));
        ms150 = ((int) (150 / msPerSample + 0.5));
        ms160 = ((int) (160 / msPerSample + 0.5));
        ms175 = ((int) (175 / msPerSample + 0.5));
        ms195 = ((int) (195 / msPerSample + 0.5));
        ms200 = ((int) (200 / msPerSample + 0.5));
        ms220 = ((int) (220 / msPerSample + 0.5));
        ms250 = ((int) (250 / msPerSample + 0.5));
        ms300 = ((int) (300 / msPerSample + 0.5));
        ms360 = ((int) (360 / msPerSample + 0.5));
        ms450 = ((int) (450 / msPerSample + 0.5));
        ms1000 = this.sampleRate;
        ms1500 = ((int) (1500 / msPerSample));
        derivLength = ms10;
        lpBufferLength = ((int) (2 * ms25));
        hpBufferLength = ms125;
        windowWidth = ms80;
    }

    public static class PreBlankParameters {
        public int preBlank;
        /**
         * filter delays plus pre blanking delay
         */
        public int filterDelay;
        public int derDelay;

        public PreBlankParameters(ECGPeakDetectorParameters qrsDetParas, int preBlank) {
            this.preBlank = preBlank;
            filterDelay = (int) (((double) qrsDetParas.derivLength / 2) + ((double) qrsDetParas.lpBufferLength / 2 - 1) + (((double) qrsDetParas.hpBufferLength - 1) / 2) + preBlank);
            derDelay = qrsDetParas.windowWidth + filterDelay + qrsDetParas.ms100;
        }
    }

}
