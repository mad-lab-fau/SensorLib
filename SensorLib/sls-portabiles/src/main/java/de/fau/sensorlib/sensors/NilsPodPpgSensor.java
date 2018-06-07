/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.Manifest;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import de.fau.sensorlib.BleGattAttributes;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.PpgDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;


/**
 * Represents a NilsPod Sensor device for PPG measurement.
 */
public class NilsPodPpgSensor extends GenericBleSensor {

    /**
     * UUID for Data Streaming Service of NilsPod sensor
     */
    private static final UUID NILSPOD_STREAMING_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Config Characteristic (write) of NilsPod Sensor
     */
    private static final UUID NILSPOD_CONFIG = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Streaming Characteristic (read) of NilsPod Sensor
     */
    private static final UUID NILSPOD_STREAMING = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private static final int PACKET_SIZE = 14;


    // Adding custom Hoop BLE UUIDs to known UUID pool
    static {
        BleGattAttributes.addService(NILSPOD_STREAMING_SERVICE, "NilsPod Sensor Streaming");
        BleGattAttributes.addCharacteristic(NILSPOD_CONFIG, "NilsPod Sensor Configuration");
        BleGattAttributes.addCharacteristic(NILSPOD_STREAMING, "NilsPod Stream Data");
    }

    /**
     * Sensor commands for communication with NilsPod Sensor. Used with the Sensor Config Characteristic
     */
    private enum NilsPodSensorCommands {
        /**
         * Start Streaming Command
         */
        START_STREAMING((byte) 0xC2),
        /**
         * Stop Streaming Command
         */
        STOP_STREAMING((byte) 0xC1);

        private byte cmd;

        NilsPodSensorCommands(byte cmd) {
            this.cmd = cmd;
        }
    }

    /**
     * Global counter for incoming packages (local counter only has 15 bit)
     */
    private int globalCounter = 0;

    /**
     * Local counter for incoming packages
     */
    private int lastCounter = 0;

    /**
     * Flag indicating whether data should be logged
     */
    private boolean mLoggingEnabled;

    /**
     * Data logger
     */
    private NilsPodPpgDataLogger mDataLogger;

    /**
     * Keep a local reference to the Streaming Service
     */
    private BluetoothGattService mStreamingService;


    /**
     * Create a new instance of one NilsPod Sensor
     *
     * @param context     Application context
     * @param knownSensor The Sensor reference retrieved from the BLE scan
     */
    public NilsPodPpgSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        super(context, knownSensor.getName(), knownSensor.getDeviceAddress(), dataHandler, 200);
    }

    @Override
    public void startStreaming() {
        if (send(NilsPodSensorCommands.START_STREAMING)) {
            super.startStreaming();
            if (mLoggingEnabled) {
                mDataLogger = new NilsPodPpgDataLogger(mContext);
            }
        } else {
            Log.e(TAG, "startStreaming failed!");
        }
    }

    @Override
    public void stopStreaming() {
        if (send(NilsPodSensorCommands.STOP_STREAMING)) {
            super.stopStreaming();
            if (mDataLogger != null) {
                mDataLogger.completeLogger();
            }
        } else {
            Log.e(TAG, "stopStreaming failed!");
        }
    }

    @Override
    protected boolean onNewCharacteristicValue(BluetoothGattCharacteristic characteristic, boolean isChange) {
        if (super.onNewCharacteristicValue(characteristic, isChange)) {
            return true;
        } else {
            if (NILSPOD_STREAMING.equals(characteristic.getUuid())) {
                extractSensorData(characteristic);
                return true;
            }
            return false;
        }
    }


    /**
     * Extracts sensor data into data frames from the given characteristic.
     *
     * @param characteristic Received characteristic from the BLE API
     */
    private void extractSensorData(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();

        // one data packet always has size PACKET_SIZE
        if (values.length % PACKET_SIZE != 0) {
            Log.e(TAG, "Wrong BLE Packet Size!");
            return;
        }

        // iterate over data packets
        for (int i = 0; i < values.length; i += PACKET_SIZE) {
            int offset = i;
            double[] gyro = new double[3];
            double[] accel = new double[3];
            int localCounter;

            // extract gyroscope data
            for (int j = 0; j < 3; j++) {
                gyro[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                offset += 2;
            }
            // extract accelerometer data
            for (int j = 0; j < 3; j++) {
                accel[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                offset += 2;
            }

            // extract packet counter (only 15 bit, therefore getIntValue() method not applicable)
            localCounter = (values[offset + 1] & 0xFF) | ((values[offset] & 0x7F) << 8);

            // check if packets have been lost
            if (((localCounter - lastCounter) % (2 << 14)) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }
            // increment global counter if local counter overflows
            if (localCounter < lastCounter) {
                globalCounter++;
            }

            // TODO Just a quick dirty hack!
            //NilsPodPpgDataFrame df = new NilsPodPpgDataFrame(this, globalCounter * (2 << 14) + localCounter, accel, gyro);
            NilsPodPpgDataFrame df = new NilsPodPpgDataFrame(this, globalCounter * (2 << 14) + localCounter, new double[3], new double[3], new double[]{accel[0], gyro[0]});
            Log.d(TAG, df.toString());
            // send new data to the SensorDataProcessor
            sendNewData(df);
            lastCounter = localCounter;
            if (mLoggingEnabled) {
                mDataLogger.writeData(df);
            }
        }
    }

    @Override
    protected boolean shouldEnableNotification(BluetoothGattCharacteristic c) {
        if (super.shouldEnableNotification(c)) {
            return true;
        } else if (NILSPOD_STREAMING.equals(c.getUuid())) {
            return true;
        }

        return false;
    }


    @Override
    protected void onDiscoveredService(BluetoothGattService service) {
        super.onDiscoveredService(service);
        if (NILSPOD_STREAMING_SERVICE.equals(service.getUuid())) {
            mAvailableSensors.add(HardwareSensor.ACCELEROMETER);
            mAvailableSensors.add(HardwareSensor.GYROSCOPE);
            mStreamingService = service;
        }
    }


    /**
     * Send command to sensor via Config Characteristic
     *
     * @param cmd Sensor Command
     * @return true if data has been successfully sent, false otherwise
     */
    private boolean send(NilsPodSensorCommands cmd) {
        Log.d(TAG, "Sending " + cmd + " command to " + getName());
        return send(new byte[]{cmd.cmd});
    }


    private boolean send(byte[] data) {
        if (mStreamingService == null) {
            Log.w(TAG, "Service not found");
            return false;
        }
        BluetoothGattCharacteristic characteristic = mStreamingService.getCharacteristic(NILSPOD_CONFIG);
        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return mGatt.writeCharacteristic(characteristic);
    }


    /**
     * Enables data logging for this sensor
     */
    public void enableDataLogger() {
        mLoggingEnabled = true;
    }

    /**
     * Disables data logging for this sensor
     */
    public void disableDataLogger() {
        mLoggingEnabled = false;
    }


    /**
     * Data frame to store data received from the NilsPod Sensor
     */
    public static class NilsPodPpgDataFrame extends SensorDataFrame implements AccelDataFrame, GyroDataFrame, PpgDataFrame {

        private double[] accel;
        private double[] gyro;
        private double[] ppg;
        private long timestamp;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         */
        public NilsPodPpgDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] gyro) {
            super(sensor, timestamp);
            if (accel.length != 3 || gyro.length != 3) {
                throw new IllegalArgumentException("Illegal array size for " + ((accel.length != 3) ? "acceleration" : "gyroscope") + " values! ");
            }
            this.accel = accel;
            this.gyro = gyro;
            this.ppg = new double[0];
            this.timestamp = timestamp;
        }

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         * @param ppg       array storing PPG values
         */
        public NilsPodPpgDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] gyro, double[] ppg) {
            super(sensor, timestamp);
            if (accel.length != 3 || gyro.length != 3) {
                throw new IllegalArgumentException("Illegal array size for " + ((accel.length != 3) ? "acceleration" : "gyroscope") + " values! ");
            }
            this.accel = accel;
            this.gyro = gyro;
            this.ppg = ppg;
            this.timestamp = timestamp;
        }

        @Override
        public double getGyroX() {
            return gyro[0];
        }

        @Override
        public double getGyroY() {
            return gyro[1];
        }

        @Override
        public double getGyroZ() {
            return gyro[2];
        }

        @Override
        public double getAccelX() {
            return accel[0];
        }

        @Override
        public double getAccelY() {
            return accel[1];
        }

        @Override
        public double getAccelZ() {
            return accel[2];
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + timestamp + ", accel: " + Arrays.toString(accel) + ", gyro: " + Arrays.toString(gyro) + ", ppg: " + Arrays.toString(ppg);
        }

        @Override
        public double getPpg() {
            if (ppg.length > 0) {
                return ppg[0];
            } else {
                return -1;
            }
        }

        @Override
        public double getPpgSecondary() {
            if (ppg.length > 1) {
                return ppg[1];
            }
            return -1;
        }
    }


    /**
     * Data logger for sensor data. If possible, data is logged onto the device's SD card. If not,
     * data is logged to the device's external storage.
     */
    public class NilsPodPpgDataLogger {

        /**
         * Value separator
         */
        private static final String SEPARATOR = ",";
        /**
         * Line delimiter
         */
        private static final String DELIMITER = "\r\n";
        /**
         * File header
         */
        private String mHeader = "samplingrate" +
                SEPARATOR + ((int) getSamplingRate()) + DELIMITER + "timestamp" +
                SEPARATOR + "acc_x" + SEPARATOR + "acc_y" +
                SEPARATOR + "acc_z" + SEPARATOR + "gyr_x" +
                SEPARATOR + "gyr_y" + SEPARATOR + "gyr_z" +
                SEPARATOR + "ppg_1" + SEPARATOR + "ppg_2" +
                DELIMITER;
        private String mFilename;
        /**
         * Directory name where data will be stored on the external storage
         */
        private String dirName = "HoopSensorRecordings";
        private BufferedWriter mBufferedWriter;
        private File mFileHandler;
        private boolean mStorageWritable;
        private boolean mFileCreated;
        private Context mContext;


        /**
         * Creates a new data logger instance
         */
        public NilsPodPpgDataLogger(Context context) {
            mContext = context;
            String currTime = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
            // Filename consists of sensor device name and start time of data logging
            mFilename = NilsPodPpgSensor.this.getDeviceName() + "_" + currTime + ".csv";

            if (checkPermissions()) {
                createFile();
                prepareWriter();
            } else {
                Toast.makeText(mContext, "Permissions to write external storage needed!", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Checks if permissions to read and write external storage have been granted by the user
         *
         * @return true if permissions have been granted, false otherwise
         */
        private boolean checkPermissions() {
            return ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;
        }

        private void createFile() {
            String state;
            File root = null;
            File path;

            // try to write on SD card
            state = Environment.getExternalStorageState();
            switch (state) {
                case Environment.MEDIA_MOUNTED:
                    // media readable and writable
                    root = Environment.getExternalStorageDirectory();
                    mStorageWritable = true;
                    break;
                case Environment.MEDIA_MOUNTED_READ_ONLY:
                    // media only readable
                    mStorageWritable = false;
                    Log.e(TAG, "SD card only readable!");
                    break;
                default:
                    // not readable or writable
                    mStorageWritable = false;
                    Log.e(TAG, "SD card not readable and writable!");
                    break;
            }

            if (!mStorageWritable) {
                // try to write on external storage
                root = Environment.getDataDirectory();
                if (root.canWrite()) {
                    mStorageWritable = true;
                } else {
                    Log.e(TAG, "External storage not readable and writable!");
                    Toast.makeText(mContext, "External storage not readable and writable!", Toast.LENGTH_SHORT).show();
                }
            }

            if (mStorageWritable) {
                try {
                    // create directory
                    path = new File(root, dirName);
                    mFileCreated = path.mkdir();
                    if (!mFileCreated) {
                        mFileCreated = path.exists();
                        if (!mFileCreated) {
                            Log.e(TAG, "File could not be created!");
                            return;
                        } else {
                            Log.i(TAG, "Working directory is " + path.getAbsolutePath());
                        }
                    }
                    // create files
                    mFileHandler = new File(path + "/" + mFilename);
                    mFileCreated = mFileHandler.createNewFile();
                    if (!mFileCreated) {
                        mFileCreated = mFileHandler.exists();
                        if (!mFileCreated) {
                            Log.e(TAG, "File could not be created!");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception on dir and file create!", e);
                    mFileCreated = false;
                }
            }
        }

        private void prepareWriter() {
            FileWriter fw;
            if (mStorageWritable && mFileCreated) {
                try {
                    // open buffered writer and write header line
                    fw = new FileWriter(mFileHandler);
                    mBufferedWriter = new BufferedWriter(fw);
                    mBufferedWriter.write(mHeader);
                } catch (Exception e) {
                    Log.e(TAG, "Exception on dir and file create!", e);
                    mFileCreated = false;
                }
            }
        }

        /**
         * Writes next line of sensor data
         *
         * @param data data frame from Hoop Sensor
         */
        public void writeData(NilsPodPpgDataFrame data) {
            if (isWritable()) {
                try {
                    String line = (data.getTimestamp() + SEPARATOR) +
                            (data.getAccelX() + SEPARATOR + data.getAccelY() + SEPARATOR + data.getAccelZ() + SEPARATOR) +
                            (data.getGyroX() + SEPARATOR + data.getGyroY() + SEPARATOR + data.getGyroZ()) +
                            (data.getPpg() + SEPARATOR + data.getPpgSecondary()) + DELIMITER;
                    //Log.d(TAG, line);
                    mBufferedWriter.write(line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Closes file after data logging has been completed
         */
        private void completeLogger() {
            if (isWritable()) {
                try {
                    // flush and close writer
                    mBufferedWriter.flush();
                    mBufferedWriter.close();
                    mBufferedWriter = null;
                } catch (Exception e) {
                    Log.e(TAG, "Error on completing writer!");
                }
            }
        }

        /**
         * Checks if data can be written to the device
         *
         * @return true if data can be written, false otherwise
         */
        private boolean isWritable() {
            return (mStorageWritable && mFileCreated && (mBufferedWriter != null));
        }

    }
}
