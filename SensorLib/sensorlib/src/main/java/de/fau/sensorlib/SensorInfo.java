package de.fau.sensorlib;

/**
 * Basic sensor information that is used to describe all sensors and handle sensors not constructed (yet).
 */
public class SensorInfo {

    /**
     * The descriptive name of this sensor. Should be Human-readable and Human-understandable.
     */
    protected String mName = "Unknown";

    /**
     * The address under which this device can be found, e.g. this can be the Bluetooth MAC-address, or the IP-address for WLAN-connected sensors.
     */
    protected String mDeviceAddress = "n/a";

    /**
     * The class of the device, if it is known and implemented in the sensorlib.
     */
    protected KnownSensor mDeviceClass = null;

    /**
     * @return the device name.
     */
    public String getName() {
        return mName;
    }

    /**
     * @return the address of a given implementation identified by this enum instance.
     */
    public String getDeviceAddress() {
        return mDeviceAddress;
    }

    /**
     * @return the sensor class for this sensor.
     */
    public KnownSensor getDeviceClass() {
        return mDeviceClass;
    }

    /**
     * Default constructor. The sensor class is inferred from the name or device address.
     *
     * @param deviceName    name of the device.
     * @param deviceAddress address of the device.
     */
    public SensorInfo(String deviceName, String deviceAddress) {
        this(deviceName, deviceAddress, KnownSensor.inferSensorClass(deviceName, deviceAddress));
    }

    /**
     * Default constructor.
     *
     * @param deviceName    name of the device.
     * @param deviceAddress address of the device.
     * @param deviceClass   class of the device.
     */
    public SensorInfo(String deviceName, String deviceAddress, KnownSensor deviceClass) {
        mName = deviceName;
        mDeviceAddress = deviceAddress;
        mDeviceClass = deviceClass;
    }

    /**
     * @param deviceAddress address of the device.
     */
    public SensorInfo(String deviceAddress) {
        this(DsSensorManager.getNameForDeviceAddress(deviceAddress), deviceAddress);
    }
}
