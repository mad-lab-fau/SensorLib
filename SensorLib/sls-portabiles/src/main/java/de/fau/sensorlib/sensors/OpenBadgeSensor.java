package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

import de.fau.sensorlib.BleGattAttributes;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;

public class OpenBadgeSensor extends GenericBleSensor {

    private static final String TAG = OpenBadgeSensor.class.getSimpleName();

    /**
     * UUID for UART Streaming Service of OpenBadge sensor
     */
    protected static final UUID NORDIC_UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");

    /**
     * UUID for Config Characteristic (write) of OpenBadges
     */
    protected static final UUID BADGE_COMMANDS = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Streaming Characteristic (notification) of OpenBadges
     */
    protected static final UUID BADGE_STREAMING = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    static {
        BleGattAttributes.addService(NORDIC_UART_SERVICE, "OpenBadge UART Service");
        BleGattAttributes.addCharacteristic(BADGE_COMMANDS, " OpenBadge Commands");
        BleGattAttributes.addCharacteristic(BADGE_STREAMING, " OpenBadge Streaming");
    }

    public OpenBadgeSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
        super(context, info, dataHandler);
    }


    @Override
    protected boolean shouldEnableNotification(BluetoothGattCharacteristic c) {
        if (super.shouldEnableNotification(c)) {
            return true;
        } else if (BADGE_STREAMING.equals(c.getUuid())) {
            return true;
        }

        return false;
    }

    @Override
    public void startStreaming() {
        //super.startStreaming();
        Log.e(TAG, "START STREAMING");
        sendStartRecording();
    }

    @Override
    protected boolean onNewCharacteristicValue(BluetoothGattCharacteristic characteristic, boolean isChange) {
        if (super.onNewCharacteristicValue(characteristic, isChange)) {
            return true;
        } else {
            if (BADGE_STREAMING.equals(characteristic.getUuid())) {
                Log.e(TAG, "STREAM: " + Arrays.toString(characteristic.getValue()));
                return true;
            } else if (BADGE_COMMANDS.equals(characteristic.getUuid())) {
                Log.e(TAG, "COMMAND: " + Arrays.toString(characteristic.getValue()));
                return true;
            }
        }

        return true;
    }

    protected BluetoothGattService getStreamingService() {
        return mGatt.getService(NORDIC_UART_SERVICE);
    }


    protected boolean sendStartRecording() {
        if (getStreamingService() == null) {
            Log.w(TAG, "Service not found");
            return false;
        }

        BluetoothGattCharacteristic characteristic = getStreamingService().getCharacteristic(BADGE_STREAMING);
        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        characteristic.setValue((byte) 'i', BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        characteristic.setValue(5, BluetoothGattCharacteristic.FORMAT_UINT16, 1);

        return writeCharacteristic(characteristic, characteristic.getValue());
    }
}
