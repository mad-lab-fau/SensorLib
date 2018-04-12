package de.fau.sensorlib.sensors;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.enums.SensorState;

public class SimbleeSimulatedSensor extends SimulatedSensor {

    private static final String TAG = SimbleeSimulatedSensor.class.getSimpleName();

    private static final char mSeparator = ',';

    public SimbleeSimulatedSensor(Context context, String deviceName, String fileName, SensorDataProcessor dataHandler) {
        super(context, deviceName, fileName, dataHandler, 250, true);
    }

    public SimbleeSimulatedSensor(Context context, SensorInfo sensorInfo, SensorDataProcessor dataHandler) {
        this(context, sensorInfo.getName(), sensorInfo.getDeviceAddress(), dataHandler);
    }

    @Override
    public boolean connect() throws Exception {
        super.connect();
        boolean ret = prepareReader();
        if (ret) {
            sendConnected();
        }
        return ret;
    }

    private boolean prepareReader() {
        // read header
        try {
            // TODO verify that first 2 lines are correct and extract sampling rate and enabled sensors from header
            mBufferedReader.readLine();
            mBufferedReader.readLine();

        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    protected void transmitData() {
        int samplingInterval = (int) (1000 / getSamplingRate());
        try {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(mSeparator);
            String line;
            while ((line = mBufferedReader.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                // new instance
                splitter.setString(line);
                double timestamp = 0.0;
                double[] accel = new double[3];
                double[] ecg = new double[2];
                if (splitter.hasNext()) {
                    timestamp = Double.parseDouble(splitter.next());
                }
                for (int i = 0; i < accel.length; i++) {
                    if (splitter.hasNext()) {
                        accel[i] = Double.parseDouble(splitter.next());
                    }
                }
                for (int i = 0; i < ecg.length; i++) {
                    if (splitter.hasNext()) {
                        ecg[i] = Double.parseDouble(splitter.next());
                    }
                }

                SimbleeSensor.SimbleeDataFrame data = new SimbleeSensor.SimbleeDataFrame(this, (long) timestamp, accel, ecg);

                // let thread sleep when sensor should not be streaming
                while (getState() != SensorState.STREAMING) {
                    Thread.sleep(1);
                }

                // send message
                sendNewData(data);

                Thread.sleep(samplingInterval);

                if (Thread.interrupted()) {
                    Log.e(TAG, "Thread interrupted!");
                    return;
                }

            }
        } catch (InterruptedException e) {
            // Thread probably interrupted by disconnect request, so return
            return;
        } catch (Exception e) {
            Log.e(TAG, "Error during data reading", e);
        }

        // end of file
        disconnect();
    }
}
