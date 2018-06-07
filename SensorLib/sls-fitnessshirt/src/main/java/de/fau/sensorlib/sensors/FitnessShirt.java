/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import de.fau.sensorlib.BleSensorManager;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.HeartRateDataFrame;
import de.fau.sensorlib.dataframe.RespirationDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;

/**
 * Implementation of the Fraunhofer FitnessSHIRT.
 */
public class FitnessShirt extends AbstractSensor {
    //generic UUID for serial port protocol
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final double ADC_BASELINE_IN_V = 1.65;
    private static final double ADC_TO_V_MULTIPLIER = 3.3 / 4095d;

    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private InputStream btInputStream;
    private DataInputStream mBtDataInStream;
    private ConnectedThread commThread;
    private double samplingIntervalMillis = 0;
    private long startStreamingTimestamp = 0;


    public static class FitnessShirtDataFrame extends SensorDataFrame implements EcgDataFrame, AccelDataFrame, RespirationDataFrame, HeartRateDataFrame {
        double[] ecgSamples = new double[16];
        double ecg;
        long respiration;
        long respirationRate;
        long heartRate;
        int ax, ay, az;

        public FitnessShirtDataFrame(AbstractSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        @Override
        public double getEcgSample() {
            return ecg;
        }

        @Override
        public double getSecondaryEcgSample() {
            return 0;
        }

        @Override
        public double getHeartRate() {
            return heartRate;
        }

        @Override
        public double getInterbeatInterval() {
            return 60d / heartRate;
        }

        @Override
        public double getRespirationSample() {
            return respiration;
        }

        @Override
        public double getRespirationRate() {
            return respirationRate;
        }

        @Override
        public double getAccelX() {
            return ax;
        }

        @Override
        public double getAccelY() {
            return ay;
        }

        @Override
        public double getAccelZ() {
            return az;
        }
    }

    @Override
    protected void dispatchNewData(SensorDataFrame data) {
        if (!(data instanceof FitnessShirtDataFrame))
            return;

        FitnessShirtDataFrame fsdf = (FitnessShirtDataFrame) data;

        // each data frame contains 16 consecutive ecg samples, we reuse the same dataframe to send them separately
        for (int i = 0; i < fsdf.ecgSamples.length; i++) {
            FitnessShirtDataFrame fdf = new FitnessShirtDataFrame(this, fsdf.getTimestamp() + i * samplingIntervalMillis);
            fdf.ecg = fsdf.ecgSamples[i];
            fdf.respiration = fsdf.respiration;

            super.dispatchNewData(fdf);

            //fsdf.ecg = fsdf.ecgSamples[i];
            //fsdf.setTimestamp( fsdf.getTimestamp() + samplingIntervalMillis );
            //mExternalHandler.onNewData( fsdf );
        }
    }

    public FitnessShirt(Context context, BluetoothDevice btDevice, SensorDataProcessor dataHandler) {
        this(context, btDevice.getName(), btDevice.getAddress(), dataHandler);
    }

    public FitnessShirt(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(context, deviceName, deviceAddress, dataHandler, 256);
    }

    @Override
    public boolean connect() throws Exception {
        super.connect();

        btDevice = BleSensorManager.findBtDevice(mDeviceAddress);
        if (btDevice == null)
            return false;

        sendSensorCreated();

        // try various possible ways to connect to the FS via BT
        if (connectNormally()) {
            Log.i(this.getClass().getSimpleName(), "Connected normally.");
        } else {
            if (connectInsecurely()) {
                Log.i(this.getClass().getSimpleName(), "Connected insecurely.");
            } else {
                if (!connectUsingReflection())
                    return false;   // Everything failed. Connection is not possible.
            }
        }

        samplingIntervalMillis = 1000d / getSamplingRate();
        sendConnected();
        // immediately try to start streaming after successful connection
        startStreaming();
        return true;
    }

    private boolean connectNormally() {
        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            btSocket.connect();
        } catch (IOException e) {
            try {
                UUID uuid = btDevice.getUuids()[0].getUuid();
                btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
                btSocket.connect();
            } catch (IOException e1) {
                return false;
            }
        }
        return true;
    }

    private boolean connectInsecurely() {
        try {
            btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
            }
            btSocket.connect();
        } catch (IOException e) {
            try {
                UUID uuid = btDevice.getUuids()[0].getUuid();
                btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(uuid);
                btSocket.connect();
            } catch (IOException e1) {
                return false;
            }
        }

        return true;
    }

    private boolean connectUsingReflection() {
        try {
            Method m = btDevice.getClass().getMethod("createRfcommSocket", int.class);
            btSocket = (BluetoothSocket) m.invoke(btDevice, 1);
            btSocket.connect();
        } catch (NoSuchMethodException e1) {
            return false;
        } catch (IllegalAccessException e1) {
            return false;
        } catch (InvocationTargetException e1) {
            return false;
        } catch (IOException e1) {
            return false;
        }
        return true;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        try {
            stopStreaming();
            btSocket.close();
            sendDisconnected();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startStreaming() {
        try {
            if (btSocket == null)
                return;

            if (commThread != null) {
                commThread.cancel();
                commThread = null;
            }

            btInputStream = btSocket.getInputStream();
            mBtDataInStream = new DataInputStream(btInputStream);

            startStreamingTimestamp = System.nanoTime();

            sendStartStreaming();
            commThread = new ConnectedThread();
            commThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopStreaming() {
        try {
            if (mBtDataInStream != null)
                mBtDataInStream.close();
            if (btInputStream != null)
                btInputStream.close();
            sendStopStreaming();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean requestSamplingRateChange(double toSamplingRate) {
        // FS works at fixed sampling rate
        return false;
    }

    private class ConnectedThread extends Thread {
        public synchronized void run() {
            byte[] buffer = new byte[46];  // buffer store for the stream

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // read 46 byte data frames
                    //bytes = btInputStream.read( buffer );
                    mBtDataInStream.readFully(buffer, 0, 46);

                    //Log.d( this.getClass().getSimpleName(), "@" + System.currentTimeMillis() + " --> BT read: " + numBytesRead );

                    FitnessShirtDataFrame df = extractDataFrame(buffer);
                    if (df == null)
                        continue;

                    sendNewData(df);

/*
                    // dispatch message with data to UI
                    // each data frame contains 16 consecutive ecg samples, we reuse the same dataframe to send them separately
                    for (int i = 0; i < df.ecgSamples.length; i++)
                    {
                        FitnessShirtDataFrame fdf = new FitnessShirtDataFrame( getThis(), df.getTimestamp() + i * samplingIntervalMillis );
                        fdf.ecg = df.ecgSamples[i];
                        fdf.respiration = df.respiration;
                        //fdf.setTimestamp( df.getTimestamp() + samplingIntervalMillis );
                        //mInternalHandler.obtainMessage( MESSAGE_NEW_DATA, df ).sendToTarget();
                        mExternalHandler.onNewData( fdf );
                    }*/

                } catch (IOException e) {
                    Log.i(this.getClass().getSimpleName(), "Datastream read failed, probably BT connection terminated.");
                    //e.printStackTrace();
                    //notifyExternalHandler( "Connection closed by remote sensor." );
                    sendConnectionLost();
                    disconnect();
                    break;
                }
            }
        }

        public void cancel() {
            // TODO: cleanup
        }
    }


    /**
     * @param lowByte  lower Byte
     * @param highByte upper Byte
     * @return merged value.
     */
    private static long mergeLowHigh16BitUnsigned(byte lowByte, byte highByte) {
        return (lowByte & 0xFF) | (highByte & 0xFF) << 8;
    }

    /**
     * @param lowByte  lower Byte
     * @param highByte upper Byte
     * @return merged value.
     */
    private static int mergeLowHigh12BitSigned(byte lowByte, byte highByte) {
        // since the 12 bit "shorts" are actually signed, we need to check for that negative sign bit and fill
        // the 4 high bits in the 16-bit Java short to correctly compensate.
        if ((highByte & 0x8) == 0)
            // positive value
            return (lowByte & 0xFF) | (highByte & 0xFF) << 8;
        // negative 12-bit value, shift fill the highest 4 bits in the 16 bit short
        return (lowByte & 0xFF) | (highByte & 0xF) << 8 | (0xFFFFF << 12);
    }

    /**
     * Extracts the data from the dataframe byte buffer.
     *
     * @param buffer byte data buffer.
     * @return extracted FitnessSHIRT data frame.
     */
    private FitnessShirtDataFrame extractDataFrame(byte[] buffer) {
        //ByteBuffer buf = ByteBuffer.wrap(buffer);
        //if (buf.get() != -1 || buf.get() != -1)


        if (buffer[0] != -1 || buffer[1] != -1) {
            // invalid data frame
            Log.d(this.getClass().getSimpleName(), "Invalid data frame (" + buffer[0] + " " + buffer[1] + ")");
            return null;
        }

        FitnessShirtDataFrame df = new FitnessShirtDataFrame(this, (System.nanoTime() - startStreamingTimestamp) / 1.0e6);

        // each value is encoded in a low and following high-byte.

        // 16 ECG samples are 16-bit unsigned
        for (int i = 0; i < 16; i++) {
            df.ecgSamples[i] = mergeLowHigh16BitUnsigned(buffer[2 + i * 2], buffer[3 + i * 2]);
            // convert from ADC units to Volt and subtract baseline
            df.ecgSamples[i] = df.ecgSamples[i] * ADC_TO_V_MULTIPLIER - ADC_BASELINE_IN_V;
        }

        // respiration, respRate and heartRate are all 16-bit unsigned
        df.respiration = mergeLowHigh16BitUnsigned(buffer[34], buffer[35]);
        df.respirationRate = mergeLowHigh16BitUnsigned(buffer[36], buffer[37]);
        df.heartRate = mergeLowHigh16BitUnsigned(buffer[38], buffer[39]);

        // acceleration is 12-bit signed
        df.ax = mergeLowHigh12BitSigned(buffer[40], buffer[41]);
        df.ay = mergeLowHigh12BitSigned(buffer[42], buffer[43]);
        df.az = mergeLowHigh12BitSigned(buffer[44], buffer[45]);

        return df;
    }
}
