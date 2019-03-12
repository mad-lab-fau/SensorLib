package de.sensorlib.dataprocessinglib.Detectors;

import java.util.ArrayList;
import java.util.Collections;

import de.sensorlib.dataprocessinglib.PPGExtract;
import de.sensorlib.dataprocessinglib.ProcessingEventGenerator;
import de.sensorlib.dataprocessinglib.ProcessingEventListener;
import de.sensorlib.dataprocessinglib.enums.ProcessingState;
import de.sensorlib.dataprocessinglib.filters.AbstractFilterer;
import de.sensorlib.dataprocessinglib.filters.PPGFilterer;

public class PPGPointDetector implements ProcessingEventGenerator {

    //private PPGFilterer ppgFilterer;
    private AbstractFilterer ppgFilterer;
    private PPGExtract ppgExtract;
    private ArrayList<Integer> ppgFilteredExtract = new ArrayList<Integer>();
    private ArrayList<Integer> ppgDeriv1Extract = new ArrayList<Integer>();

    private ProcessingState mState;

    private int peak;
    private int turn;
    private int through;

    public int getPeak() {
        return this.peak;
    }

    public int getTurn() {
        return this.turn;
    }

    public int getThrough() {
        return this.through;
    }

    /**
     * variables and method making PPGPointDetector observable
     */
    // listeners stores all listeners of PPGPointDetector.
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
            //listener.onNewProcessingEvent(this);
            listener.onNewProcessingEvent(mState);
        }
        mState = ProcessingState.NO_EVENT_DETECTED;
    }

    /**
     * This constructor initializes the PPG Filterer and the PPGSequence between two R-Peaks and computes the fildered PPGSequence and the derivative of the PPGSequence
     *
     * @param ppgFilterer
     * @param ppgExtract
     */
    public PPGPointDetector(AbstractFilterer ppgFilterer, PPGExtract ppgExtract) {
        this.ppgFilterer = ppgFilterer;
        this.ppgExtract = ppgExtract;
        for (int i = 0; i < ppgExtract.getPPGSequence().size(); i++) {
            int filtered = ppgFilterer.filter(ppgExtract.getPPGSequence().get(i));
            this.ppgFilteredExtract.add(filtered);

        }
        for (int i = 0; i < ppgFilteredExtract.size(); i++) {
            int deriv1 = ppgFilterer.deriv1(ppgFilteredExtract.get(i));
            this.ppgDeriv1Extract.add(deriv1);
        }

    }


    /**
     * PointDetection implements an algorithm for the detection of the turn and through of the PPG signal between two R-peaks
     *
     * @return array of the peak[0], the turn of PPG[1] and the through of PPG[2].
     * The values are the sample delays seen from the end of the PPG Sequence (second R-Peak).
     * If no detection the return values are zero.
     */

    public int[] pointDetection() {

        int turn = 0;
        int through = 0;

        // Peak of PPG is max Value of PPG-Extract
        int ppgPeak = this.ppgFilteredExtract.indexOf(Collections.max(this.ppgFilteredExtract));

        // Turn of PPG is max derivative value of PPG Extract before Peak
        int ppgTurnValue = this.ppgDeriv1Extract.get(0);
        int ppgTurn = 0;
        for (int i = 0; i < ppgPeak; i++) {
            if (this.ppgDeriv1Extract.get(i) > ppgTurnValue) {
                ppgTurnValue = this.ppgDeriv1Extract.get(i);
                ppgTurn = i;
            }
        }
        // ppgTurn is position of the maximal gradient pf the PPG Signal in the RR-interval
        //int ppgTurn = this.ppgDeriv1Extract.indexOf(Collections.max(this.ppgDeriv1Extract));

        // ppgExtrema is the position of the first sign change of the gradient
        int ppgExtrema = ppgTurn;
        Integer[] ppgDeriv1Array = new Integer[this.ppgDeriv1Extract.size()];
        ppgDeriv1Array = this.ppgDeriv1Extract.toArray(ppgDeriv1Array);
        for (int i = 0; i < ppgDeriv1Array.length; i++) {
            ppgDeriv1Array[i] = (int) Math.signum((float) ppgDeriv1Array[i]);
        }
        for (int i = 0; i < ppgTurn; i++) { //take last Extrema before turning point
            int dif = ppgDeriv1Array[i + 1] + ppgDeriv1Array[i];
            if (dif == 0 || dif == -1 || dif == 1) {
                ppgExtrema = i;
            }
        }


        // skip this peak for onset detection if no through can be found
        if (ppgExtrema < ppgTurn) {
            through = ppgExtrema;
            turn = ppgTurn;
        }

        // save the number of samples regarded from the end of the PPG-sequence (samples before R-peak)
        if (ppgPeak != 0) {
            ppgPeak = this.ppgExtract.getPPGSequence().size() - 1 - ppgPeak;
            this.peak = ppgPeak;
        }
        if (turn != 0) {
            turn = this.ppgExtract.getPPGSequence().size() - 1 - turn;
            this.turn = turn;
        }
        if (through != 0) {
            through = this.ppgExtract.getPPGSequence().size() - 1 - through;
            this.through = through;
        }

        // call update method if new points are detected.
        if (ppgPeak != 0 && turn != 0 && through != 0) {
            mState = ProcessingState.PPG_POINTS_DETECTED;
            this.notifyListeners();
        }

        int[] result = {ppgPeak, turn, through};
        return result;

    }

    /**
     * Onset detection calculates the position of the onset point of the PPG.
     * It is the cross point of the tangents in turn and through.
     *
     * @param turn    turning point of the PPG sequence between two ECG R-Peaks.
     * @param through through point of the same PPG sequence.
     * @return onset, defined as the position of crossing point of tangents in turn and through (number of samples to onset point from first R-Peak of the PPG-Sequence),
     * onsetDelay and the interpolated PPG value an the onset point (only for plotting)
     */

    public double[] onsetDetection(int turn, int through) {
        double onset = 0.0;
        int turnDeriv = ppgDeriv1Extract.get(ppgDeriv1Extract.size() - 1 - turn);
        int turnValue = ppgFilteredExtract.get(ppgFilteredExtract.size() - 1 - turn);
        int throughValue = ppgFilteredExtract.get(ppgFilteredExtract.size() - 1 - through);
        int turnidx = ppgFilteredExtract.size() - turn - 1;

        // onset is crossing point of the tangente in through and turn
        onset = (throughValue - turnValue) / turnDeriv + turnidx;
        double onsetDelay = this.ppgExtract.getPPGSequence().size() - onset;
        double onsetValue = this.interpolateOnset(onset);
        double[] result = new double[]{onset, onsetDelay, onsetValue};

        return result;
    }

    /**
     * InterpolateOnset makes a linear interpolation of the PPG value at the onset point
     *
     * @param onset This is the position of the onset point in the PPGExtract.
     * @return The interpolated PPG value at the onset point.
     */
    public double interpolateOnset(double onset) {
        int low = (int) onset;
        int high = (int) onset + 1;
        double ppgInterpol = ppgFilteredExtract.get(low) * (high - onset) / (high - low) + ppgFilteredExtract.get(high) * (onset - low) / (high - low);
        double onsetValue = ppgInterpol;
        return onsetValue;
    }


}
