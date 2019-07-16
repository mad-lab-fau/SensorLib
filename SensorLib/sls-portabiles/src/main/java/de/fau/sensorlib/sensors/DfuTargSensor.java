package de.fau.sensorlib.sensors;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

import de.fau.sensorlib.BleGattAttributes;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.dfu.NilsPodDfuService;
import no.nordicsemi.android.dfu.DfuLogListener;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class DfuTargSensor extends GenericBleSensor implements FirmwareUpgradable {


    /**
     * UUID of Secure DFU (Device Firmware Update) Service
     */
    protected static final UUID SECURE_DFU_SERVICE = UUID.fromString("0000fe59-0000-1000-8000-00805f9b34fb");

    /**
     * UUID for Buttonless DFU Characteristic (write)
     */
    protected static final UUID BUTTONLESS_DFU = UUID.fromString("8ec90003-f315-4f60-9fb8-838830daea50");


    private FirmwareUpgradeListener mFirmwareUpgradeListener;


    private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {

        @Override
        public void onDeviceConnecting(@NonNull String deviceAddress) {
            Log.d(TAG, getDeviceName() + " >> DFU Connecting!");
        }


        @Override
        public void onDfuProcessStarting(@NonNull String deviceAddress) {
            Log.d(TAG, getDeviceName() + " >> DFU Process Starting!");
            setState(SensorState.UPGRADING_FIRMWARE);
            if (mFirmwareUpgradeListener != null) {
                mFirmwareUpgradeListener.onFirmwareUpgradeStart(DfuTargSensor.this);
            } else {
                Log.e(TAG, "No FirmwareUpgradeListener attached!");
            }
        }

        @Override
        public void onDfuCompleted(@NonNull String deviceAddress) {
            Log.d(TAG, getDeviceName() + " >> DFU Completed!");
            setState(SensorState.DISCONNECTED);
            if (mFirmwareUpgradeListener != null) {
                mFirmwareUpgradeListener.onFirmwareUpgradeFinished(DfuTargSensor.this);
            } else {
                Log.e(TAG, "No FirmwareUpgradeListener attached!");
            }
        }

        @Override
        public void onProgressChanged(@NonNull String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            Log.d(TAG, getDeviceName() + " >> DFU Progress: " + percent + "%");
            if (mFirmwareUpgradeListener != null) {
                mFirmwareUpgradeListener.onFirmwareUpgradeProgress(DfuTargSensor.this, percent);
            } else {
                Log.e(TAG, "No FirmwareUpgradeListener attached!");
            }
        }
    };


    private final DfuLogListener mDfuLogListener = new DfuLogListener() {
        @Override
        public void onLogEvent(String deviceAddress, int level, String message) {
            Log.d(TAG, getDeviceName() + " >> " + message);
        }
    };


    static {
        BleGattAttributes.addService(SECURE_DFU_SERVICE, "Secure DFU Service");
        BleGattAttributes.addCharacteristic(BUTTONLESS_DFU, "Buttonless DFU");
    }


    public DfuTargSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
        super(context, info.getDeviceName(), info.getDeviceAddress(), dataHandler);
    }

    @Override
    public void upgradeFirmware(String filePath) {
        final DfuServiceInitiator starter = new DfuServiceInitiator(getDeviceAddress())
                .setDeviceName(getDeviceName())
                .setKeepBond(true);

        starter.setZip(filePath);

        //final DfuServiceController controller =
        starter.start(getContext(), NilsPodDfuService.class);

        DfuServiceListenerHelper.registerLogListener(getContext(), mDfuLogListener);
        DfuServiceListenerHelper.registerProgressListener(getContext(), mDfuProgressListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(getContext());
        }
    }


    @Override
    public void setFirmwareUpgradeListener(FirmwareUpgradeListener listener) {
        mFirmwareUpgradeListener = listener;
    }
}
