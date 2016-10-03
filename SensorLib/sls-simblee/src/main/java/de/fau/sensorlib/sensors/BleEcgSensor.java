/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.DsSensorManager;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;

/**
 * Custom BLE ECG sensor implementation.
 */
public class BleEcgSensor extends DsSensor {

    private static final String TAG = BleEcgSensor.class.getSimpleName();

    public static final String DEVICE_NAME = "POSTAGE STAMP ECG";


    // ECG SERVICE
    private static final UUID ECG_SERVICE = UUID.fromString("00002d0d-0000-1000-8000-00805f9b34fb");
    // CHARACTERS1
    private static final UUID ECG_MEASURE_CHAR = UUID.fromString("00002d37-0000-1000-8000-00805f9b34fb");
    // DESCRIPTORS
    private static final UUID GATT_CLIENT_CFG_DESC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BleEcgDataWriter mWriter;

    private BluetoothGatt mBluetoothGatt;

    /**
     * GATT callback for the communication with the Bluetooth remote device
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String name = gatt.getDevice().getName();

            if (status == BluetoothGatt.GATT_SUCCESS &&
                    newState == BluetoothProfile.STATE_CONNECTED) {

                 /*
                 * Once successfully connected, we must next discover all the
				 * services on the device before we can read and write their
				 * characteristics.
				 */
                sendConnected();
                sendNotification("Discovering services...");

            } else if (status == BluetoothGatt.GATT_SUCCESS
                    && newState == BluetoothProfile.STATE_DISCONNECTED) {

                 /*
                 * If at any point we disconnect, send a message to clear the
				 * values out of the UI
				 */
                if (name.equals(mName)) {
                    sendDisconnected();
                }
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * if there is a failure at any stage, simply disconnect
                 */
                if (name.equals(mName)) {
                    sendConnectionLost();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services discovered: " + status);
            /*
             * With services discovered, we are going to reset our state machine
             * and start working through the sensors we need to enable
             */
            if (gatt.getDevice().getName().equals(mName)) {
                sendNotification("Enabling Sensors...");
                enableEcgSensor(gatt);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Problem writing characteristics!");
            } else {
                Log.d(TAG, "Gatt success!");
            }
            if (gatt.getDevice().getName().equals(mName)) {
                readEcgSensor(gatt, null, characteristic);
            }
        }

        private long timestamp;

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            if (gatt.getDevice().getName().equals(mName)) {
                byte[] values = characteristic.getValue();
                double[] data = new double[values.length / 2];
                int i = 0;
                while (i != values.length) {
                    int tmp1 = ((values[i] & 0xFF) << 6) - 128;
                    int tmp2 = (int) values[i + 1] & 0xFF;
                    int tmp = tmp1 + tmp2;
                    data[i / 2] = tmp;
                    BleEcgDataFrame ecgData = new BleEcgDataFrame(BleEcgSensor.this, timestamp++, data[i / 2]);
                    sendNewData(ecgData);
                    scheduleWriting(ecgData);
                    i += 2;
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {

            if (gatt.getDevice().getName().equals(mName)) {
                Log.d(TAG, "Descriptor wrote");
                readEcgSensor(gatt, descriptor, null);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "Remote RSSI: " + rssi);
        }
    };

    public static class BleEcgDataFrame extends SensorDataFrame implements EcgDataFrame {

        double ecg;
        char label;
        public long timestamp;

        public BleEcgDataFrame(DsSensor sensor, long timestamp) {
            super(sensor, timestamp);

        }

        public BleEcgDataFrame(DsSensor sensor, long timestamp, double ecg) {
            super(sensor, timestamp);
            this.ecg = ecg;
            this.timestamp = timestamp;
        }

        public BleEcgDataFrame(DsSensor sensor, long timestamp, double ecg, char label) {
            super(sensor, timestamp);
            this.ecg = ecg;
            this.timestamp = timestamp;
            this.label = label;
        }

        @Override
        public double getEcgSample() {
            return ecg;
        }

        @Override
        public double getSecondaryEcgSample() {
            return 0;
        }

        public char getLabel() {
            return label;
        }
    }


    public BleEcgSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        super(context, knownSensor, dataHandler);
    }

    public BleEcgSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(context, deviceName, deviceAddress, dataHandler);
    }

    @Override
    public boolean connect() throws Exception {
        super.connect();
        BluetoothDevice device = DsSensorManager.findBtDevice(mDeviceAddress);
        if (device != null) {
            mBluetoothGatt = device.connectGatt(mContext, true, mGattCallback);
            mWriter = new BleEcgDataWriter();
            sendNotification("Connecting to... " + device.getName());
            sendConnecting();
            return true;
        }
        return false;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        Log.d(TAG, "disconnect");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @Override
    public void startStreaming() {
        Log.d(TAG, "start streaming");
        mBluetoothGatt.discoverServices();
    }

    @Override
    public void stopStreaming() {
        Log.d(TAG, "stop streaming");
        if (mWriter != null) {
            mWriter.completeWriter();
        }
        mBluetoothGatt.disconnect();
        sendStopStreaming();
    }

    /**
     * Enables the specific ECG device
     *
     * @param gatt GATT client invoked
     */
    private void enableEcgSensor(BluetoothGatt gatt) {
        List<BluetoothGattService> services = gatt.getServices();
        for (int p = 0; p < services.size(); p++) {
            Log.d("SERVICE" + Integer.toString(p), services.get(p)
                    .getUuid().toString());
            List<BluetoothGattCharacteristic> characs = services.get(p)
                    .getCharacteristics();
            for (int i = 0; i < characs.size(); i++) {
                Log.d("CHARACS" + Integer.toString(i), characs.get(i)
                        .getUuid().toString());
                List<BluetoothGattDescriptor> descri = characs.get(i)
                        .getDescriptors();
                if (!descri.isEmpty()) {
                    for (int n = 0; n < descri.size(); n++) {
                        Log.d("DESCRIPS", descri.get(n).getUuid()
                                .toString());
                    }
                }
                Log.d("*******", "*******************************");
            }
            Log.d("--------", "-----------------------------");
        }

        Log.d(TAG, "Enabling ECG Service");

        BluetoothGattService service = gatt.getService(ECG_SERVICE);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(ECG_MEASURE_CHAR);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GATT_CLIENT_CFG_DESC);
        descriptor.setValue(new byte[]{0x1, 0x0});

        Log.d(TAG, "Trying to write characteristics");
        if (!gatt.writeDescriptor(descriptor)) {
            Log.e(TAG, "Error enabling ECG");
        }
        gatt.setCharacteristicNotification(characteristic, true);

        sendStartStreaming();
        if (mWriter != null) {
            mWriter.prepareWriter(mSamplingRate);
        }
    }

    /**
     * Reads the characteristics of the specific ECG device
     *
     * @param gatt           GATT client invoked
     * @param descriptor     GATT descriptor
     * @param characteristic Characteristic that was written to the associated remote device
     */
    private void readEcgSensor(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                               BluetoothGattCharacteristic characteristic) {


        if (characteristic != null) {
            gatt.readCharacteristic(characteristic);
            return;
        }

        characteristic = descriptor.getCharacteristic();

        if (!characteristic.getUuid().equals(ECG_MEASURE_CHAR)) {
            gatt.readCharacteristic(characteristic);
        }
    }

    /**
     * Schedules the writing of the device's raw ECG data
     *
     * @param ecgData The raw ECG data from the Bluetooth device
     */
    private void scheduleWriting(final BleEcgDataFrame ecgData) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mWriter != null) {
                    mWriter.writeData(ecgData);
                }
            }
        }).start();
    }

    /**
     * Original version by Tim Maiwald, Max Schaldach endowed professorship of Biomedical Engineering.
     * <p>
     * Modified by Robert Richer, Digital Sports Group, Pattern Recognition Lab, Department of Computer Science.
     * <p>
     * FAU Erlangen-Nürnberg
     * <p>
     * (c) 2014
     *
     * @author Tim Maiwald
     * @author Robert Richer
     */
    public class BleEcgDataWriter {

        private static final char mSeparator = '\n';
        private static final String mHeader = "samplingrate";
        private String mName;
        private BufferedWriter mBufferedWriter;
        private File mECGFileHandler;
        private boolean mStorageWritable;
        private boolean mECGFileCreated;

        /**
         * Creates a new DataWriter to write the received ECG data to the external storage
         */
        public BleEcgDataWriter() {
            String[] parts = mDeviceAddress.split(":");
            String name = parts[parts.length - 2] + parts[parts.length - 1];
            if (parts.length > 1) {
                mName = name;
            } else {
                mName = "XXXX";
            }
            // create file
            createFile();
        }

        private void createFile() {
            String state;
            File root = null;
            File path;
            String currentTimeString;

            // set current time
            currentTimeString = new SimpleDateFormat("dd.MM.yy_HH.mm_", Locale.getDefault()).format(new Date());

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
                }
            }

            if (mStorageWritable) {
                try {
                    // create directory
                    path = new File(root, "DailyHeartRecordings");
                    mECGFileCreated = path.mkdir();
                    if (!mECGFileCreated) {
                        mECGFileCreated = path.exists();
                        if (!mECGFileCreated) {
                            Log.e(TAG, "File could not be created!");
                            return;
                        } else {
                            Log.i(TAG, "Working directory is " + path.getAbsolutePath());
                        }
                    }
                    // create files
                    mECGFileHandler = new File(path + "/dailyheart_" + currentTimeString + mName + ".csv");
                    mECGFileCreated = mECGFileHandler.createNewFile();
                    if (!mECGFileCreated) {
                        mECGFileCreated = mECGFileHandler.exists();
                        if (!mECGFileCreated) {
                            Log.e(TAG, "File could not be created!");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception on dir and file create!", e);
                    mECGFileCreated = false;
                }
            }
        }

        /**
         * Prepares the {@link BleEcgDataWriter}
         *
         * @param samplingRate The sensor's sampling rate
         */
        public void prepareWriter(double samplingRate) {
            FileWriter fw;
            if (mStorageWritable && mECGFileCreated) {
                try {
                    // open buffered writer and write header line
                    fw = new FileWriter(mECGFileHandler);
                    mBufferedWriter = new BufferedWriter(fw);
                    mBufferedWriter.write(mHeader);
                    mBufferedWriter.write(String.valueOf(samplingRate));
                    mBufferedWriter.write(mSeparator);
                } catch (Exception e) {
                    Log.e(TAG, "Exception on dir and file create!", e);
                    mECGFileCreated = false;
                }
            }
        }

        /**
         * Adds the received ECG data into the internal {@link BufferedWriter}.
         *
         * @param ecgData An array of incoming ECG data
         */
        public void writeData(BleEcgDataFrame ecgData) {
            if (isWritable()) {
                try {
                    // writes the raw value into the BufferedWriter
                    mBufferedWriter.write(String.valueOf(ecgData.ecg));
                    mBufferedWriter.write(mSeparator);
                } catch (Exception ignored) {
                }
            }
        }

        /**
         * Flushes and closes the internal {@link BufferedWriter}
         */
        public void completeWriter() {
            if (isWritable()) {
                try {
                    // flush and close writer
                    mBufferedWriter.flush();
                    mBufferedWriter.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error on completing writer!");
                }
            }
        }

        private boolean isWritable() {
            return (mStorageWritable && mECGFileCreated && (mBufferedWriter != null));
        }

    }
}
