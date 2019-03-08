package de.sensorlib.dataprocessinglib;

import java.util.ArrayList;

import de.fau.sensorlib.ProcessingEventGenerator;
import de.fau.sensorlib.ProcessingEventListener;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.PpgDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensors.NilsPodPpgSensor.NilsPodPpgDataFrame;
import de.fau.sensorlib.sensors.NilsPodEcgSensor.NilsPodEcgDataFrame;
import de.fau.sensorlib.sensors.SimulatedEcgSensor;
import de.fau.sensorlib.sensors.SimulatedEcgSensor.SimulatedEcgDataFrame;
import de.fau.sensorlib.sensors.SimulatedPpgSensor;
import de.fau.sensorlib.sensors.SimulatedPpgSensor.SimulatedPpgDataFrame;
import de.fau.sensorlib.widgets.SensorPlotter;

/**
 * The class ComputePAT computes the pulse arrival time (PAT) for a given ECG and PPG signal and their sampling rate
 */

public class ComputePAT implements ProcessingEventListener, ProcessingEventGenerator {

    private int samplingRate;
    private QRSFilterer qrsFilterer;
    private QRSDetector qrsDetector;
    private QRSDetectorParameters qrsDetectorParameters;
    private PPGFilterer ppgFilterer;
    private PPGExtract ppgExtract;

    private PPGExtract tempPpgExtract;

    private int qrsDelay;
    private ArrayList<Integer> nextPPGExtract;
    private PPGPointDetector ppgPointDetector;
    private int[] detectedPoints;
    private double[] onsetPoint;
    private double pat;

    private int ppgDatum;
    private int ecgDatum;
    private boolean ppgDataReceived = false;
    private boolean ecgDataReceived = false;
    private boolean ppgAndEcgDataReceived = false;

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
            listener.update(this);
        }
    }

    //only for plotting
    private ArrayList<Integer> qrsDelayList;
    private ArrayList<Integer> ppgPeakDelay;
    private ArrayList<Integer> ppgTurnDelay;
    private ArrayList<Integer> ppgThroughDelay;
    private ArrayList<Double> ppgOnsetDelay;
    private ArrayList<Double> ppgOnsetValue;


    /**
     * ComputePAT ist the constructor of an object of ComputePAT. It initializes the ECG and PPG Filterer and the Point detectors in the ECG and PPG Signal
     *
     * @param samplingRate This is the sampling rate of the given PPG and ECG signals
     */
    public ComputePAT(int samplingRate) {
        this.samplingRate = samplingRate;
        this.qrsDetectorParameters = new QRSDetectorParameters(samplingRate);
        this.qrsFilterer = new QRSFilterer(qrsDetectorParameters);
        this.qrsDetector = new QRSDetector(qrsDetectorParameters);
        qrsDetector.setObjects(qrsFilterer);
        this.ppgFilterer = new PPGFilterer(samplingRate);
        this.ppgExtract = new PPGExtract();

        this.nextPPGExtract = new ArrayList<Integer>();
        this.ppgPointDetector = new PPGPointDetector(ppgFilterer, ppgExtract);
        // add this object to the Lists of ProcessEventListeners of qrsDetector and ppgPointDetector -> observable
        this.qrsDetector.addProcessingEventListener(this);
        this.ppgPointDetector.addProcessingEventListener(this);

        //only for Plotting
        this.qrsDelayList = new ArrayList<Integer>();
        this.ppgPeakDelay = new ArrayList<Integer>();
        this.ppgTurnDelay = new ArrayList<Integer>();
        this.ppgThroughDelay = new ArrayList<Integer>();
        this.ppgOnsetDelay = new ArrayList<Double>();
        this.ppgOnsetValue = new ArrayList<Double>();
    }

    /**
     * getter methods for the arrays of detected points for plotting them
     *
     * @return
     */
    public ArrayList<Integer> getQrsDelay() {
        return this.qrsDelayList;
    }

    public ArrayList<Integer> getPpgPeakDelay() {
        return this.ppgPeakDelay;
    }

    public ArrayList<Integer> getPpgTurnDelay() {
        return this.ppgTurnDelay;
    }

    public ArrayList<Integer> getPpgThroughDelay() {
        return this.ppgThroughDelay;
    }

    public ArrayList<Double> getPpgOnsetDelay() {
        return this.ppgOnsetDelay;
    }

    public ArrayList<Double> getPpgOnsetValue() {
        return this.ppgOnsetValue;
    }


    /**
     * update method making ComputePat Listener
     * either calls ppgPointDetection if update is called by new QRS detected, or calls onsetDetector if update is called by PPGPointDetector
     */

    public void update(ProcessingEventGenerator generator) {
        if (generator instanceof QRSDetector) {
            this.qrsDelay = qrsDetector.getQrsDelay();
            this.nextPPGExtract = this.ppgExtract.removeQRSDelay(this.qrsDelay);
            this.tempPpgExtract = new PPGExtract(this.ppgExtract.getPPGSequence());
            this.ppgExtract = new PPGExtract(nextPPGExtract);
            this.ppgPointDetector = new PPGPointDetector(ppgFilterer, tempPpgExtract);
            this.ppgPointDetector.addProcessingEventListener(this);
            this.detectedPoints = ppgPointDetector.pointDetection();

        } else if (generator instanceof PPGPointDetector) {

            int turn = ppgPointDetector.getTurn();
            int through = ppgPointDetector.getThrough();
            int peak = ppgPointDetector.getPeak();
            System.out.println("turn, through, peak: " + turn + ", " + through + ", " + peak);
            this.onsetPoint = ppgPointDetector.onsetDetection(turn, through);
            this.pat = (onsetPoint[0] / this.samplingRate) * 1000;
            System.out.println("onsetPoint = " + pat + "ms");

            //only for plotting
            this.qrsDelayList.add(this.qrsDelay);
            this.ppgPeakDelay.add(peak + qrsDelay);
            this.ppgTurnDelay.add(turn + qrsDelay);
            this.ppgThroughDelay.add(through + qrsDelay);
            this.ppgOnsetDelay.add(onsetPoint[1] + qrsDelay);
            this.ppgOnsetValue.add(onsetPoint[2]);

            // for plotting in the App
            notifyListeners();
            //mPlotter.onNewOnsetPoint(this.qrsDelay, peak+qrsDelay, turn+qrsDelay, through+qrsDelay, onsetPoint[1]+qrsDelay, onsetPoint[2]);

        }
    }

    /**
     * This is called when a new ECG dataframe is detected in OnNewData in BleService
     * It stores the new ECG sample and calls the compute method of computePat, if ECG and PPG sample are received
     *
     * @param data ECG Dataframe
     */
    public void onNewEcgData(SensorDataFrame data) {
        if (data instanceof EcgDataFrame) {
            this.ecgDatum = (int) ((EcgDataFrame) data).getEcgSample();
            ecgDataReceived = true;
        }
        if (data instanceof SimulatedEcgDataFrame) {
            this.ecgDatum = (int) ((SimulatedEcgDataFrame) data).getEcgSample();
            ecgDataReceived = true;
        }
        if (data instanceof NilsPodEcgDataFrame) {
            this.ecgDatum = (int) ((NilsPodEcgDataFrame) data).getEcgSample();
            ecgDataReceived = true;
        }
        ppgAndEcgDataReceived = ecgDataReceived && ppgDataReceived;
        if (ppgAndEcgDataReceived) {
            ecgDataReceived = ppgDataReceived = ppgAndEcgDataReceived = false;
            compute(this.ecgDatum, this.ppgDatum);
        }

    }

    /**
     * This is called when a new PPG dataframe is detected in OnNewData in BleService
     * It stores the new PPG sample and calls the compute method of computePat, if ECG and PPG sample are received
     *
     * @param data PPG Dataframe
     */
    public void onNewPpgData(SensorDataFrame data) {
        if (data instanceof PpgDataFrame) {
            this.ppgDatum = (int) ((PpgDataFrame) data).getPpgRedSample();
            ppgDataReceived = true;
        }
        if (data instanceof SimulatedPpgDataFrame) {
            this.ppgDatum = (int) ((SimulatedPpgDataFrame) data).getPpgRedSample();
            ppgDataReceived = true;
        }
        if (data instanceof NilsPodPpgDataFrame) {
            this.ppgDatum = (int) ((NilsPodPpgDataFrame) data).getPpgRedSample();
            ppgDataReceived = true;
        }

        ppgAndEcgDataReceived = ecgDataReceived && ppgDataReceived;
        if (ppgAndEcgDataReceived) {
            ecgDataReceived = ppgDataReceived = ppgAndEcgDataReceived = false;
            compute(this.ecgDatum, this.ppgDatum);
        }
    }

    /**
     * Compute computes the PAT (pulse arrival time) for corresponding ECG and PPG Signals, it is called when both (ECG and PPG sample) are received
     *
     * @param ecgDatum This is one sample of the ECG signal.
     * @param ppgDatum This is the corresponding sample of the PPG signal.
     * @return This method returns the PAT in ms, after an R-Peak in the ECG signal was found. Until next PAT can be computed return value is zero.
     */
    public double compute(int ecgDatum, int ppgDatum) {
        // R-Peak detection in the ECG-Signal
        this.qrsDelay = qrsDetector.qrsDet(ecgDatum);

        // if no R-Peak detected add the next PPG sample to ppgExtract, that stores the PPG data between two R-Peaks
        // if new QRS detected, update method is called
        if (qrsDelay == 0) {
            ppgExtract.addSample(ppgDatum);

            //only for Plotting
            this.qrsDelayList.add(qrsDelay);
            this.ppgPeakDelay.add(0);
            this.ppgTurnDelay.add(0);
            this.ppgThroughDelay.add(0);
            this.ppgOnsetDelay.add((double) 0);
            this.ppgOnsetValue.add((double) 0);
        }
        return pat;
    }

}
