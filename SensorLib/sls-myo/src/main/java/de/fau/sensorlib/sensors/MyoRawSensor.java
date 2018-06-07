/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
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
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

import de.fau.sensorlib.BleSensorManager;
import de.fau.sensorlib.SensorDataProcessor;

/**
 * Implementation of the Myo sensor that reports raw EMG data from the Myo band, instead of abstract gestures
 */
public class MyoRawSensor extends AbstractSensor {

    private static final String TAG = MyoRawSensor.class.getSimpleName();

    // Service IDs
    public static final UUID SERVICE_MYO_COMMAND = UUID.fromString("d5060001-a904-deb9-4748-2c7f4a124842");
    public static final UUID SERVICE_MYO_EMG_DATA = UUID.fromString("d5060005-a904-deb9-4748-2c7f4a124842");


    public static final class MyoCommands {
        public static final byte[] COMMAND_UNSET_DATA = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00};
        public static final byte[] COMMAND_VIBRATION_3 = new byte[]{0x03, 0x01, 0x03};
        public static final byte[] COMMAND_EMG_ONLY = new byte[]{0x01, 0x03, 0x02, 0x00, 0x00};
        public static final byte[] COMMAND_UNLOCK = new byte[]{0x0A, 0x01, 0x01};
        public static final byte[] COMMAND_UNSLEEP = new byte[]{0x09, 0x01, 0x01};
        public static final byte[] COMMAND_NORMAL_SLEEP = new byte[]{0x09, 0x01, 0x00};
    }


    public class MyoGattCallback extends BluetoothGattCallback {

        private final String TAG = MyoGattCallback.class.getSimpleName();

        // Characteristics ID's
        private final UUID CHAR_MYO_INFO = UUID.fromString("d5060101-a904-deb9-4748-2c7f4a124842");
        private final UUID CHAR_MYO_FIRMWARE = UUID.fromString("d5060201-a904-deb9-4748-2c7f4a124842");
        private final UUID CHAR_EMG_0 = UUID.fromString("d5060105-a904-deb9-4748-2c7f4a124842");
        private final UUID CHAR_COMMAND = UUID.fromString("d5060401-a904-deb9-4748-2c7f4a124842");

        // Android Characteristic ID
        private final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

        long mLastSendNeverSleep = System.currentTimeMillis();
        final static long NEVER_SLEEP_TIME = 10000; // in ms

        // Myo & Bluetooth
        private BluetoothGattCharacteristic mCommandCharacteristic;
        private BluetoothGattCharacteristic mEmgCharacteristic;
        private Queue<BluetoothGattDescriptor> mWriteDescriptorQueue = new LinkedList<>();
        private Queue<BluetoothGattCharacteristic> mReadCharacteristicQueue = new LinkedList<>();
        private BluetoothGatt mBluetoothGatt;

        private int[] mEmgData = new int[16];

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange: " + status + " -> " + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // GATT connected => discover services
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // GATT disconnected => stop GATT callback
                stopCallback();
                Log.d(TAG, "GATT disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered received: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Find GATT service
                BluetoothGattService serviceEmg = gatt.getService(SERVICE_MYO_EMG_DATA);
                if (serviceEmg == null) {
                    Log.e(TAG, "No EMG service available");
                } else {
                    Log.d(TAG, "Found EMG service");
                    // Get EMG characteristic
                    mEmgCharacteristic = serviceEmg.getCharacteristic(CHAR_EMG_0);
                    if (mEmgCharacteristic == null) {
                        Log.e(TAG, "No EMG data characteristic found");
                    } else {
                        boolean registered = gatt.setCharacteristicNotification(mEmgCharacteristic, true);
                        if (!registered) {
                            Log.d(TAG, "EMG data notification false");
                        } else {
                            Log.d(TAG, "EMG data notification true");

                            // Now we can turn on the characteristic notification
                            BluetoothGattDescriptor descriptor = mEmgCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);

                            if (descriptor == null) {
                                Log.e(TAG, "No descriptor found");
                            } else {
                                Log.d(TAG, "Setting the descriptor");
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                // Put descriptor into write queue
                                mWriteDescriptorQueue.add(descriptor);
                                // If only 1 item in queue --> write it; if more than 1, handle asynchronously
                                // in callback above
                                if (mWriteDescriptorQueue.size() == 1) {
                                    mBluetoothGatt.writeDescriptor(descriptor);
                                }
                            }
                        }
                    }
                }

                BluetoothGattService serviceCommand = gatt.getService(SERVICE_MYO_COMMAND);
                if (serviceCommand == null) {
                    Log.e(TAG, "No Myo control service found");
                } else {
                    Log.d(TAG, "Found Myo control service");
                    // Get Myo Info Characteristic
                    BluetoothGattCharacteristic characteristic = serviceCommand.getCharacteristic(CHAR_MYO_INFO);
                    if (characteristic == null) {
                        Log.e(TAG, "No Myo info characteristic found");
                    } else {
                        Log.d(TAG, "Found Myo characteristic");
                        // Put characteristic into read queue
                        mReadCharacteristicQueue.add(characteristic);
                        // If only 1 item in queue --> read it;
                        // if more than 1, handle asynchronously in callback above
                        // but make sure descriptor writes finished first
                        if ((mReadCharacteristicQueue.size() == 1) && (mWriteDescriptorQueue.size() == 0)) {
                            Log.d(TAG, "Characteristic read success is: " + mBluetoothGatt.readCharacteristic(characteristic));
                        }
                    }
                    // Get command characteristic
                    mCommandCharacteristic = serviceCommand.getCharacteristic(CHAR_COMMAND);
                    if (mCommandCharacteristic == null) {
                        Log.e(TAG, "No characteristic found");
                    } else {
                        Log.d(TAG, "Found characteristic command: " + mCommandCharacteristic);
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onDescriptorWrite: wrote GATT descriptor successfully");
            } else {
                Log.e(TAG, "onDescriptorWrite: ERROR while reading GATT descriptor - " + status);
            }

            // remove item that just finished writing
            mWriteDescriptorQueue.remove();
            // check if there's more to write or read
            if (mWriteDescriptorQueue.size() > 0) {
                mBluetoothGatt.writeDescriptor(mWriteDescriptorQueue.element());
            } else if (mReadCharacteristicQueue.size() > 0) {
                mBluetoothGatt.readCharacteristic(mReadCharacteristicQueue.element());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            mReadCharacteristicQueue.remove();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicRead successful");
                if (CHAR_MYO_FIRMWARE.equals(characteristic.getUuid())) {
                    // Myo firmware info
                    byte[] data = characteristic.getValue();
                    if (data != null && data.length > 0) {
                        ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                        Log.d(TAG, String.format(Locale.getDefault(), "This is version %d.%d.%.d - %d",
                                byteBuffer.getShort(), byteBuffer.getShort(), byteBuffer.getShort(), byteBuffer.getShort()));
                    } else if (data == null) {
                        Log.d(TAG, "Characteristic string is: " + characteristic);
                    }
                } else if (CHAR_MYO_INFO.equals(characteristic.getUuid())) {
                    // Myo device information
                    byte[] data = characteristic.getValue();
                    if (data != null && data.length > 0) {
                        ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

                        Log.d(TAG, String.format(Locale.getDefault(), "Serial Number: %02x:%02x:%02x:%02x:%02x:%02x",
                                byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get()));
                        Log.d(TAG, String.format(Locale.getDefault(), "Unlock: %d",
                                byteBuffer.getShort()));
                        Log.d(TAG, String.format(Locale.getDefault(), "Classifier built-in: %d active: %d (have: %d)",
                                byteBuffer.get(), byteBuffer.get(), byteBuffer.get()));
                        Log.d(TAG, String.format(Locale.getDefault(), "Stream type: %d",
                                byteBuffer.get()));
                    }
                }
            } else {
                Log.e(TAG, "onCharacteristicRead error: " + status);
            }

            if (mReadCharacteristicQueue.size() > 0) {
                mBluetoothGatt.readCharacteristic(mReadCharacteristicQueue.element());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicWrite successful");
            } else {
                Log.e(TAG, "onCharacteristicWrite error: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (CHAR_EMG_0.equals(characteristic.getUuid())) {
                long systemTime = System.currentTimeMillis();
                byte[] emgData = characteristic.getValue();

                ByteBuffer byteBuffer = ByteBuffer.wrap(emgData).order(ByteOrder.LITTLE_ENDIAN);
                Log.d(TAG, String.format(Locale.getDefault(),
                        "EMG data: %5d, %5d, %5d, %5d, %5d, %5d, %5d, %5d\n" +
                                "%5d, %5d, %5d, %5d, %5d, %5d, %5d, %5d,",
                        byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get(),
                        byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get(),
                        byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get(),
                        byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get()));

                byteBuffer = (ByteBuffer) byteBuffer.reset();
                for (int i = 0; i < mEmgData.length; i++) {
                    mEmgData[i] = byteBuffer.get();
                }

                if (systemTime > mLastSendNeverSleep + NEVER_SLEEP_TIME) {
                    // set Myo mode to [Never sleep mode]
                    setMyoControlCommand(MyoCommands.COMMAND_UNSLEEP);
                    mLastSendNeverSleep = systemTime;
                }
            }
        }

        public void stopCallback() {
            Log.d(TAG, "stopCallback is called");
            // Before the closing GATT, set Myo [Normal Sleep Mode].
            setMyoControlCommand(MyoCommands.COMMAND_NORMAL_SLEEP);
            mWriteDescriptorQueue = new LinkedList<>();
            mReadCharacteristicQueue = new LinkedList<>();
            if (mCommandCharacteristic != null) {
                mCommandCharacteristic = null;
            }
            if (mEmgCharacteristic != null) {
                mEmgCharacteristic = null;
            }
            if (mBluetoothGatt != null) {
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
        }

        public void setBluetoothGatt(BluetoothGatt gatt) {
            mBluetoothGatt = gatt;
        }

        public boolean setMyoControlCommand(byte[] command) {
            if (mCommandCharacteristic != null) {
                mCommandCharacteristic.setValue(command);
                int prop = mCommandCharacteristic.getProperties();
                if (prop == BluetoothGattCharacteristic.PROPERTY_WRITE) {
                    if (mBluetoothGatt.writeCharacteristic(mCommandCharacteristic)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public BluetoothGatt getBluetoothGatt() {
            return mBluetoothGatt;
        }
    }

    private MyoGattCallback mMyoGattCallback = new MyoGattCallback();


    public MyoRawSensor(Context context, String deviceAddress, SensorDataProcessor dataHandler) {
        super(context, "Myo-Raw", deviceAddress, dataHandler);
        sendSensorCreated();
    }


    @Override
    public boolean connect() throws Exception {
        super.connect();
        // Previously connected device.  Try to reconnect.
        if (mDeviceAddress != null && mMyoGattCallback.getBluetoothGatt() != null) {
            Log.d(TAG, "Trying to use an existing BluetoothGatt for connection.");
            return mMyoGattCallback.getBluetoothGatt().connect();
        }

        BluetoothDevice device = BleSensorManager.findBtDevice(mDeviceAddress);
        if (device != null) {
            sendNotification("Connecting to... " + device.getName());
            sendConnecting();
            mMyoGattCallback.setBluetoothGatt(device.connectGatt(mContext, false, mMyoGattCallback));
            return true;
        }

        return false;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        Log.d(TAG, "disconnect");
        mMyoGattCallback.stopCallback();
    }

    @Override
    public void startStreaming() {
        Log.d(TAG, "startStreaming");
        // start EMG capture
        if (mMyoGattCallback.getBluetoothGatt() != null) {
            mMyoGattCallback.setMyoControlCommand(MyoCommands.COMMAND_EMG_ONLY);
        }
    }

    @Override
    public void stopStreaming() {
        Log.d(TAG, "stopStreaming");
        // stop EMG capture
        if (mMyoGattCallback.getBluetoothGatt() != null) {
            mMyoGattCallback.setMyoControlCommand(MyoCommands.COMMAND_UNSET_DATA);
            mMyoGattCallback.setMyoControlCommand(MyoCommands.COMMAND_NORMAL_SLEEP);
        }
    }

}
