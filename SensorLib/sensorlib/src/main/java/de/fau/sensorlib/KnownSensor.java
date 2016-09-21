/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.bluetooth.BluetoothDevice;

/**
 * List of all known sensors implemented in the SensorLib with a descriptive and identifying String for each.
 */
public enum KnownSensor {
    // =============================================================================================
    // = List of all implemented sensors in the SensorLib
    // =============================================================================================
    INTERNAL("<Internal>", new String[]{"n/a"}),
    GENERIC_BLE("Generic BLE Sensor", new String[]{"BLE", "Polar", "miCoach"}),
    GENERIC_SIMULATED("Generic Simulated Sensor", null),
    EMPATICA("Empatica", new String[]{"Empatica"}),
    FITNESS_SHIRT("FitnessSHIRT", new String[]{"FSv3"}),
    MYO("Myo", new String[]{"Myo"}),
    SHIMMER("Shimmer", new String[]{"RN42", "Shimmer3"}),
    SIMBLEE("Simblee", new String[]{"Simblee"}),
    SMARTWATCH("SmartWatch", new String[]{"Moto"}),
    TEK("Bosch TEK", new String[]{"PRM-TECH"}),
    BLE_ECG_SENSOR("BleEcgSensor", new String[]{"POSTAGE"}),
    SMARTBAND2("Smartband 2", new String[]{"SWR12"});
    // =============================================================================================


    private String mDescriptiveName;
    private String[] mIdentifyingKeywords;

    /**
     * Default constructor.
     *
     * @param descriptiveName     a descriptive name for the sensor type/class.
     * @param identifyingKeywords keywords that can be used to identify this sensor type/class given only the name e.g. reported via Bluetooth.
     */
    KnownSensor(String descriptiveName, String[] identifyingKeywords) {
        mDescriptiveName = descriptiveName;
        mIdentifyingKeywords = identifyingKeywords;
    }

    /**
     * @return the device name.
     */
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * @return the address of a given implementation identified by this enum instance.
     */
    public String getDeviceAddress() {
        return mDeviceAddress;
    }

    private String mDeviceName;
    private String mDeviceAddress;

    /**
     * Infers the most likely sensor class based on the device name.
     *
     * @param deviceName    the device name of the sensor/device.
     * @param deviceAddress address of the device.
     * @return the KnownSensors enum corresponding to the given deviceName or null if it could not be associated with any entry.
     */
    public static KnownSensor inferSensorClass(String deviceName, String deviceAddress) {
        for (KnownSensor s : KnownSensor.values()) {
            if (s.mIdentifyingKeywords == null || s.mIdentifyingKeywords.length <= 0)
                continue;
            for (String str : s.mIdentifyingKeywords) {
                if (deviceName == null || deviceName.isEmpty())
                    continue;
                if (deviceName.contains(str)) {
                    s.mDeviceName = deviceName;
                    s.mDeviceAddress = deviceAddress;
                    return s;
                }
            }
        }
        return null;
    }
}
