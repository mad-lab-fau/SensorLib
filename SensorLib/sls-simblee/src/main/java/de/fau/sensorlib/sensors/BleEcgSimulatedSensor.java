package de.fau.sensorlib.sensors;

import android.content.Context;
import android.os.DeadObjectException;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SimulatedSensor;
import de.fau.shiftlist.ShiftListDouble;
import de.fau.shiftlist.ShiftListLong;

/**
 * Sensor simulator that simulates both pre-recorded DailyHeart ECG data or data
 * from the MIT-BIH-Arrhythmia database.
 */
public class BleEcgSimulatedSensor extends SimulatedSensor {

    /**
     * Line separator
     */
    private static final char mSeparator = '\n';
    private static final String mHeader = "samplingrate";

    /**
     * Signal simulating MIT-BIH files
     */
    private static WfDbEcgSignal mSimSignal;
    /**
     * MIT-BIH signal filename
     */
    private String mSigFileName;
    /**
     * MIT-BIH annotation filename
     */
    private String mAnnFileName;
    /**
     * MIT-BIH signal file
     */
    private File mSigFile;
    /**
     * MIT-BIH annotation file
     */
    private File mAnnFile;
    private int mSimPlotIter;


    /**
     * Simulator Type
     */
    public enum Simulator {
        DAILYHEART,
        MIT_BIH
    }

    private Simulator mSimType;


    public BleEcgSimulatedSensor(Context context, String deviceName, SensorDataProcessor dataHandler, String fileName, Simulator simType, double samplingRate, boolean liveMode) {
        super(context, deviceName, dataHandler, fileName, samplingRate, liveMode);
        mSigFileName = fileName;
        mSimType = simType;
    }

    @Override
    public boolean connect() throws Exception {
        sendConnecting();
        boolean ret = false;
        switch (mSimType) {
            case DAILYHEART:
                ret = connectDailyHeart();
                break;
            case MIT_BIH:
                ret = connectMitBih();
                break;
        }
        if (ret) {
            sendConnected();
        }
        return ret && super.connect();
    }

    /**
     * connect DailyHeart simulator
     */
    private boolean connectDailyHeart() throws Exception {
        String line;

        if (getState() != SensorState.CONNECTING) {
            return false;
        }

        // reset BufferedReader
        try {
            mBufferedReader.reset();
        } catch (Exception e) {
            Log.e(TAG, "Cannot reset state of BufferedReader in connect().", e);
            return false;
        }

        // compute sampling rate from second line
        try {
            line = mBufferedReader.readLine();
            Log.d(TAG, "first line: " + line);
            Log.d(TAG, "header: " + mHeader);

            // check syntax
            if (line.contains(mHeader)) {
                mSamplingRate = Double.parseDouble(line.substring(mHeader.length()));
                setSamplingRate(mSamplingRate);
            } else {
                Log.e(TAG, "Cannot read sampling rate from chosen file!");
                Log.e(TAG, "Header 1 is wrong or missing.");
                sendNotification("Invalid file format!");
                disconnect();
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "Cannot read sampling rate from chosen file!");
            e.printStackTrace();
            disconnect();
            return false;
        }

        return true;
    }

    private boolean connectMitBih() {
        // load database record in seperate thread
        simThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // get annotation filename
                mAnnFileName = mSigFileName.replace("sig", "ann");

                mSigFile = new File(mSigFileName);
                mAnnFile = new File(mAnnFileName);

                // load MIT-BIH Database record
                Log.d(TAG, "MIT-BIH: Loading..." + mSigFileName + ", " + mAnnFileName);

                // load data
                mSimSignal = new WfDbEcgSignal();

                if (mSimSignal.load(mSigFile.getAbsolutePath(), mAnnFile.getAbsolutePath(),
                        0, 2) != SensorState.CONNECTED) {
                    Log.e(TAG, "Error loading MIT-BIH sim file!");
                    return;
                }
                mSimPlotIter = 0;
            }

        });
        simThread.start();
        return true;
    }

    @Override
    protected void transmitData() {
        switch (mSimType) {
            case DAILYHEART:
                transmitDailyHeartData();
                break;
            case MIT_BIH:
                transmitMitBihData();
                break;
        }
    }

    private void transmitDailyHeartData() {

        int samplingInterval = (int) (1000d / this.getSamplingRate());
        Log.d(TAG, "interval: " + samplingInterval + ", samplingrate: " + getSamplingRate());

        try {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(mSeparator);
            String line;
            // check if line contains recorded values
            int c = 0;
            int timeStamp = 0;
            while ((line = mBufferedReader.readLine()) != null) {
                // file contains values
                if (c == 0) {
                    c++;
                }

                // new instance
                splitter.setString(line);
                int time = 0;
                double ecg = 0.0;
                if (splitter.hasNext()) {
                    time = timeStamp++;
                }
                if (splitter.hasNext()) {
                    ecg = Double.parseDouble(splitter.next());
                }
                BleEcgSensor.BleEcgDataFrame data = new BleEcgSensor.BleEcgDataFrame(this, time, ecg);

                // send message
                if (getState() == SensorState.STREAMING) {
                    sendNewData(data);
                }

                Thread.sleep(samplingInterval);

                if (Thread.interrupted()) {
                    Log.e(TAG, "Thread interrupted!");
                    return;
                }
            }
            sendNotification("Record ended.");
            stopStreaming();
        } catch (Exception e) {
            Log.e(TAG, "Error during data reading", e);
        }
    }

    private void transmitMitBihData() {
        try {
            // iterates through all values
            for (mSimPlotIter = 0; mSimPlotIter < mSimSignal.mValueList.getMaxSize(); mSimPlotIter++) {
                // new instance
                BleEcgSensor.BleEcgDataFrame data =
                        new BleEcgSensor.BleEcgDataFrame(this, mSimSignal.mTimeList.get(mSimPlotIter),
                                mSimSignal.mValueList.get(mSimPlotIter),
                                (char) mSimSignal.labels.get(mSimSignal.mTimeList.get(mSimPlotIter).intValue()));
                sendNewData(data);
                Thread.sleep(2, 777777);
            }
            sendNotification("MSG_SIM_FILE_ENDED");
        } catch (Exception e) {
            Log.e(TAG, "Error receiving MIT-BIH data!", e);
            if (DeadObjectException.class.isInstance(e)) {
                Thread.currentThread().interrupt();
                mSimSignal = null;
            }
        }
    }

    public class WfDbEcgSignal {

        public float sampleInterval;
        public ShiftListDouble mValueList;
        //public FloatValueList mValueList;
        //public LongValueList mTimeList;
        public ShiftListLong mTimeList;
        public String recordName;
        public SparseIntArray labels = new SparseIntArray(8192);

        /**
         * Loads a WFDB ECG csv converted signal and annotation file
         *
         * @param signalFile The CSV-converted signal file.
         * @param annFile    The CSV-converted annotation file.
         * @param maxSamples The maximal number of samples to load. Setting
         *                   this to <code>0</code> loads all samples
         * @param leadColumn The columns number (starting at 1)
         *                   of the desired lead
         * @return The current sensor state
         */
        public SensorState load(String signalFile, String annFile, int maxSamples,
                                int leadColumn) {
            try {
                File fs = new File(signalFile);
                File fa = new File(annFile);

                float val;
                long time;
                int sample = 0;
                int currentColumn;
                ArrayList<Float> vals;
                ArrayList<Long> times;
                BufferedReader reader;
                long size;
                int counter = 0;

                sampleInterval = -1;

                reader = new BufferedReader(new FileReader(fs));
                // estimate the sizes
                size = fs.length() >> 3;

                recordName = signalFile;

                // preallocate value arrays
                vals = new ArrayList<>((int) size);
                times = new ArrayList<>((int) size);

                // initialize string (line) splitter
                TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
                String line;

                // skip header lines
                reader.readLine();

                // read sampling interval
                line = reader.readLine();
                splitter.setString(line);
                if (splitter.hasNext()) {
                    String str = splitter.next();
                    if (str != null && str.startsWith("'")) {
                        int i = str.indexOf(' ');
                        if (i != 1) {
                            str = str.substring(1, i);
                            sampleInterval = Float.parseFloat(str);
                        }
                    }
                }

                // if invalid, reset to default
                if (sampleInterval < 0) {
                    sampleInterval = 0.00277778f;
                }

                /////////// read lines
                while ((line = reader.readLine()) != null) {

                    // split
                    splitter.setString(line);

                    currentColumn = 1;
                    for (String split : splitter) {

                        if (currentColumn == 1) {
                            time = Long.parseLong(split);
                            times.add(time);
                        }

                        if (currentColumn == leadColumn) {
                            val = Float.parseFloat(split);

                            vals.add(val);
                            counter++;
                            //Log.d("val", String.valueOf(val));
                        }

                        currentColumn++;
                    }
                    if (maxSamples > 0) {
                        if (counter >= maxSamples)
                            break;
                    }

                }

                //mValueList = new FloatValueList(vals.size());
                mValueList = new ShiftListDouble(vals);
                //mValueList.copy(vals);

                //mTimeList = new LongValueList(times.size(), true);
                //mTimeList.copy(times);
                mTimeList = new ShiftListLong(times);

                reader.close();

                // load annotations
                reader = new BufferedReader(new FileReader(fa));

                // initialize string (line) buffer
                splitter = new TextUtils.SimpleStringSplitter(' ');

                // skip header lines
                reader.readLine();

                // read lines
                while ((line = reader.readLine()) != null) {
                    // split
                    splitter.setString(line);

                    // iterate columns, 2nd is sample #, 3rd is Type
                    currentColumn = 1;

                    for (String split : splitter) {
                        if (split.length() < 1) {
                            continue;
                        }

                        if (currentColumn == 2) {
                            sample = Integer.parseInt(split.trim());
                        }

                        if (currentColumn == 3) {
                            char tmp = split.trim().charAt(0);
                            labels.append(sample, (int) tmp);
                        }

                        currentColumn++;
                    }
                }

                reader.close();

            } catch (Exception e) {
                e.printStackTrace();
                return SensorState.UNDEFINED;
            }

            return SensorState.CONNECTED;
        }
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        // Overrides hardcoded value from KnownSensor
        return EnumSet.of(HardwareSensor.ECG);
    }
}
