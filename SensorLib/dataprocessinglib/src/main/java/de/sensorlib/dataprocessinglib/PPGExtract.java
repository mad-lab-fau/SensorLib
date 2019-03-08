package de.sensorlib.dataprocessinglib;

import java.util.ArrayList;

public class PPGExtract {

    private ArrayList<Integer> ppgSequence;

    /**
     * PPGExtract saves the PPG data of one RR-interval in ArrayLists
     */

    public PPGExtract(ArrayList<Integer> ppgExtract) {
        this.ppgSequence = ppgExtract;
    }

    public PPGExtract() {
        this.ppgSequence = new ArrayList<Integer>();
    }

    /**
     * This method returns the PPG data stored in PPGSequence (between two R-Peaks)
     *
     * @return
     */
    public ArrayList<Integer> getPPGSequence() {
        return this.ppgSequence;
    }

    /**
     * AddSample adds a new PPG sample to the PPGSequence
     *
     * @param datum This is a sample of PPG data.
     */
    public void addSample(int datum) {
        this.ppgSequence.add(datum);
    }

    public void clearPPGExtract() {
        this.ppgSequence.clear();
    }

    /**
     * RemoveQRSDelay removes as many samples at the end of PPGSequence as QRSDelay is long. Those samples are stored in an ArrayList that is returned.
     *
     * @param qrsDelay This is the QRSDelay detected by the QRSDetector.
     * @return This is the Arraylist of PPGsamples stored after the second R-Peak.
     */
    public ArrayList<Integer> removeQRSDelay(int qrsDelay) {
        ArrayList<Integer> nextPPGExtract = new ArrayList(this.ppgSequence.subList((this.ppgSequence).size() - qrsDelay, this.ppgSequence.size() - 1));
        this.ppgSequence = new ArrayList(this.ppgSequence.subList(0, (this.ppgSequence).size() - qrsDelay));
        return nextPPGExtract;
    }


}
