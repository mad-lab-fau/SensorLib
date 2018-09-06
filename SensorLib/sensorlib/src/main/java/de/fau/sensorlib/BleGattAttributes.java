/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.UUID;

/**
 * Static storage for all used/known GATT attributes and BLE standard definitions.
 */
public final class BleGattAttributes {

    public enum BodySenorLocation {
        OTHER("Other"),
        CHEST("Chest"),
        WRIST("Wrist"),
        FINGER("Finger"),
        HAND("Hand"),
        EAR_LOBE("Ear lobe"),
        FOOT("Foot");

        private String mLocation;

        BodySenorLocation(String location) {
            mLocation = location;
        }

        public static String getLocation(BodySenorLocation location) {
            return (location == null) ? "Unknown" : location.mLocation;
        }

        public static BodySenorLocation inferBodySensorLocation(int location) {
            return BodySenorLocation.values()[location];
        }
    }

    public static final long leastSigBits = 0x800000805f9b34fbL;


    //////////////////////////////////////////////////////
    /////////////////  BLE GATT Services /////////////////
    //////////////////////////////////////////////////////

    /**
     * <b>Generic Access</b> <i>(org.bluetooth.service.generic_access)</i>
     * <p>
     * The generic_access service contains generic information about the device. All available Characteristics are read-only.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.generic_access.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#DEVICE_NAME}</li>
     * <li>{@link BleGattAttributes#APPEARANCE}</li>
     * <li>{@link BleGattAttributes#PERIPHERAL_PRIVACY_FLAG}</li>
     * <li>{@link BleGattAttributes#RECONNECTION_ADDRESS}</li>
     * <li>{@link BleGattAttributes#PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS}</li>
     * </ul>
     */
    public static final UUID GENERIC_ACCESS_SERVICE = new UUID((0x1800L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Alert Notification Service</b> <i>(org.bluetooth.service.alert_notification)</i>
     * <p>
     * The Alert Notification service exposes alert information in a device.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.alert_notification.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#SUPPORTED_NEW_ALERT_CATEGORY}</li>
     * <li>{@link BleGattAttributes#NEW_ALERT}</li>
     * <li>{@link BleGattAttributes#SUPPORTED_UNREAD_ALERT_CATEGORY}</li>
     * <li>{@link BleGattAttributes#UNREAD_ALERT_STATUS}</li>
     * <li>{@link BleGattAttributes#ALERT_NOTIFICATION_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID ALERT_NOTIFICATION_SERVICE = new UUID((0x1811L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Automation IO Service</b> <i>(org.bluetooth.service.automation_io)</i>
     * <p>
     * Used to expose the analog inputs/outputs and digital input/outputs of a generic IO module (IOM).
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.automation_io.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#DIGITAL}</li>
     * <li>{@link BleGattAttributes#ANALOG}</li>
     * <li>{@link BleGattAttributes#AGGREGATE}</li>
     * </ul>
     */
    public static final UUID AUTOMATION_IO_SERVICE = new UUID((0x1815L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Battery Service</b> <i>(org.bluetooth.service.battery_service)</i>
     * <p>
     * Exposes the Battery State and Battery Level of a single battery or set of batteries in a device.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.battery_service.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#BATTERY_LEVEL}</li>
     * </ul>
     */
    public static final UUID BATTERY_SERVICE = new UUID((0x180FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Blood Pressure Service</b> <i>(org.bluetooth.service.blood_pressure)</i>
     * <p>
     * Exposes blood pressure and other data related to a blood pressure monitor.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.blood_pressure.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#BLOOD_PRESSURE_MEASUREMENT}</li>
     * <li>{@link BleGattAttributes#INTERMEDIATE_CUFF_PRESSURE}</li>
     * <li>{@link BleGattAttributes#BLOOD_PRESSURE_FEATURE}</li>
     * </ul>
     */
    public static final UUID BLOOD_PRESSURE_SERVICE = new UUID((0x1810L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Body Composition Service</b> <i>(org.bluetooth.service.body_composition)</i>
     * <p>
     * Exposes data related to body composition from a body composition analyzer (Server) intended for consumer healthcare as well as sports/fitness applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.body_composition.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#BODY_COMPOSITION_FEATURE}</li>
     * <li>{@link BleGattAttributes#BODY_COMPOSITION_MEASUREMENT}</li>
     * </ul>
     */
    public static final UUID BODY_COMPOSITION_SERVICE = new UUID((0x181BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Bond Management Service</b> <i>(org.bluetooth.service.bond_management)</i>
     * <p>
     * Defines how a peer Bluetooth device can manage the storage of bond information, especially the deletion of it, on the Bluetooth device supporting this service.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.bond_management.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#BOND_MANAGEMENT_CONTROL_POINT}</li>
     * <li>{@link BleGattAttributes#BOND_MANAGEMENT_FEATURES}</li>
     * </ul>
     */
    public static final UUID BOND_MANAGEMENT_SERVICE_SERVICE = new UUID((0x181EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Continuous Glucose Monitoring Service</b> <i>(org.bluetooth.service.continuous_glucose_monitoring)</i>
     * <p>
     * Exposes glucose measurement and other data related to a personal CGM sensor for healthcare applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.continuous_glucose_monitoring.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#CGM_MEASUREMENT}</li>
     * <li>{@link BleGattAttributes#CGM_FEATURE}</li>
     * <li>{@link BleGattAttributes#CGM_STATUS}</li>
     * <li>{@link BleGattAttributes#CGM_SESSION_START_TIME}</li>
     * <li>{@link BleGattAttributes#CGM_SESSION_RUN_TIME}</li>
     * <li>{@link BleGattAttributes#RECORD_ACCESS_CONTROL_POINT}</li>
     * <li>{@link BleGattAttributes#CGM_SPECIFIC_OPS_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID CONTINUOUS_GLUCOSE_MONITORING_SERVICE = new UUID((0x181FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Current Time Service</b> <i>(org.bluetooth.service.current_time)</i>
     * <p>
     * Defines how a Bluetooth device can expose time information to other Bluetooth devices.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.current_time.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#CURRENT_TIME}</li>
     * <li>{@link BleGattAttributes#LOCAL_TIME_INFORMATION}</li>
     * <li>{@link BleGattAttributes#REFERENCE_TIME_INFORMATION}</li>
     * </ul>
     */
    public static final UUID CURRENT_TIME_SERVICE_SERVICE = new UUID((0x1805L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Cycling Power Service</b> <i>(org.bluetooth.service.cycling_power)</i>
     * <p>
     * Exposes power- and force-related data and optionally speed- and cadence-related data from a Cycling Power sensor intended for sports and fitness applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.cycling_power.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#CYCLING_POWER_MEASUREMENT}</li>
     * <li>{@link BleGattAttributes#CYCLING_POWER_FEATURE}</li>
     * <li>{@link BleGattAttributes#SENSOR_LOCATION}</li>
     * <li>{@link BleGattAttributes#CYCLING_POWER_VECTOR}</li>
     * <li>{@link BleGattAttributes#CYCLING_POWER_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID CYCLING_POWER_SERVICE = new UUID((0x1818L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Cycling Speed and Cadence Service</b> <i>(org.bluetooth.service.cycling_speed_and_cadence)</i>
     * <p>
     * Exposes speed-related and cadence-related data from a Cycling Speed and Cadence sensor intended for fitness applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.cycling_speed_and_cadence.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#CSC_MEASUREMENT}</li>
     * <li>{@link BleGattAttributes#CSC_FEATURE}</li>
     * <li>{@link BleGattAttributes#SENSOR_LOCATION}</li>
     * <li>{@link BleGattAttributes#SC_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID CYCLING_SPEED_AND_CADENCE_SERVICE = new UUID((0x1816L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Device Information Service</b> <i>(org.bluetooth.service.device_information)</i>
     * <p>
     * Exposes manufacturer and/or vendor information about a device.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.device_information.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#MANUFACTURER_NAME_STRING}</li>
     * <li>{@link BleGattAttributes#MODEL_NUMBER_STRING}</li>
     * <li>{@link BleGattAttributes#SERIAL_NUMBER_STRING}</li>
     * <li>{@link BleGattAttributes#HARDWARE_REVISION_STRING}</li>
     * <li>{@link BleGattAttributes#FIRMWARE_REVISION_STRING}</li>
     * <li>{@link BleGattAttributes#SOFTWARE_REVISION_STRING}</li>
     * <li>{@link BleGattAttributes#SYSTEM_ID}</li>
     * <li>{@link BleGattAttributes#IEEE11073_20601_REGULATORY_CERTIFICATION_DATA_LIST}</li>
     * <li>{@link BleGattAttributes#PNP_ID}</li>
     * </ul>
     */
    public static final UUID DEVICE_INFORMATION_SERVICE = new UUID((0x180AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Environmental Sensing Service</b> <i>(org.bluetooth.service.environmental_sensing)</i>
     * <p>
     * Exposes measurement data from an environmental sensor intended for sports and fitness applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.environmental_sensing.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#DESCRIPTOR_VALUE_CHANGED}</li>
     * <li>{@link BleGattAttributes#APPARENT_WIND_DIRECTION}</li>
     * <li>{@link BleGattAttributes#APPARENT_WIND_SPEED}</li>
     * <li>{@link BleGattAttributes#DEW_POINT}</li>
     * <li>{@link BleGattAttributes#ELEVATION}</li>
     * <li>{@link BleGattAttributes#GUST_FACTOR}</li>
     * <li>{@link BleGattAttributes#HEAT_INDEX}</li>
     * <li>{@link BleGattAttributes#HUMIDITY}</li>
     * <li>{@link BleGattAttributes#IRRADIANCE}</li>
     * <li>{@link BleGattAttributes#POLLEN_CONCENTRATION}</li>
     * <li>{@link BleGattAttributes#RAINFALL}</li>
     * <li>{@link BleGattAttributes#PRESSURE}</li>
     * <li>{@link BleGattAttributes#TEMPERATURE}</li>
     * <li>{@link BleGattAttributes#TRUE_WIND_DIRECTION}</li>
     * <li>{@link BleGattAttributes#TRUE_WIND_SPEED}</li>
     * <li>{@link BleGattAttributes#UV_INDEX}</li>
     * <li>{@link BleGattAttributes#WIND_CHILL}</li>
     * <li>{@link BleGattAttributes#BAROMETRIC_PRESSURE_TREND}</li>
     * <li>{@link BleGattAttributes#MAGNETIC_DECLINATION}</li>
     * <li>{@link BleGattAttributes#MAGNETIC_FLUX_DENSITY_2D}</li>
     * <li>{@link BleGattAttributes#MAGNETIC_FLUX_DENSITY_3D}</li>
     * </ul>
     */
    public static final UUID ENVIRONMENTAL_SENSING_SERVICE = new UUID((0x181AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Fitness Machine Service</b> <i>(org.bluetooth.service.fitness_machine)</i>
     * <p>
     * Exposes training-related data in the sports and fitness environment, which allows a Server (e.g., a fitness machine) to send training-related data to a Client.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.fitness_machine.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#FITNESS_MACHINE_FEATURE}</li>
     * <li>{@link BleGattAttributes#TREADMILL_DATA}</li>
     * <li>{@link BleGattAttributes#CROSS_TRAINER_DATA}</li>
     * <li>{@link BleGattAttributes#STEP_CLIMBER_DATA}</li>
     * <li>{@link BleGattAttributes#STAIR_CLIMBER_DATA}</li>
     * <li>{@link BleGattAttributes#ROWER_DATA}</li>
     * <li>{@link BleGattAttributes#INDOOR_BIKE_DATA}</li>
     * <li>{@link BleGattAttributes#TRAINING_STATUS}</li>
     * <li>{@link BleGattAttributes#SUPPORTED_SPEED_RANGE}</li>
     * <li>{@link BleGattAttributes#SUPPORTED_INCLINATION_RANGE}</li>
     * <li>{@link BleGattAttributes#SUPPORTED_RESISTANCE_LEVEL_RANGE}</li>
     * <li>{@link BleGattAttributes#SUPPORTED_POWER_RANGE}</li>
     * <li>{@link BleGattAttributes#SUPPORTED_HEART_RATE_RANGE}</li>
     * <li>{@link BleGattAttributes#FITNESS_MACHINE_CONTROL_POINT}</li>
     * <li>{@link BleGattAttributes#FITNESS_MACHINE_STATUS}</li>
     * </ul>
     */
    public static final UUID FITNESS_MACHINE_SERVICE = new UUID((0x1826L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Generic Attribute Service</b> <i>(org.bluetooth.service.generic_attribute)</i>
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.generic_attribute.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#SERVICE_CHANGED}</li>
     * </ul>
     */
    public static final UUID GENERIC_ATTRIBUTE_SERVICE = new UUID((0x1801L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Glucose Service</b> <i>(org.bluetooth.service.glucose)</i>
     * <p>
     * Exposes glucose and other data from a glucose sensor for use in consumer and professional healthcare applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.glucose.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#GLUCOSE_MEASUREMENT}</li>
     * <li>{@link BleGattAttributes#GLUCOSE_MEASUREMENT_CONTEXT}</li>
     * <li>{@link BleGattAttributes#GLUCOSE_FEATURE}</li>
     * <li>{@link BleGattAttributes#RECORD_ACCESS_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID GLUCOSE_SERVICE = new UUID((0x1808L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Health Thermometer Service</b> <i>(org.bluetooth.service.health_thermometer)</i>
     * <p>
     * Exposes temperature and other data from a thermometer intended for healthcare and fitness applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.health_thermometer.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#TEMPERATURE_MEASUREMENT}</li>
     * <li>{@link BleGattAttributes#TEMPERATURE_TYPE}</li>
     * <li>{@link BleGattAttributes#INTERMEDIATE_TEMPERATURE}</li>
     * <li>{@link BleGattAttributes#MEASUREMENT_INTERVAL}</li>
     * </ul>
     */
    public static final UUID HEALTH_THERMOMETER_SERVICE = new UUID((0x1809L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Heart Rate Service</b> <i>(org.bluetooth.service.heart_rate)</i>
     * <p>
     * Exposes heart rate and other data from a Heart Rate Sensor intended for fitness applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.heart_rate.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#HEART_RATE_MEASUREMENT}</li>
     * <li>{@link BleGattAttributes#BODY_SENSOR_LOCATION}</li>
     * <li>{@link BleGattAttributes#HEART_RATE_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID HEART_RATE_SERVICE = new UUID((0x180DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>HTTP Proxy Service</b> <i>(org.bluetooth.service.http_proxy)</i>
     * <p>
     * Allows a Client device, typically a sensor, to communicate with a Web Server through a gateway device. The gateway device implements the HTTP Proxy Service and therefore provides the services available through the Internet to the Client sensor device.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.http_proxy.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#URI}</li>
     * <li>{@link BleGattAttributes#HTTP_HEADERS}</li>
     * <li>{@link BleGattAttributes#HTTP_ENTITIY_BODY}</li>
     * <li>{@link BleGattAttributes#HTTP_CONTROL_POINT}</li>
     * <li>{@link BleGattAttributes#HTTP_STATUS_CODE}</li>
     * <li>{@link BleGattAttributes#HTTPS_SECURITY}</li>
     * </ul>
     */
    public static final UUID HTTP_PROXY_SERVICE = new UUID((0x1823L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Human Interface Device Service</b> <i>(org.bluetooth.service.human_interface_device)</i>
     * <p>
     * Exposes the HID reports and other HID data intended for HID Hosts and HID Devices.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.human_interface_device.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#PROTOCOL_MODE}</li>
     * <li>{@link BleGattAttributes#REPORT}</li>
     * <li>{@link BleGattAttributes#REPORT_MAP}</li>
     * <li>{@link BleGattAttributes#BOOT_KEYBOARD_INPUT_REPORT}</li>
     * <li>{@link BleGattAttributes#BOOT_KEYBOARD_OUTPUT_REPORT}</li>
     * <li>{@link BleGattAttributes#BOOT_MOUSE_INPUT_REPORT}</li>
     * <li>{@link BleGattAttributes#HID_INFORMATION}</li>
     * <li>{@link BleGattAttributes#HID_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID HUMAN_INTERFACE_DEVICE_SERVICE = new UUID((0x1812L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Immediate Alert Service</b> <i>(org.bluetooth.service.immediate_alert)</i>
     * <p>
     * Exposes a control point to allow a peer device to cause the device to immediately alert.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.immediate_alert.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#ALERT_LEVEL}</li>
     * </ul>
     */
    public static final UUID IMMEDIATE_ALERT_SERVICE = new UUID((0x1802L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Indoor Positioning Service</b> <i>(org.bluetooth.service.indoor_positioning)</i>
     * <p>
     * Exposes location information to support e.g. mobile handsets to position themselves in an environment where GPS signal is not available, like indoor premises.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.indoor_positioning.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#INDOOR_POSITIONING_CONFIGURATION}</li>
     * <li>{@link BleGattAttributes#LATITUDE}</li>
     * <li>{@link BleGattAttributes#LONGITUDE}</li>
     * <li>{@link BleGattAttributes#LOCAL_NORTH_COORDINATE}</li>
     * <li>{@link BleGattAttributes#LOCAL_EAST_COORDINATE}</li>
     * <li>{@link BleGattAttributes#FLOOR_NUMBER}</li>
     * <li>{@link BleGattAttributes#ALTITUDE}</li>
     * <li>{@link BleGattAttributes#UNCERTAINTY}</li>
     * <li>{@link BleGattAttributes#LOCATION_NAME}</li>
     * </ul>
     */
    public static final UUID INDOOR_POSITIONING_SERVICE = new UUID((0x1821L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Internet Protocol Support Service</b> <i>(org.bluetooth.service.internet_protocol_support)</i>
     * <p>
     * When in a GAP Discoverable Mode for an initial connection to a Router, the Node will include the IP Support Service in the Service UUIDs AD type field of the advertising data.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.internet_protocol_support.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID INTERNET_PROTOCOL_SUPPORT_SERVICE = new UUID((0x1820L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Link Loss Service</b> <i>(org.bluetooth.service.link_loss)</i>
     * <p>
     * Defines behavior when a link is lost between two devices.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.link_loss.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#ALERT_LEVEL}</li>
     * </ul>
     */
    public static final UUID LINK_LOSS_SERVICE = new UUID((0x1803L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Link Loss Service</b> <i>(org.bluetooth.service.location_and_navigation)</i>
     * <p>
     * Exposes location and navigation-related data from a Location and Navigation sensor intended for outdoor activity applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.location_and_navigation.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#LN_FEATURE}</li>
     * <li>{@link BleGattAttributes#LOCATION_AND_SPEED}</li>
     * <li>{@link BleGattAttributes#LN_CONTROL_POINT}</li>
     * <li>{@link BleGattAttributes#NAVIGATION}</li>
     * </ul>
     */
    public static final UUID LOCATION_AND_NAVIGATION_SERVICE = new UUID((0x1819L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Mesh Provisioning Service</b> <i>(org.bluetooth.service.mesh_provisioning)</i>
     * <p>
     * Allows a Provisioning Client to provision a Provisioning Server to allow it to participate in the mesh network.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.mesh_provisioning.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID MESH_PROVISIONING_SERVICE = new UUID((0x1827L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Mesh Proxy Service</b> <i>(org.bluetooth.service.mesh_proxy)</i>
     * <p>
     * Used to enable a server to send and receive Proxy PDUs with a client.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.mesh_proxy.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID MESH_PROXY_SERVICE = new UUID((0x1828L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Next DST Change Service</b> <i>(org.bluetooth.service.next_dst_change)</i>
     * <p>
     * Defines how the information about an upcoming DST change can be exposed using the Generic Attribute Profile (GATT).
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.next_dst_change.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#TIME_WITH_DST}</li>
     * </ul>
     */
    public static final UUID NEXT_DST_CHANGE_SERVICE = new UUID((0x1807L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object Transfer Service</b> <i>(org.bluetooth.service.object_transfer)</i>
     * <p>
     * Provides management and control features supporting bulk data transfers which occur via a separate L2CAP connection oriented channel.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.object_transfer.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#OTS_FEATURE}</li>
     * <li>{@link BleGattAttributes#OBJECT_NAME}</li>
     * <li>{@link BleGattAttributes#OBJECT_TYPE}</li>
     * <li>{@link BleGattAttributes#OBJECT_SIZE}</li>
     * <li>{@link BleGattAttributes#OBJECT_FIRST_CREATED}</li>
     * <li>{@link BleGattAttributes#OBJECT_LAST_MODIFIED}</li>
     * <li>{@link BleGattAttributes#OBJECT_ID}</li>
     * <li>{@link BleGattAttributes#OBJECT_PROPERTIES}</li>
     * <li>{@link BleGattAttributes#OBJECT_ACTION_CONTROL_POINT}</li>
     * <li>{@link BleGattAttributes#OBJECT_LIST_CONTROL_POINT}</li>
     * <li>{@link BleGattAttributes#OBJECT_LIST_FILTER}</li>
     * <li>{@link BleGattAttributes#OBJECT_CHANGED}</li>
     * </ul>
     */
    public static final UUID OBJECT_TRANSFER_SERVICE = new UUID((0x1825L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Phone Alert Status Service</b> <i>(org.bluetooth.service.phone_alert_status)</i>
     * <p>
     * Exposes the phone alert status when in a connection.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.phone_alert_status.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#ALERT_STATUS}</li>
     * <li>{@link BleGattAttributes#RINGER_SETTING}</li>
     * <li>{@link BleGattAttributes#RINGER_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID PHONE_ALERT_STATUS_SERVICE = new UUID((0x180EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Pulse Oximeter Service</b> <i>(org.bluetooth.service.pulse_oximeter)</i>
     * <p>
     * Proposes a pulse oximetry server for use in consumer and professional healthcare applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.pulse_oximeter.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#PLX_SPOT_CHECK_MEASUREMENT}</li>
     * <li>{@link BleGattAttributes#PLX_CONTINUOUS_MEASUREMENT}</li>
     * <li>{@link BleGattAttributes#PLX_FEATURES}</li>
     * <li>{@link BleGattAttributes#RECORD_ACCESS_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID PULSE_OXIMETER_SERVICE = new UUID((0x1822L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Reconnection Configuration Service</b> <i>(org.bluetooth.service.reconnection_configuration)</i>
     * <p>
     * Enables the control of certain communication parameters of a Bluetooth Low Energy peripheral device.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.reconnection_configuration.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#RC_FEATURE}</li>
     * <li>{@link BleGattAttributes#RC_SETTINGS}</li>
     * <li>{@link BleGattAttributes#RECONNECTION_CONFIGURATION_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID RECONNECTION_CONFIGURATION_SERVICE = new UUID((0x1829L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Reference Time Update Service</b> <i>(org.bluetooth.service.reference_time_update)</i>
     * <p>
     * Defines how a client can request an update from a reference time source from a time server using the Generic Attribute Profile (GATT).
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.reference_time_update.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#TIME_UPDATE_CONTROL_POINT}</li>
     * <li>{@link BleGattAttributes#TIME_UPDATE_STATE}</li>
     * </ul>
     */
    public static final UUID REFERENCE_TIME_UPDATE_SERVICE = new UUID((0x1806L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Running Speed and Cadence Service</b> <i>(org.bluetooth.service.running_speed_and_cadence)</i>
     * <p>
     * Exposes speed, cadence and other data from a Running Speed and Cadence Sensor intended for fitness applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.running_speed_and_cadence.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#RSC_MEASUREMENT}</li>
     * <li>{@link BleGattAttributes#RSC_FEATURE}</li>
     * <li>{@link BleGattAttributes#SENSOR_LOCATION}</li>
     * <li>{@link BleGattAttributes#SC_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID RUNNING_SPEED_AND_CADENCE_SERVICE = new UUID((0x1814L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Scan Parameter Service</b> <i>(org.bluetooth.service.scan_parameters)</i>
     * <p>
     * Enables a GATT Client to store the LE scan parameters it is using on a GATT Server device so that the GATT Server can utilize the information to adjust behavior to optimize power consumption and/or reconnection latency.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.scan_parameters.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#SCAN_INTERVAL_WINDOW}</li>
     * <li>{@link BleGattAttributes#SCAN_REFRESH}</li>
     * </ul>
     */
    public static final UUID SCAN_PARAMETER_SERVICE = new UUID((0x1813L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Transport Discovery Service</b> <i>(org.bluetooth.service.transport_discovery)</i>
     * <p>
     * Enables a device using Bluetooth low energy wireless technology to expose services that are available on a transport other than Bluetooth low energy.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.transport_discovery.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#TDS_CONTROL_POINT}</li>
     * </ul>
     */
    public static final UUID TRANSPORT_DISCOVERY_SERVICE = new UUID((0x1824L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Tx Power Service</b> <i>(org.bluetooth.service.tx_power)</i>
     * <p>
     * Exposes a device’s current transmit power level when in a connection.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.tx_power.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#TX_POWER_LEVEL}</li>
     * </ul>
     */
    public static final UUID TX_POWER_SERVICE = new UUID((0x1804L << 32) | 0x1000, leastSigBits);

    /**
     * <b>User Data Service</b> <i>(org.bluetooth.service.user_data)</i>
     * <p>
     * Exposes user-related data in the sports and fitness environment.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.user_data.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#FIRST_NAME}</li>
     * <li>{@link BleGattAttributes#LAST_NAME}</li>
     * <li>{@link BleGattAttributes#EMAIL_ADDRESS}</li>
     * <li>{@link BleGattAttributes#AGE}</li>
     * <li>{@link BleGattAttributes#DATE_OF_BIRTH}</li>
     * <li>{@link BleGattAttributes#GENDER}</li>
     * <li>{@link BleGattAttributes#WEIGHT}</li>
     * <li>{@link BleGattAttributes#HEIGHT}</li>
     * <li>{@link BleGattAttributes#VO2_MAX}</li>
     * <li>{@link BleGattAttributes#HEART_RATE_MAX}</li>
     * <li>{@link BleGattAttributes#RESTING_HEART_RATE}</li>
     * <li>{@link BleGattAttributes#MAXIMUM_RECOMMENDED_HEART_RATE}</li>
     * <li>{@link BleGattAttributes#AEROBIC_THRESHOLD}</li>
     * <li>{@link BleGattAttributes#ANAEROBIC_THRESHOLD}</li>
     * <li>{@link BleGattAttributes#SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS}</li>
     * <li>{@link BleGattAttributes#DATE_OF_THRESHOLD_ASSESSMENT}</li>
     * <li>{@link BleGattAttributes#WAIST_CIRCUMFERENCE}</li>
     * <li>{@link BleGattAttributes#HIP_CIRCUMFERENCE}</li>
     * <li>{@link BleGattAttributes#FAT_BURN_HEART_RATE_LOWER_LIMIT}</li>
     * <li>{@link BleGattAttributes#FAT_BURN_HEART_RATE_UPPER_LIMIT}</li>
     * <li>{@link BleGattAttributes#AEROBIC_HEART_RATE_LOWER_LIMIT}</li>
     * <li>{@link BleGattAttributes#AEROBIC_HEART_RATE_UPPER_LIMIT}</li>
     * <li>{@link BleGattAttributes#ANAEROBIC_HEART_RATE_LOWER_LIMIT}</li>
     * <li>{@link BleGattAttributes#ANAEROBIC_HEART_RATE_UPPER_LIMIT}</li>
     * <li>{@link BleGattAttributes#FIVE_ZONE_HEART_RATE_LIMITS}</li>
     * <li>{@link BleGattAttributes#THREE_ZONE_HEART_RATE_LIMITS}</li>
     * <li>{@link BleGattAttributes#TWO_ZONE_HEART_RATE_LIMIT}</li>
     * <li>{@link BleGattAttributes#DATABASE_CHANGE_INCREMENT}</li>
     * <li>{@link BleGattAttributes#USER_INDEX}</li>
     * <li>{@link BleGattAttributes#USER_CONTROL_POINT}</li>
     * <li>{@link BleGattAttributes#LANGUAGE}</li>
     * </ul>
     */
    public static final UUID USER_DATA_SERVICE = new UUID((0x181CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Weight Scale Service</b> <i>(org.bluetooth.service.weight_scale)</i>
     * <p>
     * Exposes weight and related data from a weight scale intended for consumer healthcare and sports/fitness applications.
     * <p>
     * See the <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.weight_scale.xml>Bluetooth SIG</a> specification for further information.
     * <p>
     * Characteristics included in this service:
     * <ul>
     * <li>{@link BleGattAttributes#WEIGHT_SCALE_FEATURE}</li>
     * <li>{@link BleGattAttributes#WEIGHT_MEASUREMENT}</li>
     * </ul>
     */
    public static final UUID WEIGHT_SCALE_SERVICE = new UUID((0x181DL << 32) | 0x1000, leastSigBits);


    //////////////////////////////////////////////////////
    /////////////  BLE GATT Characteristics //////////////
    //////////////////////////////////////////////////////

    /**
     * <b>Aerobic Heart Rate Lower Limit</b> <i>(org.bluetooth.characteristic.aerobic_heart_rate_lower_limit)</i>
     * <p>
     * Lower limit of the heart rate where the user enhances his endurance while exercising.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.aerobic_heart_rate_lower_limit.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID AEROBIC_HEART_RATE_LOWER_LIMIT = new UUID((0x2A7EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Aerobic Heart Rate Upper Limit</b> <i>(org.bluetooth.characteristic.aerobic_heart_rate_upper_limit)</i>
     * <p>
     * Upper limit of the heart rate where the user enhances his endurance while exercising.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.aerobic_heart_rate_upper_limit.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID AEROBIC_HEART_RATE_UPPER_LIMIT = new UUID((0x2A84L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Aerobic Threshold</b> <i>(org.bluetooth.characteristic.aerobic_threshold)</i>
     * <p>
     * First metabolic threshold.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.aerobic_threshold.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID AEROBIC_THRESHOLD = new UUID((0x2A7FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Age</b> <i>(org.bluetooth.characteristic.age)</i>
     * <p>
     * Age of the User.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.age.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID AGE = new UUID((0x2A80L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Aggregate</b> <i>(org.bluetooth.characteristic.age)</i>
     * <p>
     * The Aggregate Input is an aggregate of the Digital Input Characteristic value (if available) and ALL Analog Inputs available.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.age.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID AGGREGATE = new UUID((0x2A5AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Alert Category ID</b> <i>(org.bluetooth.characteristic.alert_category_id)</i>
     * <p>
     * The Alert Category ID characteristic defines the predefined categories of messages as an enumeration.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_category_id.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ALERT_CATEGORY_ID = new UUID((0x2A43L << 32) | 0x1000, leastSigBits);
    /**
     * <b>Alert Category ID Bit Mask</b> <i>(org.bluetooth.characteristic.alert_category_id_bit_mask)</i>
     * <p>
     * The Alert Category ID Bit Mask characteristic defines one bit for each predefined category ID.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_category_id_bit_mask.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ALERT_CATEGORY_ID_BIT_MASK = new UUID((0x2A42L << 32) | 0x1000, leastSigBits);
    /**
     * <b>Alert Level</b> <i>(org.bluetooth.characteristic.alert_level)</i>
     * <p>
     * The level of an alert a device is to sound. If this level is changed while the alert is being sounded, the new level should take effect.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_level.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ALERT_LEVEL = new UUID((0x2A06L << 32) | 0x1000, leastSigBits);
    /**
     * <b>Alert Notification Control Point</b> <i>(org.bluetooth.characteristic.alert_notification_control_point)</i>
     * <p>
     * Control point of the Alert Notification server. Client can write the command here to request the several functions toward the server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_notification_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ALERT_NOTIFICATION_CONTROL_POINT = new UUID((0x2A44L << 32) | 0x1000, leastSigBits);
    /**
     * <b>Alert Status</b> <i>(org.bluetooth.characteristic.alert_status)</i>
     * <p>
     * The Alert Status characteristic defines the Status of alert.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_status.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ALERT_STATUS = new UUID((0x2A3FL << 32) | 0x1000, leastSigBits);
    /**
     * <b>Altitude</b> <i>(org.bluetooth.characteristic.altitude)</i>
     * <p>
     * The Altitude characteristic describes the altitude of the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.altitude.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ALTITUDE = new UUID((0x2A3FL << 32) | 0x1000, leastSigBits);
    /**
     * <b>Anaerobic Heart Rate Lower Limit</b> <i>(org.bluetooth.characteristic.anaerobic_heart_rate_lower_limit)</i>
     * <p>
     * Lower limit of the heart rate where the user enhances his anaerobic tolerance while exercising.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.anaerobic_heart_rate_lower_limit.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ANAEROBIC_HEART_RATE_LOWER_LIMIT = new UUID((0x2A81L << 32) | 0x1000, leastSigBits);
    /**
     * <b>Anaerobic Heart Rate Upper Limit</b> <i>(org.bluetooth.characteristic.anaerobic_heart_rate_upper_limit)</i>
     * <p>
     * Upper limit of the heart rate where the user enhances his anaerobic tolerance while exercising.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.anaerobic_heart_rate_upper_limit.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ANAEROBIC_HEART_RATE_UPPER_LIMIT = new UUID((0x2A82L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Anaerobic Threshold</b> <i>(org.bluetooth.characteristic.aerobic_threshold)</i>
     * <p>
     * Second metabolic threshold.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.anaerobic_threshold.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ANAEROBIC_THRESHOLD = new UUID((0x2A83L << 32) | 0x1000, leastSigBits);
    /**
     * <b>Analog</b> <i>(org.bluetooth.characteristic.analog)</i>
     * <p>
     * Used to read or write the value of one of the IO Module's analog signals.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.analog.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ANALOG = new UUID((0x2A58L << 32) | 0x1000, leastSigBits);
    /**
     * <b>Analog Output</b> <i>(org.bluetooth.characteristic.analog_output)</i>
     * <p>
     * Represents the value of an analog output as a unsigned 16-bit integer. The format of the analog value depends on the implementation.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.analog_output.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ANALOG_OUTPUT = new UUID((0x2A59L << 32) | 0x1000, leastSigBits);
    /**
     * <b>Apparent Wind Direction</b> <i>(org.bluetooth.characteristic.apparent_wind_direction)</i>
     * <p>
     * Wind experienced by an observer in motion. Measured as the relative velocity of the wind in relation to the observer.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.apparent_wind_direction.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID APPARENT_WIND_DIRECTION = new UUID((0x2A73L << 32) | 0x1000, leastSigBits);
    /**
     * <b>Apparent Wind Speed</b> <i>(org.bluetooth.characteristic.apparent_wind_speed)</i>
     * <p>
     * Wind experienced by an observer in motion. Measured as the relative velocity of the wind in relation to the observer.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.apparent_wind_speed.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID APPARENT_WIND_SPEED = new UUID((0x2A72L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Appearance</b> <i>(org.bluetooth.characteristic.gap.appearance)</i>
     * <p>
     * The external appearance of this device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.appearance.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID APPEARANCE = new UUID((0x2A01L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Barometric Pressure Trend</b> <i>(org.bluetooth.characteristic.gap.barometric_pressure_trend)</i>
     * <p>
     * Trend of barometric pressure.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.barometric_pressure_trend.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BAROMETRIC_PRESSURE_TREND = new UUID((0x2AA3L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Battery Level</b> <i>(org.bluetooth.characteristic.battery_level)</i>
     * <p>
     * The current charge level of a battery. 100% represents fully charged while 0% represents fully discharged.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.battery_level.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BATTERY_LEVEL = new UUID((0x2A19L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Battery Level State</b> <i>(org.bluetooth.characteristic.battery_level_state)</i>
     * <p>
     * Combination of Battery Level ({@link BleGattAttributes#BATTERY_LEVEL}) and Battery Power State ({@link BleGattAttributes#BATTERY_POWER_STATE}).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.battery_level_state.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BATTERY_LEVEL_STATE = new UUID((0x2A1BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Battery Power State</b> <i>(org.bluetooth.characteristic.battery_power_state)</i>
     * <p>
     * Current battery state.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.battery_power_state.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BATTERY_POWER_STATE = new UUID((0x2A1AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Blood Pressure Feature</b> <i>(org.bluetooth.characteristic.blood_pressure_feature)</i>
     * <p>
     * Describes the supported features of the Blood Pressure Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.blood_pressure_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BLOOD_PRESSURE_FEATURE = new UUID((0x2A49L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Blood Pressure Measurement</b> <i>(org.bluetooth.characteristic.blood_pressure_measurement)</i>
     * <p>
     * Contains measurement values from the Blood Pressure Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.blood_pressure_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BLOOD_PRESSURE_MEASUREMENT = new UUID((0x2A35L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Body Composition Feature</b> <i>(org.bluetooth.characteristic.body_composition_feature)</i>
     * <p>
     * Describes the supported features of the Body Composition Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.body_composition_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BODY_COMPOSITION_FEATURE = new UUID((0x2A9BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Body Composition Measurement</b> <i>(org.bluetooth.characteristic.body_composition_measurement)</i>
     * <p>
     * Contains measurement values from the Body Composition Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.body_composition_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BODY_COMPOSITION_MEASUREMENT = new UUID((0x2A9CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Body Sensor Location</b> <i>(org.bluetooth.characteristic.body_sensor_location)</i>
     * <p>
     * Describes on-body location of sensor device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.body_sensor_location.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BODY_SENSOR_LOCATION = new UUID((0x2A38L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Bond Management Control Point</b> <i>(org.bluetooth.characteristic.bond_management_control_point)</i>
     * <p>
     * Encapsulates functionality and mechanisms to control the bonds of a device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.bond_management_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BOND_MANAGEMENT_CONTROL_POINT = new UUID((0x2AA4L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Bond Management Control Point</b> <i>(org.bluetooth.characteristic.bond_management_feature)</i>
     * <p>
     * Describes features of the Bond Management Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.bond_management_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BOND_MANAGEMENT_FEATURES = new UUID((0x2AA5L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Boot Keyboard Input Report</b> <i>(org.bluetooth.characteristic.boot_keyboard_input_report)</i>
     * <p>
     * Transfers fixed format and length Input Report data between a HID Host operating in Boot Protocol Mode and a HID Service corresponding to a boot keyboard.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.boot_keyboard_input_report.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BOOT_KEYBOARD_INPUT_REPORT = new UUID((0x2A22L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Boot Keyboard Output Report</b> <i>(org.bluetooth.characteristic.boot_keyboard_output_report)</i>
     * <p>
     * Transfers fixed format and length Output Report data between a HID Host operating in Boot Protocol Mode and a HID Service corresponding to a boot keyboard.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.boot_keyboard_output_report.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BOOT_KEYBOARD_OUTPUT_REPORT = new UUID((0x2A32L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Boot Mouse Input Report</b> <i>(org.bluetooth.characteristic.boot_mouse_input_report)</i>
     * <p>
     * Transfer fixed format and length Input Report data between a HID Host operating in Boot Protocol Mode and a HID Service corresponding to a boot mouse.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.boot_mouse_input_report.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID BOOT_MOUSE_INPUT_REPORT = new UUID((0x2A33L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Central Address Resolution</b> <i>(org.bluetooth.characteristic.gap.central_address_resolution)</i>
     * <p>
     * The Peripheral checks if the peer device supports address resolution by reading the Central Address Resolution characteristic before using directed advertisement where the initiator address is set to a Resolvable Private Address (RPA).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.central_address_resolution.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CENTRAL_ADDRESS_RESOLUTION = new UUID((0x2AA6L << 32) | 0x1000, leastSigBits);

    /**
     * <b>CGM Feature</b> <i>(org.bluetooth.characteristic.cgm_feature)</i>
     * <p>
     * Describes features of the Continuous Glucose Monitoring (CGM) Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cgm_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CGM_FEATURE = new UUID((0x2AA8L << 32) | 0x1000, leastSigBits);

    /**
     * <b>CGM Measurement</b> <i>(org.bluetooth.characteristic.cgm_measurement)</i>
     * <p>
     * Contains measurement values from the Continuous Glucose Monitoring (CGM) Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cgm_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CGM_MEASUREMENT = new UUID((0x2AA8L << 32) | 0x1000, leastSigBits);

    /**
     * <b>CGM Session Run Time</b> <i>(org.bluetooth.characteristic.cgm_session_run_time)</i>
     * <p>
     * Describes the session run time of the Continuous Glucose Monitoring (CGM) Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cgm_session_run_time.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CGM_SESSION_RUN_TIME = new UUID((0x2AABL << 32) | 0x1000, leastSigBits);

    /**
     * <b>CGM Session Start Time</b> <i>(org.bluetooth.characteristic.cgm_session_start_time)</i>
     * <p>
     * Describes the session start time of the Continuous Glucose Monitoring (CGM) Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cgm_session_start_time.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CGM_SESSION_START_TIME = new UUID((0x2AAAL << 32) | 0x1000, leastSigBits);

    /**
     * <b>CGM Specific Ops Control Point</b> <i>(org.bluetooth.characteristic.cgm_specific_ops_control_point)</i>
     * <p>
     * Encapsulates all functionality and mechanisms that are unique to a CGM-device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cgm_specific_ops_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CGM_SPECIFIC_OPS_CONTROL_POINT = new UUID((0x2AACL << 32) | 0x1000, leastSigBits);

    /**
     * <b>CGM Status</b> <i>(org.bluetooth.characteristic.cgm_status)</i>
     * <p>
     * Allows the Collector to actively request the current status from the CGM Sensor, particularly when the CGM measurement is not running and the status cannot be given in the measurement result in the Status Annunciation.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cgm_status.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CGM_STATUS = new UUID((0x2AA9L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Cross Trainer Data</b> <i>(org.bluetooth.characteristic.cross_trainer_data)</i>
     * <p>
     * Sends training-related data to the Client from a cross trainer (Server).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cross_trainer_data.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CROSS_TRAINER_DATA = new UUID((0x2ACEL << 32) | 0x1000, leastSigBits);

    /**
     * <b>CSC Feature</b> <i>(org.bluetooth.characteristic.csc_feature)</i>
     * <p>
     * Describes the supported features of the Cycling Speed and Cadence (CSC) Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.csc_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CSC_FEATURE = new UUID((0x2A5CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>CSC Measurement</b> <i>(org.bluetooth.characteristic.csc_measurement)</i>
     * <p>
     * Contains measurement values from the Cycling Speed and Cadence (CSC) Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.csc_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CSC_MEASUREMENT = new UUID((0x2A5BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Current Time</b> <i>(org.bluetooth.characteristic.current_time)</i>
     * <p>
     * Describes the current time.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.current_time.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CURRENT_TIME = new UUID((0x2A2BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Cycling Power Control Point</b> <i>(org.bluetooth.characteristic.cycling_power_control_point)</i>
     * <p>
     * Request a specific function to be executed on the receiving device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cycling_power_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CYCLING_POWER_CONTROL_POINT = new UUID((0x2A66L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Cycling Power Feature</b> <i>(org.bluetooth.characteristic.cycling_power_feature)</i>
     * <p>
     * Reports a list of features supported by the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cycling_power_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CYCLING_POWER_FEATURE = new UUID((0x2A65L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Cycling Power Measurement</b> <i>(org.bluetooth.characteristic.cycling_power_measurement)</i>
     * <p>
     * Contains measurement values from the Cycling Power (CSC) Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cycling_power_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CYCLING_POWER_MEASUREMENT = new UUID((0x2A63L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Cycling Power Vector</b> <i>(org.bluetooth.characteristic.cycling_power_vector)</i>
     * <p>
     * Contains measurement values from the Cycling Power (CSC) Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.cycling_power_vector.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CYCLING_POWER_VECTOR = new UUID((0x2A64L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Database Change Increment</b> <i>(org.bluetooth.characteristic.database_change_increment)</i>
     * <p>
     * Describes the database change increment.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.database_change_increment.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DATABASE_CHANGE_INCREMENT = new UUID((0x2A99L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Date of Birth</b> <i>(org.bluetooth.characteristic.date_of_birth)</i>
     * <p>
     * Describes the date of birth.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.date_of_birth.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DATE_OF_BIRTH = new UUID((0x2A85L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Date of Threshold Assessment</b> <i>(org.bluetooth.characteristic.date_of_threshold_assessment)</i>
     * <p>
     * Describes the date of threshold assessment.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.date_of_threshold_assessment.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DATE_OF_THRESHOLD_ASSESSMENT = new UUID((0x2A86L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Date Time</b> <i>(org.bluetooth.characteristic.date_time)</i>
     * <p>
     * Represents time.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.date_time.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DATE_TIME = new UUID((0x2A08L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Day Date Time</b> <i>(org.bluetooth.characteristic.day_date_time)</i>
     * <p>
     * Represents day time.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.day_date_time.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DAY_DATE_TIME = new UUID((0x2A0AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Day of Week</b> <i>(org.bluetooth.characteristic.day_of_week)</i>
     * <p>
     * Represents day of week time.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.day_of_week.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DAY_OF_WEEK = new UUID((0x2A09L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Descriptor Value Changed</b> <i>(org.bluetooth.characteristic.descriptor_value_changed)</i>
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.descriptor_value_changed.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DESCRIPTOR_VALUE_CHANGED = new UUID((0x2A7DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Device Name</b> <i>(org.bluetooth.characteristic.gap.device_name)</i>
     * <p>
     * Represents the device name.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.device_name.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DEVICE_NAME = new UUID((0x2A00L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Dew Point</b> <i>(org.bluetooth.characteristic.dew_point)</i>
     * <p>
     * Represents the dew point.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.dew_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DEW_POINT = new UUID((0x2A7BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Digital</b> <i>(org.bluetooth.characteristic.digital)</i>
     * <p>
     * Exposes and changes the state of an IO Module's digital signals.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.digital.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DIGITAL = new UUID((0x2A56L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Digital Output</b> <i>(org.bluetooth.characteristic.digital_output)</i>
     * <p>
     * Represents the value of an IO Module's digital signal as an array of n 2-bit values in a bit field.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.digital_output.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DIGITAL_OUTPUT = new UUID((0x2A57L << 32) | 0x1000, leastSigBits);

    /**
     * <b>DST Offset</b> <i>(org.bluetooth.characteristic.dst_offset)</i>
     * <p>
     * Represents the Daylight Saving Time (DST) offset.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.dst_offset.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID DST_OFFSET = new UUID((0x2A0DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Elevation</b> <i>(org.bluetooth.characteristic.elevation)</i>
     * <p>
     * Represents the elevation of the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.elevation.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ELEVATION = new UUID((0x2A6CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Email Address</b> <i>(org.bluetooth.characteristic.email_address)</i>
     * <p>
     * Represents the email address of the user.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.email_address.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID EMAIL_ADDRESS = new UUID((0x2A87L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Exact Time 100</b> <i>(org.bluetooth.characteristic.exact_time_100)</i>
     * <p>
     * Represents the exact time in fractions of 100.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.exact_time_100.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID EXACT_TIME_100 = new UUID((0x2A0BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Exact Time 256</b> <i>(org.bluetooth.characteristic.exact_time_256)</i>
     * <p>
     * Represents the exact time in fractions of 256.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.exact_time_256.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID EXACT_TIME_256 = new UUID((0x2A0CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Fat Burn Heart Rate Lower Limit</b> <i>(org.bluetooth.characteristic.fat_burn_heart_rate_lower_limit)</i>
     * <p>
     * Lower limit of the heart rate where the user maximizes the fat burn while exercising.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.fat_burn_heart_rate_lower_limit.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID FAT_BURN_HEART_RATE_LOWER_LIMIT = new UUID((0x2A88L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Fat Burn Heart Rate Upper Limit</b> <i>(org.bluetooth.characteristic.fat_burn_heart_rate_upper_limit)</i>
     * <p>
     * Upper limit of the heart rate where the user maximizes the fat burn while exercising.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.fat_burn_heart_rate_upper_limit.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID FAT_BURN_HEART_RATE_UPPER_LIMIT = new UUID((0x2A89L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Firmware Revision String</b> <i>(org.bluetooth.characteristic.firmware_revision_string)</i>
     * <p>
     * Represents the firmware revision for the firmware within the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.firmware_revision_string.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID FIRMWARE_REVISION_STRING = new UUID((0x2A26L << 32) | 0x1000, leastSigBits);

    /**
     * <b>First Name</b> <i>(org.bluetooth.characteristic.first_name)</i>
     * <p>
     * Represents the first name of the user.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.first_name.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID FIRST_NAME = new UUID((0x2A8AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Fitness Machine Control Point</b> <i>(org.bluetooth.characteristic.fitness_machine_control_point)</i>
     * <p>
     * Encapsulates all functionality and mechanisms that are unique to a Fitness Machine device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.fitness_machine_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID FITNESS_MACHINE_CONTROL_POINT = new UUID((0x2AD9L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Fitness Machine Feature</b> <i>(org.bluetooth.characteristic.fitness_machine_feature)</i>
     * <p>
     * Describes the supported features of the Fitness Machine Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.fitness_machine_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID FITNESS_MACHINE_FEATURE = new UUID((0x2ACCL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Fitness Machine Status</b> <i>(org.bluetooth.characteristic.fitness_machine_status)</i>
     * <p>
     * Describes the status of the Fitness Machine Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.fitness_machine_status.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID FITNESS_MACHINE_STATUS = new UUID((0x2ADAL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Five Zone Heart Rate Limits</b> <i>(org.bluetooth.characteristic.five_zone_heart_rate_limits)</i>
     * <p>
     * Contains the limits between the heart rate zones for the 5-zone heart rate definition (Maximum, Hard, Moderate, Light and Very Light).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.five_zone_heart_rate_limits.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID FIVE_ZONE_HEART_RATE_LIMITS = new UUID((0x2A8BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Floor Number</b> <i>(org.bluetooth.characteristic.floor_number)</i>
     * <p>
     * Describes in which floor the device is installed.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.floor_number.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID FLOOR_NUMBER = new UUID((0x2AB2L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Gender</b> <i>(org.bluetooth.characteristic.gender)</i>
     * <p>
     * Gender of the user.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gender.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID GENDER = new UUID((0x2A8CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Glucose Feature</b> <i>(org.bluetooth.characteristic.glucose_feature)</i>
     * <p>
     * Describes the supported features of the Glucose Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.glucose_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID GLUCOSE_FEATURE = new UUID((0x2A51L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Glucose Measurement</b> <i>(org.bluetooth.characteristic.glucose_measurement)</i>
     * <p>
     * Contains measurement values from the Glucose Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.glucose_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID GLUCOSE_MEASUREMENT = new UUID((0x2A18L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Glucose Measurement Context</b> <i>(org.bluetooth.characteristic.glucose_measurement_context)</i>
     * <p>
     * Describes the context of the Glucose Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.glucose_measurement_context.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID GLUCOSE_MEASUREMENT_CONTEXT = new UUID((0x2A34L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Gust Factor</b> <i>(org.bluetooth.characteristic.gust_factor)</i>
     * <p>
     * Describes the gust factor.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gust_factor.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID GUST_FACTOR = new UUID((0x2A74L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Hardware Revision String</b> <i>(org.bluetooth.characteristic.hardware_revision_string)</i>
     * <p>
     * Represents the hardware revision for the hardware within the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.hardware_revision_string.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HARDWARE_REVISION_STRING = new UUID((0x2A27L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Heart Rate Control Point</b> <i>(org.bluetooth.characteristic.heart_rate_control_point)</i>
     * <p>
     * Represents a heart rate control point.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.heart_rate_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HEART_RATE_CONTROL_POINT = new UUID((0x2A39L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Heart Rate Max</b> <i>(org.bluetooth.characteristic.heart_rate_max)</i>
     * <p>
     * Maximum heart rate a user can reach.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.heart_rate_max.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HEART_RATE_MAX = new UUID((0x2A8DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Heart Rate Measurement</b> <i>(org.bluetooth.characteristic.heart_rate_measurement)</i>
     * <p>
     * Contains measurement values from the Heart Rate (HR) Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.heart_rate_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HEART_RATE_MEASUREMENT = new UUID((0x2A37L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Heat Index</b> <i>(org.bluetooth.characteristic.heat_index)</i>
     * <p>
     * Represents heat index values.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.heat_index.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HEAT_INDEX = new UUID((0x2A7AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Height</b> <i>(org.bluetooth.characteristic.height)</i>
     * <p>
     * Height of the User.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.height.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HEIGHT = new UUID((0x2A8EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>HID Control Point</b> <i>(org.bluetooth.characteristic.hid_control_point)</i>
     * <p>
     * Control-point attribute that defines the HID Commands Suspend and Exit Suspend when written.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.hid_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HID_CONTROL_POINT = new UUID((0x2A4CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>HID Information</b> <i>(org.bluetooth.characteristic.hid_information)</i>
     * <p>
     * Contains the HID attributes. The value of this Characteristic is static and can be cached for the lifetime of the bond between the HID device and the HID host.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.hid_information.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HID_INFORMATION = new UUID((0x2A4AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Hip Circumference</b> <i>(org.bluetooth.characteristic.hip_circumference)</i>
     * <p>
     * Used with the Waist Circumference value to calculate the Waist to Hip Ratio (WHR)
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.hip_circumference.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HIP_CIRCUMFERENCE = new UUID((0x2A8FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>HTTP Control Point</b> <i>(org.bluetooth.characteristic.http_control_point)</i>
     * <p>
     * Initiates a request to send an HTTP request message from the device containing the HTTP Proxy Service, acting as an HTTP Client, and an HTTP Server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.http_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HTTP_CONTROL_POINT = new UUID((0x2ABAL << 32) | 0x1000, leastSigBits);

    /**
     * <b>HTTP Entity Body</b> <i>(org.bluetooth.characteristic.http_entity_body)</i>
     * <p>
     * Contains the contents of the message body after any Transfer Encoding has been applied.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.http_entity_body.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HTTP_ENTITIY_BODY = new UUID((0x2AB9L << 32) | 0x1000, leastSigBits);

    /**
     * <b>HTTP Headers</b> <i>(org.bluetooth.characteristic.http_headers)</i>
     * <p>
     * Holds the headers that would be sent to the HTTP Request or the headers contained within an HTTP response message from the HTTP Server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.http_headers.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HTTP_HEADERS = new UUID((0x2AB7L << 32) | 0x1000, leastSigBits);

    /**
     * <b>HTTP Status Code</b> <i>(org.bluetooth.characteristic.http_status_code)</i>
     * <p>
     * Contains the Status-Code from the Status-Line of the first line of the HTTP Response Message, followed by one octet indicating the Data Status Bit Field indicating the status of the data received.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.http_status_code.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HTTP_STATUS_CODE = new UUID((0x2AB8L << 32) | 0x1000, leastSigBits);

    /**
     * <b>HTTPS Security</b> <i>(org.bluetooth.characteristic.https_security)</i>
     * <p>
     * Contains the known authenticity of the HTTPS Server certificate for the URI.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.https_security.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HTTPS_SECURITY = new UUID((0x2ABBL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Humidity</b> <i>(org.bluetooth.characteristic.humidity)</i>
     * <p>
     * Contains the relative humidity.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.humidity.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID HUMIDITY = new UUID((0x2A6FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>IEEE 11073-20601 Regulatory Certification Data List</b> <i>(org.bluetooth.characteristic.ieee_11073-20601_regulatory_certification_data_list)</i>
     * <p>
     * Opaque structure listing various regulatory and/or certification compliance items to which the device claims adherence.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.ieee_11073-20601_regulatory_certification_data_list.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID IEEE11073_20601_REGULATORY_CERTIFICATION_DATA_LIST = new UUID((0x2A2AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Indoor Bike Data</b> <i>(org.bluetooth.characteristic.indoor_bike_data)</i>
     * <p>
     * Sends training-related data to the Client from an indoor bike (Server).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.indoor_bike_data.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID INDOOR_BIKE_DATA = new UUID((0x2AD2L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Indoor Positioning Configuration</b> <i>(org.bluetooth.characteristic.indoor_positioning_configuration)</i>
     * <p>
     * Describes the set of characteristic values included in the Indoor Positioning Service AD type.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.indoor_positioning_configuration.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID INDOOR_POSITIONING_CONFIGURATION = new UUID((0x2AADL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Intermediate Cuff Pressure</b> <i>(org.bluetooth.characteristic.intermediate_cuff_pressure)</i>
     * <p>
     * Contains the intermediate cuff pressure.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.intermediate_cuff_pressure.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID INTERMEDIATE_CUFF_PRESSURE = new UUID((0x2A36L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Intermediate Temperature</b> <i>(org.bluetooth.characteristic.intermediate_temperature)</i>
     * <p>
     * Contains the intermediate temperature.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.intermediate_temperature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID INTERMEDIATE_TEMPERATURE = new UUID((0x2A1EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Irradiance</b> <i>(org.bluetooth.characteristic.irradiance)</i>
     * <p>
     * Contains the irradiance.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.irradiance.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID IRRADIANCE = new UUID((0x2A77L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Language</b> <i>(org.bluetooth.characteristic.language)</i>
     * <p>
     * Contains the language.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.language.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LANGUAGE = new UUID((0x2AA2L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Last Name</b> <i>(org.bluetooth.characteristic.last_name)</i>
     * <p>
     * Last name of the user.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.last_name.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LAST_NAME = new UUID((0x2A90L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Latitude</b> <i>(org.bluetooth.characteristic.latitude)</i>
     * <p>
     * WGS84 North coordinate (Latitude) of the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.latitude.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LATITUDE = new UUID((0x2AAEL << 32) | 0x1000, leastSigBits);

    /**
     * <b>LN Control Point</b> <i>(org.bluetooth.characteristic.ln_control_point)</i>
     * <p>
     * Requests a specific function to be executed on the receiving device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.ln_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LN_CONTROL_POINT = new UUID((0x2A6BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>LN Feature</b> <i>(org.bluetooth.characteristic.ln_feature)</i>
     * <p>
     * Reports a list of features supported by the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.ln_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LN_FEATURE = new UUID((0x2A6AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Local East Coordinate</b> <i>(org.bluetooth.characteristic.local_east_coordinate)</i>
     * <p>
     * Describes the East coordinate of the device using local coordinate system.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.local_east_coordinate.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LOCAL_EAST_COORDINATE = new UUID((0x2AB1L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Local North Coordinate</b> <i>(org.bluetooth.characteristic.local_north_coordinate)</i>
     * <p>
     * Describes the North coordinate of the device using local coordinate system.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.local_north_coordinate.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LOCAL_NORTH_COORDINATE = new UUID((0x2AB0L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Local Time Information</b> <i>(org.bluetooth.characteristic.local_time_information)</i>
     * <p>
     * Describes the local time information.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.local_time_information.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LOCAL_TIME_INFORMATION = new UUID((0x2A0FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Location and Speed Characteristic</b> <i>(org.bluetooth.characteristic.location_and_speed)</i>
     * <p>
     * Contains location and speed information.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.location_and_speed.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LOCATION_AND_SPEED = new UUID((0x2A67L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Location Name</b> <i>(org.bluetooth.characteristic.location_name)</i>
     * <p>
     * Describes the name of the location the device is installed in.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.location_name.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LOCATION_NAME = new UUID((0x2AB5L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Longitude</b> <i>(org.bluetooth.characteristic.longitude)</i>
     * <p>
     * WGS84 East coordinate (Longitude) of the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.longitude.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID LONGITUDE = new UUID((0x2AAFL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Magnetic Declination</b> <i>(org.bluetooth.characteristic.magnetic_declination)</i>
     * <p>
     * Describes the angle on the horizontal plane between the direction of True North (geographic) and the direction of Magnetic North, measured clockwise from True North to Magnetic North.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.magnetic_declination.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID MAGNETIC_DECLINATION = new UUID((0x2A2CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Magnetic Flux Density – 2D</b> <i>(org.bluetooth.characteristic.magnetic_flux_density_2d)</i>
     * <p>
     * Describes the magnetic flux density for two orthogonal axes: X and Y.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.magnetic_flux_density_2d.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID MAGNETIC_FLUX_DENSITY_2D = new UUID((0x2AA0L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Magnetic Flux Density – 3D</b> <i>(org.bluetooth.characteristic.magnetic_flux_density_3d)</i>
     * <p>
     * Describes the magnetic flux density for three orthogonal axes: X, Y and Z.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.magnetic_flux_density_3d.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID MAGNETIC_FLUX_DENSITY_3D = new UUID((0x2AA1L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Manufacturer Name String</b> <i>(org.bluetooth.characteristic.manufacturer_name_string)</i>
     * <p>
     * Represents the name of the manufacturer of the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.manufacturer_name_string.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID MANUFACTURER_NAME_STRING = new UUID((0x2A29L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Maximum Recommended Heart Rate</b> <i>(org.bluetooth.characteristic.maximum_recommended_heart_rate)</i>
     * <p>
     * Maximum recommended heart rate is a threshold that may be set to limit exertion. The maximum recommended heart rate is smaller or equal to the maximal heart rate a user can reach.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.maximum_recommended_heart_rate.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID MAXIMUM_RECOMMENDED_HEART_RATE = new UUID((0x2A91L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Measurement Interval</b> <i>(org.bluetooth.characteristic.measurement_interval)</i>
     * <p>
     * Defines the time between measurements.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.measurement_interval.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID MEASUREMENT_INTERVAL = new UUID((0x2A21L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Model Number String</b> <i>(org.bluetooth.characteristic.model_number_string)</i>
     * <p>
     * Represents the model number assigned by the device vendor.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.model_number_string.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID MODEL_NUMBER_STRING = new UUID((0x2A24L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Navigation</b> <i>(org.bluetooth.characteristic.navigation)</i>
     * <p>
     * Represents information used for navigation.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.navigation.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID NAVIGATION = new UUID((0x2A68L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Network Availability</b> <i>(org.bluetooth.characteristic.network_availability)</i>
     * <p>
     * Represents if the network is available or not available.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.network_availability.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID NETWORK_AVAILABILITY = new UUID((0x2A3EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>New Alert</b> <i>(org.bluetooth.characteristic.new_alert)</i>
     * <p>
     * Defines the category of the alert and how many new alerts of that category have occurred in the server device. Brief text information may also be included for the last alert in the category.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.new_alert.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID NEW_ALERT = new UUID((0x2A46L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object Action Control Point</b> <i>(org.bluetooth.characteristic.object_action_control_point)</i>
     * <p>
     * Defines the Object Action Control Point (OACP) characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_action_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_ACTION_CONTROL_POINT = new UUID((0x2AC5L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object Changed</b> <i>(org.bluetooth.characteristic.object_changed)</i>
     * <p>
     * Defines the Object Changed characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_changed.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_CHANGED = new UUID((0x2AC8L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object First-Created</b> <i>(org.bluetooth.characteristic.object_first_created)</i>
     * <p>
     * Defines the Object First-Created characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_first_created.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_FIRST_CREATED = new UUID((0x2AC1L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object ID</b> <i>(org.bluetooth.characteristic.object_id)</i>
     * <p>
     * Defines the Object ID characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_id.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_ID = new UUID((0x2AC3L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object Last-Modified</b> <i>(org.bluetooth.characteristic.object_last_modified)</i>
     * <p>
     * Defines the Object Last-Modified characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_last_modified.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_LAST_MODIFIED = new UUID((0x2AC2L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object List Control Point</b> <i>(org.bluetooth.characteristic.object_list_control_point)</i>
     * <p>
     * Defines the Object List Control Point (OLCP) characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_list_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_LIST_CONTROL_POINT = new UUID((0x2AC6L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object List Filter</b> <i>(org.bluetooth.characteristic.object_list_filter)</i>
     * <p>
     * Defines the Object List Filter characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_list_filter.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_LIST_FILTER = new UUID((0x2AC7L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object Name</b> <i>(org.bluetooth.characteristic.object_name)</i>
     * <p>
     * Defines the Object Name characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_name.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_NAME = new UUID((0x2ABEL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object Properties</b> <i>(org.bluetooth.characteristic.object_properties)</i>
     * <p>
     * Defines the Object Properties characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_properties.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_PROPERTIES = new UUID((0x2AC4L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object Size</b> <i>(org.bluetooth.characteristic.object_size)</i>
     * <p>
     * Defines the Object Size characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_size.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_SIZE = new UUID((0x2AC0L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Object Type</b> <i>(org.bluetooth.characteristic.object_type)</i>
     * <p>
     * Defines the Object Type characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.object_type.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OBJECT_TYPE = new UUID((0x2ABFL << 32) | 0x1000, leastSigBits);

    /**
     * <b>OTS Feature</b> <i>(org.bluetooth.characteristic.ots_feature)</i>
     * <p>
     * Defines the Object Transfer Service (OTS) characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.ots_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID OTS_FEATURE = new UUID((0x2ABDL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Peripheral Preferred Connection Parameters</b> <i>(org.bluetooth.characteristic.gap.peripheral_preferred_connection_parameters)</i>
     * <p>
     * Defines the Peripheral Preferred Connection Parameters characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.peripheral_preferred_connection_parameters.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = new UUID((0x2A04L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Peripheral Privacy Flag</b> <i>(org.bluetooth.characteristic.gap.peripheral_privacy_flag)</i>
     * <p>
     * Defines the Peripheral Privacy Flag characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.peripheral_privacy_flag.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID PERIPHERAL_PRIVACY_FLAG = new UUID((0x2A02L << 32) | 0x1000, leastSigBits);

    /**
     * <b>PLX Continuous Measurement Characteristic</b> <i>(org.bluetooth.characteristic.plx_continuous_measurement)</i>
     * <p>
     * Sends periodic pulse oximetry measurements.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.plx_continuous_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID PLX_CONTINUOUS_MEASUREMENT = new UUID((0x2A5FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>PLX Features</b> <i>(org.bluetooth.characteristic.plx_features)</i>
     * <p>
     * Describes the supported features of the PLX Server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.plx_features.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID PLX_FEATURES = new UUID((0x2A60L << 32) | 0x1000, leastSigBits);

    /**
     * <b>PLX Spot-Check Measurement</b> <i>(org.bluetooth.characteristic.plx_spot_check_measurement)</i>
     * <p>
     * Sends Spot-check measurements of SpO2 (Percent oxygen saturation of hemoglobin) and PR (pulse rate).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.plx_spot_check_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID PLX_SPOT_CHECK_MEASUREMENT = new UUID((0x2A5EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>PnP ID</b> <i>(org.bluetooth.characteristic.pnp_id)</i>
     * <p>
     * Set of values that are used to create a device ID value that is unique for this device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.pnp_id.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID PNP_ID = new UUID((0x2A50L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Pollen Concentration</b> <i>(org.bluetooth.characteristic.pollen_concentration)</i>
     * <p>
     * Describes the Pollen Concentration.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.pollen_concentration.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID POLLEN_CONCENTRATION = new UUID((0x2A75L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Position 2D</b> <i>(org.bluetooth.characteristic.position_2d)</i>
     * <p>
     * Describes the 2D Position.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.position_2d.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID POSITION_2D = new UUID((0x2A2FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Position 3D</b> <i>(org.bluetooth.characteristic.position_3d)</i>
     * <p>
     * Describes the 3D Position.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.position_3d.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID POSITION_3D = new UUID((0x2A30L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Position Quality</b> <i>(org.bluetooth.characteristic.position_quality)</i>
     * <p>
     * Describes the Position Quality.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.position_quality.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID POSITION_QUALITY = new UUID((0x2A69L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Pressure</b> <i>(org.bluetooth.characteristic.pressure)</i>
     * <p>
     * Describes the Pressure.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.pressure.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID PRESSURE = new UUID((0x2A6DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Protocol Mode</b> <i>(org.bluetooth.characteristic.protocol_mode)</i>
     * <p>
     * Exposes the current protocol mode of the HID Service with which it is associated, or to set the desired protocol mode of the HID Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.protocol_mode.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID PROTOCOL_MODE = new UUID((0x2A4EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Pulse Oximetry Control Point</b> <i>(org.bluetooth.characteristic.pulse_oximetry_control_point)</i>
     * <p>
     * Used to set or get the sensor’s configuration.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.pulse_oximetry_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID PULSE_OXIMETRY_CONTROL_POINT = new UUID((0x2A62L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Rainfall</b> <i>(org.bluetooth.characteristic.rainfall)</i>
     * <p>
     * Describes the rainfall.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.rainfall.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RAINFALL = new UUID((0x2A78L << 32) | 0x1000, leastSigBits);

    /**
     * <b>RC Feature</b> <i>(org.bluetooth.characteristic.rc_feature)</i>
     * <p>
     * Used to describe the supported features of the Reconnection Configuration server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.rc_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RC_FEATURE = new UUID((0x2B1DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>RC Settings</b> <i>(org.bluetooth.characteristic.rc_settings)</i>
     * <p>
     * Used to both read and notify supported features on the Reconnection Configuration server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.rc_settings.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RC_SETTINGS = new UUID((0x2B1EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Reconnection Address</b> <i>(org.bluetooth.characteristic.gap.reconnection_address)</i>
     * <p>
     * Describes the GAP Reconnection Address.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.reconnection_address.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RECONNECTION_ADDRESS = new UUID((0x2A03L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Reconnection Configuration Control Point</b> <i>(org.bluetooth.characteristic.reconnection_configuration_control_point)</i>
     * <p>
     * Describes the GAP Reconnection Address.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.reconnection_configuration_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RECONNECTION_CONFIGURATION_CONTROL_POINT = new UUID((0x2B1FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Reconnection Address</b> <i>(org.bluetooth.characteristic.gap.reconnection_address)</i>
     * <p>
     * Executes a supported procedure on the Reconnection Configuration server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.reconnection_address.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RECORD_ACCESS_CONTROL_POINT = new UUID((0x2A52L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Reference Time Information</b> <i>(org.bluetooth.characteristic.reference_time_information)</i>
     * <p>
     * Describes the Reference Time Information characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.reference_time_information.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID REFERENCE_TIME_INFORMATION = new UUID((0x2A14L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Removable</b> <i>(org.bluetooth.characteristic.removable)</i>
     * <p>
     * Describes the Removable characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.removable.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID REMOVABLE = new UUID((0x2A3AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Report</b> <i>(org.bluetooth.characteristic.report)</i>
     * <p>
     * Used to exchange data between a HID Device and a HID Host.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.report.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID REPORT = new UUID((0x2A4DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Report Map</b> <i>(org.bluetooth.characteristic.report_map)</i>
     * <p>
     * Used to define formatting information for Input Report, Output Report, and Feature Report data transferred between a HID Device and HID Host, information on how this data can be used, and other information regarding physical aspects of the device (i.e. that the device functions as a keyboard, for example, or has multiple functions such as a keyboard and volume controls).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.report_map.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID REPORT_MAP = new UUID((0x2A4BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Resolvable Private Address Only</b> <i>(org.bluetooth.characteristic.resolvable_private_address_only)</i>
     * <p>
     * Defines whether the device will only use Resolvable Private Addresses (RPAs) as local addresses.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.resolvable_private_address_only.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RESOLVABLE_PRIVATE_ADDRESS_ONLY = new UUID((0x2AC9L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Resting Heart Rate</b> <i>(org.bluetooth.characteristic.resting_heart_rate)</i>
     * <p>
     * Lowest Heart Rate a user can reach.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.resting_heart_rate.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RESTING_HEART_RATE = new UUID((0x2A92L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Ringer Control Point</b> <i>(org.bluetooth.characteristic.ringer_control_point)</i>
     * <p>
     * Defines the Control Point of Ringer.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.ringer_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RINGER_CONTROL_POINT = new UUID((0x2A40L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Ringer Setting</b> <i>(org.bluetooth.characteristic.ringer_setting)</i>
     * <p>
     * Defines the Setting of the Ringer.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.ringer_setting.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RINGER_SETTING = new UUID((0x2A41L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Rower Data</b> <i>(org.bluetooth.characteristic.rower_data)</i>
     * <p>
     * Sends training-related data to the Client from a rower (Server).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.rower_data.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ROWER_DATA = new UUID((0x2AD1L << 32) | 0x1000, leastSigBits);

    /**
     * <b>RSC Feature</b> <i>(org.bluetooth.characteristic.rsc_feature)</i>
     * <p>
     * Describes the supported features of the Running Speed and Cadence (RSC) Server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.rsc_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RSC_FEATURE = new UUID((0x2A54L << 32) | 0x1000, leastSigBits);

    /**
     * <b>RSC Measurement</b> <i>(org.bluetooth.characteristic.rsc_measurement)</i>
     * <p>
     * Describes the Running Speed and Cadence (RSC) Measurement characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.rsc_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID RSC_MEASUREMENT = new UUID((0x2A53L << 32) | 0x1000, leastSigBits);

    /**
     * <b>SC Control Point</b> <i>(org.bluetooth.characteristic.sc_control_point)</i>
     * <p>
     * Request a specific function to be executed on the receiving device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.sc_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SC_CONTROL_POINT = new UUID((0x2A55L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Scan Interval Window</b> <i>(org.bluetooth.characteristic.scan_interval_window)</i>
     * <p>
     * Stores the scan parameters of the GATT Client. Included in this characteristic are the Scan Interval and Scan Window of the GATT Client device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.scan_interval_window.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SCAN_INTERVAL_WINDOW = new UUID((0x2A4FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Scan Refresh</b> <i>(org.bluetooth.characteristic.scan_refresh)</i>
     * <p>
     * Notifies the Client that the Server requires the Scan Interval Window characteristic to be written with the latest values upon notification.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.scan_refresh.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SCAN_REFRESH = new UUID((0x2A31L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Scientific Temperature Celsius</b> <i>(org.bluetooth.characteristic.scientific_temperature_celsius)</i>
     * <p>
     * Describes the Scientific Temperature in Celsius.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.scientific_temperature_celsius.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SCIENTIFIC_TEMPERATURE_CELSIUS = new UUID((0x2A3CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Secondary Time Zone</b> <i>(org.bluetooth.characteristic.secondary_time_zone)</i>
     * <p>
     * Describes the Secondary Time Zone.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.secondary_time_zone.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SECONDARY_TIME_ZONE = new UUID((0x2A10L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Sensor Location</b> <i>(org.bluetooth.characteristic.sensor_location)</i>
     * <p>
     * Exposes the location of the sensor.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.sensor_location.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SENSOR_LOCATION = new UUID((0x2A5DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Serial Number String</b> <i>(org.bluetooth.characteristic.serial_number_string)</i>
     * <p>
     * Represents the serial number for a particular instance of the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.serial_number_string.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SERIAL_NUMBER_STRING = new UUID((0x2A25L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Service Changed</b> <i>(org.bluetooth.characteristic.gatt.service_changed)</i>
     * <p>
     * Describes whether the Service has changed.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gatt.service_changed.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SERVICE_CHANGED = new UUID((0x2A05L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Service Required</b> <i>(org.bluetooth.characteristic.service_required)</i>
     * <p>
     * Describes whether the Service is required.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.service_required.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SERVICE_REQUIRED = new UUID((0x2A3BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Software Revision String</b> <i>(org.bluetooth.characteristic.software_revision_string)</i>
     * <p>
     * Represents the software revision for the software within the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.software_revision_string.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SOFTWARE_REVISION_STRING = new UUID((0x2A28L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Sport Type for Aerobic and Anaerobic Thresholds</b> <i>(org.bluetooth.characteristic.sport_type_for_aerobic_and_anaerobic_thresholds)</i>
     * <p>
     * Contains the Sport Type for Aerobic and Anaerobic Threshold characteristics.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.sport_type_for_aerobic_and_anaerobic_thresholds.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS = new UUID((0x2A93L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Stair Climber Data</b> <i>(org.bluetooth.characteristic.stair_climber_data)</i>
     * <p>
     * Sends training-related data to the Client from a stair climber (Server).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.stair_climber_data.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID STAIR_CLIMBER_DATA = new UUID((0x2AD0L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Step Climber Data</b> <i>(org.bluetooth.characteristic.step_climber_data)</i>
     * <p>
     * Sends training-related data to the Client from a step climber (Server).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.step_climber_data.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID STEP_CLIMBER_DATA = new UUID((0x2ACFL << 32) | 0x1000, leastSigBits);

    /**
     * <b>String</b> <i>(org.bluetooth.characteristic.string)</i>
     * <p>
     * Describes a generic UTF8 string which may be used in Services requiring strings.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.string.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID STRING = new UUID((0x2A3DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Supported Heart Rate Range</b> <i>(org.bluetooth.characteristic.supported_heart_rate_range)</i>
     * <p>
     * Describes the supported heart rate range as well as the minimum heart rate increment supported by the Server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.supported_heart_rate_range.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SUPPORTED_HEART_RATE_RANGE = new UUID((0x2AD7L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Supported Inclination Range</b> <i>(org.bluetooth.characteristic.supported_inclination_range)</i>
     * <p>
     * Describes the supported inclination range as well as the minimum inclination increment supported by the Server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.supported_inclination_range.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SUPPORTED_INCLINATION_RANGE = new UUID((0x2AD5L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Supported New Alert Category</b> <i>(org.bluetooth.characteristic.supported_new_alert_category)</i>
     * <p>
     * Category that the server supports for new alert.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.supported_new_alert_category.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SUPPORTED_NEW_ALERT_CATEGORY = new UUID((0x2A47L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Supported Power Range</b> <i>(org.bluetooth.characteristic.supported_power_range)</i>
     * <p>
     * Sends the supported power range as well as the minimum power increment supported by the Server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.supported_power_range.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SUPPORTED_POWER_RANGE = new UUID((0x2AD8L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Supported Resistance Level Range</b> <i>(org.bluetooth.characteristic.supported_resistance_level_range)</i>
     * <p>
     * Sends the supported resistance level range as well as the minimum resistance increment supported by the Server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.supported_resistance_level_range.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SUPPORTED_RESISTANCE_LEVEL_RANGE = new UUID((0x2AD6L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Supported Speed Range</b> <i>(org.bluetooth.characteristic.supported_speed_range)</i>
     * <p>
     * Sends the speed range as well as the minimum speed increment supported by the Server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.supported_speed_range.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SUPPORTED_SPEED_RANGE = new UUID((0x2AD4L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Supported Unread Alert Category</b> <i>(org.bluetooth.characteristic.supported_unread_alert_category)</i>
     * <p>
     * Category that the server supports for unread alert.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.supported_unread_alert_category.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SUPPORTED_UNREAD_ALERT_CATEGORY = new UUID((0x2A48L << 32) | 0x1000, leastSigBits);

    /**
     * <b>System ID</b> <i>(org.bluetooth.characteristic.system_id)</i>
     * <p>
     * Represents the system ID.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.system_id.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SYSTEM_ID = new UUID((0x2A23L << 32) | 0x1000, leastSigBits);

    /**
     * <b>TDS Control Point</b> <i>(org.bluetooth.characteristic.tds_control_point)</i>
     * <p>
     * Describes a Transport Discovery Service (TDS) control point.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.tds_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TDS_CONTROL_POINT = new UUID((0x2ABCL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Temperature</b> <i>(org.bluetooth.characteristic.temperature)</i>
     * <p>
     * Describes a temperature value in degrees Celsius.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.temperature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TEMPERATURE = new UUID((0x2A6EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Temperature Celsius</b> <i>(org.bluetooth.characteristic.temperature_celsius)</i>
     * <p>
     * Describes a temperature value in degrees Celsius.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.temperature_celsius.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TEMPERATURE_CELSIUS = new UUID((0x2A1FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Temperature Fahrenheit</b> <i>(org.bluetooth.characteristic.temperature_fahrenheit)</i>
     * <p>
     * Describes a temperature value in Fahrenheit.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.temperature_fahrenheit.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TEMPERATURE_FAHRENHEIT = new UUID((0x2A20L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Temperature Measurement</b> <i>(org.bluetooth.characteristic.temperature_measurement)</i>
     * <p>
     * Contains values from the temperature measurement service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.temperature_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TEMPERATURE_MEASUREMENT = new UUID((0x2A1CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Temperature Type</b> <i>(org.bluetooth.characteristic.temperature_measurement)</i>
     * <p>
     * Indicates where the temperature was measured.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.temperature_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TEMPERATURE_TYPE = new UUID((0x2A1DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Three Zone Heart Rate Limits</b> <i>(org.bluetooth.characteristic.three_zone_heart_rate_limits)</i>
     * <p>
     * Contains the limits between the heart rate zones for the 3-zone heart rate definition (Hard, Moderate and Light).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.three_zone_heart_rate_limits.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID THREE_ZONE_HEART_RATE_LIMITS = new UUID((0x2A94L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Time Accuracy</b> <i>(org.bluetooth.characteristic.time_accuracy)</i>
     * <p>
     * Describes the accuracy of time information.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.time_accuracy.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TIME_ACCURACY = new UUID((0x2A12L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Time Broadcast</b> <i>(org.bluetooth.characteristic.time_broadcast)</i>
     * <p>
     * Describes the time broadcast information.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.time_broadcast.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TIME_BROADCAST = new UUID((0x2A15L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Time Source</b> <i>(org.bluetooth.characteristic.time_source)</i>
     * <p>
     * Describes the time broadcast information.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.time_broadcast.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TIME_SOURCE = new UUID((0x2A13L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Time Update Control Point</b> <i>(org.bluetooth.characteristic.time_update_control_point)</i>
     * <p>
     * Describes the time update control point.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.time_update_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TIME_UPDATE_CONTROL_POINT = new UUID((0x2A16L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Time Update State</b> <i>(org.bluetooth.characteristic.time_update_state)</i>
     * <p>
     * Describes the time update state.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.time_update_state.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TIME_UPDATE_STATE = new UUID((0x2A17L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Time with DST</b> <i>(org.bluetooth.characteristic.time_with_dst)</i>
     * <p>
     * Describes the time with daylight saving time (DST).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.time_with_dst.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TIME_WITH_DST = new UUID((0x2A11L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Time Zone</b> <i>(org.bluetooth.characteristic.time_zone)</i>
     * <p>
     * Describes the time zone.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.time_zone.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TIME_ZONE = new UUID((0x2A0EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Training Status</b> <i>(org.bluetooth.characteristic.training_status)</i>
     * <p>
     * Describes the training status.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.training_status.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TRAINING_STATUS = new UUID((0x2AD3L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Treadmill Data</b> <i>(org.bluetooth.characteristic.treadmill_data)</i>
     * <p>
     * Sends training-related data to the Client from a treadmill (Server).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.treadmill_data.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TREADMILL_DATA = new UUID((0x2ACDL << 32) | 0x1000, leastSigBits);

    /**
     * <b>True Wind Direction</b> <i>(org.bluetooth.characteristic.true_wind_direction)</i>
     * <p>
     * Describes the true wind direction.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.true_wind_direction.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TRUE_WIND_DIRECTION = new UUID((0x2A71L << 32) | 0x1000, leastSigBits);

    /**
     * <b>True Wind Speed</b> <i>(org.bluetooth.characteristic.true_wind_speed)</i>
     * <p>
     * Describes the true wind speed.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.true_wind_speed.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TRUE_WIND_SPEED = new UUID((0x2A70L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Two Zone Heart Rate Limit</b> <i>(org.bluetooth.characteristic.two_zone_heart_rate_limit)</i>
     * <p>
     * Describes the heart rate limit between the heart rate zones for the 2-zone heart rate definition (Fitness and Fat Burn).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.two_zone_heart_rate_limit.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TWO_ZONE_HEART_RATE_LIMIT = new UUID((0x2A95L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Tx Power Level</b> <i>(org.bluetooth.characteristic.tx_power_level)</i>
     * <p>
     * Represents the current transmit power level in dBm, and the level ranges from -100 dBm to +20 dBm to a resolution of 1 dBm.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.tx_power_level.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TX_POWER_LEVEL = new UUID((0x2A07L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Uncertainty</b> <i>(org.bluetooth.characteristic.uncertainty)</i>
     * <p>
     * Describes the uncertainty of the location information the device exposes.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.uncertainty.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID UNCERTAINTY = new UUID((0x2AB4L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Unread Alert Status</b> <i>(org.bluetooth.characteristic.unread_alert_status)</i>
     * <p>
     * Describes how many numbers of unread alerts exist in the specific category in the device.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.unread_alert_status.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID UNREAD_ALERT_STATUS = new UUID((0x2A45L << 32) | 0x1000, leastSigBits);

    /**
     * <b>URI</b> <i>(org.bluetooth.characteristic.uri)</i>
     * <p>
     * Uniform Resource Identifier (URI) Characteristic is used to configure the URI for a subsequent request.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.uri.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID URI = new UUID((0x2AB6L << 32) | 0x1000, leastSigBits);

    /**
     * <b>User Control Point</b> <i>(org.bluetooth.characteristic.user_control_point)</i>
     * <p>
     * Defines a control point of the User Data Service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.user_control_point.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID USER_CONTROL_POINT = new UUID((0x2A9FL << 32) | 0x1000, leastSigBits);

    /**
     * <b>User Index</b> <i>(org.bluetooth.characteristic.user_index)</i>
     * <p>
     * Defines the user index.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.user_index.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID USER_INDEX = new UUID((0x2A9AL << 32) | 0x1000, leastSigBits);

    /**
     * <b>UV Index</b> <i>(org.bluetooth.characteristic.uv_index)</i>
     * <p>
     * Describes the UV index.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.uv_index.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID UV_INDEX = new UUID((0x2A76L << 32) | 0x1000, leastSigBits);

    /**
     * <b>VO2 Max</b> <i>(org.bluetooth.characteristic.vo2_max)</i>
     * <p>
     * Describes the Maximal Oxygen uptake of a user.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.vo2_max.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID VO2_MAX = new UUID((0x2A96L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Waist Circumference</b> <i>(org.bluetooth.characteristic.waist_circumference)</i>
     * <p>
     * Used with the Hip Circumference value to calculate the Waist to Hip Ratio (WHR).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.waist_circumference.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID WAIST_CIRCUMFERENCE = new UUID((0x2A97L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Weight</b> <i>(org.bluetooth.characteristic.weight)</i>
     * <p>
     * Weight of the User.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.weight.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID WEIGHT = new UUID((0x2A98L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Weight Measurement</b> <i>(org.bluetooth.characteristic.weight_measurement)</i>
     * <p>
     * Describes the weight measurement service.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.weight_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID WEIGHT_MEASUREMENT = new UUID((0x2A9DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Weight Scale Feature</b> <i>(org.bluetooth.characteristic.weight_scale_feature)</i>
     * <p>
     * Describes the features of the weight scale.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.weight_scale_feature.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID WEIGHT_SCALE_FEATURE = new UUID((0x2A9EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Wind Chill</b> <i>(org.bluetooth.characteristic.wind_chill)</i>
     * <p>
     * Describes the wind chill temperature.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.wind_chill.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID WIND_CHILL = new UUID((0x2A79L << 32) | 0x1000, leastSigBits);


    //////////////////////////////////////////////////////
    ////////////////  BLE GATT Descriptors ///////////////
    //////////////////////////////////////////////////////

    /**
     * <b>Characteristic Aggregate Format</b> <i>(org.bluetooth.descriptor.gatt.characteristic_aggregate_format)</i>
     * <p>
     * Defines the format of an aggregated Characteristic Value.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.characteristic_aggregate_format.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CHARACTERISTIC_AGGREGATE_FORMAT = new UUID((0x2905L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Characteristic Extended Properties</b> <i>(org.bluetooth.descriptor.gatt.characteristic_extended_properties)</i>
     * <p>
     * Defines additional Characteristic Properties.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.characteristic_extended_properties.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CHARACTERISTIC_EXTENDED_PROPERTIES = new UUID((0x2900L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Characteristic Presentation Format</b> <i>(org.bluetooth.descriptor.gatt.characteristic_presentation_format)</i>
     * <p>
     * Defines the format of the Characteristic Value.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.characteristic_presentation_format.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CHARACTERISTIC_PRESENTATION_FORMAT = new UUID((0x2904L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Characteristic User Description</b> <i>(org.bluetooth.descriptor.gatt.characteristic_user_description)</i>
     * <p>
     * Provides a textual user description for a characteristic value.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.characteristic_user_description.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CHARACTERISTIC_USER_DESCRIPTION = new UUID((0x2901L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Client Characteristic Configuration</b> <i>(org.bluetooth.descriptor.gatt.client_characteristic_configuration)</i>
     * <p>
     * Defines how the characteristic may be configured by a specific client.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION = new UUID((0x2902L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Environmental Sensing Configuration</b> <i>(org.bluetooth.descriptor.es_configuration)</i>
     * <p>
     * Defines the Environmental Sensing Configuration.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.es_configuration.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ENVIRONMENTAL_SENSING_CONFIGURATION = new UUID((0x290BL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Environmental Sensing Measurement</b> <i>(org.bluetooth.descriptor.es_measurement)</i>
     * <p>
     * Defines the Environmental Sensing Measurement.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.es_measurement.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ENVIRONMENTAL_SENSING_MEASUREMENT = new UUID((0x290CL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Environmental Sensing Trigger Setting</b> <i>(org.bluetooth.descriptor.es_trigger_setting)</i>
     * <p>
     * Defines the Environmental Sensing Measurement.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.es_trigger_setting.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID ENVIRONMENTAL_SENSING_TRIGGER_SETTING = new UUID((0x290DL << 32) | 0x1000, leastSigBits);

    /**
     * <b>External Report Reference</b> <i>(org.bluetooth.descriptor.external_report_reference)</i>
     * <p>
     * Allows a HID Host to map information from the Report Map characteristic value for Input Report, Output Report or Feature Report data to the Characteristic UUID of external service characteristics used to transfer the associated data.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.external_report_reference.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID EXTERNAL_REPORT_REFERENCE = new UUID((0x2907L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Number of Digitals</b> <i>(org.bluetooth.descriptor.number_of_digitals)</i>
     * <p>
     * Defines the number of digitals in a characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.number_of_digitals.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID NUMBER_OF_DIGITALS = new UUID((0x2909L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Report Reference</b> <i>(org.bluetooth.descriptor.report_reference)</i>
     * <p>
     * Maps information in the form of a Report ID and Report Type which maps the current parent characteristic to the Report ID(s) and Report Type(s).
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.report_reference.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID REPORT_REFERENCE = new UUID((0x2908L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Server Characteristic Configuration</b> <i>(org.bluetooth.descriptor.gatt.server_characteristic_configuration)</i>
     * <p>
     * Defines how the characteristic descriptor is associated with may be configured for the server.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.server_characteristic_configuration.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID SERVER_CHARACTERISTIC_CONFIGURATION = new UUID((0x2903L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Time Trigger Setting</b> <i>(org.bluetooth.descriptor.time_trigger_setting)</i>
     * <p>
     * Defines the settings of a Time Trigger.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.time_trigger_setting.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID TIME_TRIGGER_SETTING = new UUID((0x290EL << 32) | 0x1000, leastSigBits);

    /**
     * <b>Valid Range</b> <i>(org.bluetooth.descriptor.valid_range)</i>
     * <p>
     * Defining the range of a characteristic.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.valid_range.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID VALID_RANGE = new UUID((0x2906L << 32) | 0x1000, leastSigBits);

    /**
     * <b>Value Trigger Setting</b> <i>(org.bluetooth.descriptor.value_trigger_setting)</i>
     * <p>
     * Defines the settings of a Value Trigger.
     * <p>
     * See <a href=https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.value_trigger_setting.xml>Bluetooth SIG</a> specification for further information.
     */
    public static final UUID VALUE_TRIGGER_SETTING = new UUID((0x290AL << 32) | 0x1000, leastSigBits);

    protected static HashMap<UUID, String> services = new HashMap<>();
    protected static HashMap<UUID, String> characteristics = new HashMap<>();
    protected static HashMap<UUID, String> descriptors = new HashMap<>();

    static {
        services.put(GENERIC_ACCESS_SERVICE, "Generic Access Service");
        services.put(ALERT_NOTIFICATION_SERVICE, "Alert Notification Service");
        services.put(AUTOMATION_IO_SERVICE, "Automation IO Service");
        services.put(BATTERY_SERVICE, "Battery Service");
        services.put(BLOOD_PRESSURE_SERVICE, "Blood Pressure Service");
        services.put(BODY_COMPOSITION_SERVICE, "Body Composition Service");
        services.put(BOND_MANAGEMENT_SERVICE_SERVICE, "Bond Management Service");
        services.put(CONTINUOUS_GLUCOSE_MONITORING_SERVICE, "Continuous Glucose Monitoring Service");
        services.put(CURRENT_TIME_SERVICE_SERVICE, "Current Time Service");
        services.put(CYCLING_POWER_SERVICE, "Cycling Power Service");
        services.put(CYCLING_SPEED_AND_CADENCE_SERVICE, "Cycling Speed and Cadence Service");
        services.put(DEVICE_INFORMATION_SERVICE, "Device Information Service");
        services.put(ENVIRONMENTAL_SENSING_SERVICE, "Environmental Sensing Service");
        services.put(FITNESS_MACHINE_SERVICE, "Fitness Machine Service");
        services.put(GENERIC_ATTRIBUTE_SERVICE, "Generic Attribute Service");
        services.put(GLUCOSE_SERVICE, "Glucose Service");
        services.put(HEALTH_THERMOMETER_SERVICE, "Health Thermometer Service");
        services.put(HEART_RATE_SERVICE, "Heart Rate Service");
        services.put(HTTP_PROXY_SERVICE, "HTTP Proxy Service");
        services.put(HUMAN_INTERFACE_DEVICE_SERVICE, "Human Interface Device Service");
        services.put(IMMEDIATE_ALERT_SERVICE, "Immediate Alert Service");
        services.put(INDOOR_POSITIONING_SERVICE, "Indoor Positioning Service");
        services.put(INTERNET_PROTOCOL_SUPPORT_SERVICE, "Internet Protocol Support Service");
        services.put(LINK_LOSS_SERVICE, "Link Loss Service");
        services.put(LOCATION_AND_NAVIGATION_SERVICE, "Location and Navigation Service");
        services.put(MESH_PROVISIONING_SERVICE, "Mesh Provisioning Service");
        services.put(MESH_PROXY_SERVICE, "Mesh Proxy Service");
        services.put(NEXT_DST_CHANGE_SERVICE, "Next DST Change Service");
        services.put(OBJECT_TRANSFER_SERVICE, "Object Transfer Service");
        services.put(PHONE_ALERT_STATUS_SERVICE, "Phone Alert Status Service");
        services.put(PULSE_OXIMETER_SERVICE, "Pulse Oximeter Service");
        services.put(RECONNECTION_CONFIGURATION_SERVICE, "Reconnection Configuration Service");
        services.put(REFERENCE_TIME_UPDATE_SERVICE, "Reference Time Update Service");
        services.put(RUNNING_SPEED_AND_CADENCE_SERVICE, "Running Speed and Cadence Service");
        services.put(SCAN_PARAMETER_SERVICE, "Scan Parameter Service");
        services.put(TRANSPORT_DISCOVERY_SERVICE, "Transport Discovery Service");
        services.put(TX_POWER_SERVICE, "Tx Power Service");
        services.put(USER_DATA_SERVICE, "User Data Service");
        services.put(WEIGHT_SCALE_SERVICE, "Weight Scale Service");

        characteristics.put(AEROBIC_HEART_RATE_LOWER_LIMIT, "Aerobic Heart Rate Lower Limit");
        characteristics.put(AEROBIC_HEART_RATE_UPPER_LIMIT, "Aerobic Heart Rate Upper Limit");
        characteristics.put(AEROBIC_THRESHOLD, "Aerobic Threshold");
        characteristics.put(AGE, "Age");
        characteristics.put(AGGREGATE, "Aggregate");
        characteristics.put(ALERT_CATEGORY_ID, "Alert Category ID");
        characteristics.put(ALERT_CATEGORY_ID_BIT_MASK, "Alert Category ID Bit Mask");
        characteristics.put(ALERT_LEVEL, "Alert Level");
        characteristics.put(ALERT_NOTIFICATION_CONTROL_POINT, "Alert Notification Control Point");
        characteristics.put(ALERT_STATUS, "Alert Status");
        characteristics.put(ALTITUDE, "Altitude");
        characteristics.put(ANAEROBIC_HEART_RATE_LOWER_LIMIT, "Anaerobic Heart Rate Lower Limit");
        characteristics.put(ANAEROBIC_HEART_RATE_UPPER_LIMIT, "Anaerobic Heart Rate Upper Limit");
        characteristics.put(ANAEROBIC_THRESHOLD, "Anaerobic Threshold");
        characteristics.put(ANALOG, "Analog");
        characteristics.put(ANALOG_OUTPUT, "Analog Output");
        characteristics.put(APPARENT_WIND_DIRECTION, "Apparent Wind Direction");
        characteristics.put(APPARENT_WIND_SPEED, "Apparent Wind Speed");
        characteristics.put(APPEARANCE, "Appearance");
        characteristics.put(BAROMETRIC_PRESSURE_TREND, "Barometric Pressure Trend");
        characteristics.put(BATTERY_LEVEL, "Battery Level");
        characteristics.put(BATTERY_LEVEL_STATE, "Battery Level State");
        characteristics.put(BATTERY_POWER_STATE, "Battery Power State");
        characteristics.put(BLOOD_PRESSURE_FEATURE, "Blood Pressure Feature");
        characteristics.put(BLOOD_PRESSURE_MEASUREMENT, "Blood Pressure Measurement");
        characteristics.put(BODY_COMPOSITION_FEATURE, "Body Composition Feature");
        characteristics.put(BODY_COMPOSITION_MEASUREMENT, "Body Composition Measurement");
        characteristics.put(BODY_SENSOR_LOCATION, "Body Sensor Location");
        characteristics.put(BOND_MANAGEMENT_CONTROL_POINT, "Bond Management Control Point");
        characteristics.put(BOND_MANAGEMENT_FEATURES, "Bond Management Features");
        characteristics.put(BOOT_KEYBOARD_INPUT_REPORT, "Boot Keyboard Input Report");
        characteristics.put(BOOT_KEYBOARD_OUTPUT_REPORT, "Boot Keyboard Output Report");
        characteristics.put(BOOT_MOUSE_INPUT_REPORT, "Boot Mouse Input Report");
        characteristics.put(CENTRAL_ADDRESS_RESOLUTION, "Central Address Resolution");
        characteristics.put(CGM_FEATURE, "CGM Feature");
        characteristics.put(CGM_MEASUREMENT, "CGM Measurement");
        characteristics.put(CGM_SESSION_RUN_TIME, "CGM Session Run Time");
        characteristics.put(CGM_SESSION_START_TIME, "CGM Session Start Time");
        characteristics.put(CGM_SPECIFIC_OPS_CONTROL_POINT, "CGM Specific Ops Control Point");
        characteristics.put(CGM_STATUS, "CGM Status");
        characteristics.put(CROSS_TRAINER_DATA, "Cross Trainer Data");
        characteristics.put(CSC_FEATURE, "CSC Feature");
        characteristics.put(CSC_MEASUREMENT, "CSC Measurement");
        characteristics.put(CURRENT_TIME, "Current Time");
        characteristics.put(CYCLING_POWER_CONTROL_POINT, "Cycling Power Control Point");
        characteristics.put(CYCLING_POWER_FEATURE, "Cycling Power Feature");
        characteristics.put(CYCLING_POWER_MEASUREMENT, "Cycling Power Measurement");
        characteristics.put(CYCLING_POWER_VECTOR, "Cycling Power Vector");
        characteristics.put(DATABASE_CHANGE_INCREMENT, "Database Change Increment");
        characteristics.put(DATE_OF_BIRTH, "Date of Birth");
        characteristics.put(DATE_OF_THRESHOLD_ASSESSMENT, "Date of Threshold Assessment");
        characteristics.put(DATE_TIME, "Date Time");
        characteristics.put(DAY_DATE_TIME, "Day Date Time");
        characteristics.put(DAY_OF_WEEK, "Day of Week");
        characteristics.put(DESCRIPTOR_VALUE_CHANGED, "Descriptor Value Changed");
        characteristics.put(DEVICE_NAME, "Device Name");
        characteristics.put(DEW_POINT, "Dew Point");
        characteristics.put(DIGITAL, "Digital");
        characteristics.put(DIGITAL_OUTPUT, "Digital Output");
        characteristics.put(DST_OFFSET, "DST Offset");
        characteristics.put(ELEVATION, "Elevation");
        characteristics.put(EMAIL_ADDRESS, "Email Address");
        characteristics.put(EXACT_TIME_100, "Exact Time 100");
        characteristics.put(EXACT_TIME_256, "Exact Time 256");
        characteristics.put(FAT_BURN_HEART_RATE_LOWER_LIMIT, "Fat Burn Heart Rate Lower Limit");
        characteristics.put(FAT_BURN_HEART_RATE_UPPER_LIMIT, "Fat Burn Heart Rate Upper Limit");
        characteristics.put(FIRMWARE_REVISION_STRING, "Firmware Revision String");
        characteristics.put(FIRST_NAME, "First Name");
        characteristics.put(FITNESS_MACHINE_CONTROL_POINT, "Fitness Machine Control Point");
        characteristics.put(FITNESS_MACHINE_FEATURE, "Fitness Machine Feature");
        characteristics.put(FITNESS_MACHINE_STATUS, "Fitness Machine Status");
        characteristics.put(FIVE_ZONE_HEART_RATE_LIMITS, "Five Zone Heart Rate Limits");
        characteristics.put(FLOOR_NUMBER, "Floor Number");
        characteristics.put(GENDER, "Gender");
        characteristics.put(GLUCOSE_FEATURE, "Glucose Feature");
        characteristics.put(GLUCOSE_MEASUREMENT, "Glucose Measurement");
        characteristics.put(GLUCOSE_MEASUREMENT_CONTEXT, "Glucose Measurement Context");
        characteristics.put(GUST_FACTOR, "Gust Factor");
        characteristics.put(HARDWARE_REVISION_STRING, "Hardware Revision String");
        characteristics.put(HEART_RATE_CONTROL_POINT, "Heart Rate Control Point");
        characteristics.put(HEART_RATE_MAX, "Heart Rate Max");
        characteristics.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        characteristics.put(HEAT_INDEX, "Heat Index");
        characteristics.put(HEIGHT, "Height");
        characteristics.put(HID_CONTROL_POINT, "HID Control Point");
        characteristics.put(HID_INFORMATION, "HID Information");
        characteristics.put(HIP_CIRCUMFERENCE, "Hip Circumference");
        characteristics.put(HTTP_CONTROL_POINT, "HTTP Control Point");
        characteristics.put(HTTP_ENTITIY_BODY, "HTTP Entity Body");
        characteristics.put(HTTP_HEADERS, "HTTP Headers");
        characteristics.put(HTTP_STATUS_CODE, "HTTP Status Code");
        characteristics.put(HTTPS_SECURITY, "HTTPS Security");
        characteristics.put(HUMIDITY, "Humidity");
        characteristics.put(IEEE11073_20601_REGULATORY_CERTIFICATION_DATA_LIST, "IEEE 11073-20601 Regulatory Certification Data List");
        characteristics.put(INDOOR_BIKE_DATA, "Indoor Bike Data");
        characteristics.put(INDOOR_POSITIONING_CONFIGURATION, "Indoor Positioning Configuration");
        characteristics.put(INTERMEDIATE_CUFF_PRESSURE, "Intermediate Cuff Pressure");
        characteristics.put(INTERMEDIATE_TEMPERATURE, "Intermediate Temperature");
        characteristics.put(IRRADIANCE, "Irradiance");
        characteristics.put(LANGUAGE, "Language");
        characteristics.put(LAST_NAME, "Last Name");
        characteristics.put(LATITUDE, "Latitude");
        characteristics.put(LN_CONTROL_POINT, "LN Control Point");
        characteristics.put(LN_FEATURE, "LN Feature");
        characteristics.put(LOCAL_EAST_COORDINATE, "Local East Coordinate");
        characteristics.put(LOCAL_NORTH_COORDINATE, "Local North Coordinate");
        characteristics.put(LOCAL_TIME_INFORMATION, "Local Time Information");
        characteristics.put(LOCATION_AND_SPEED, "Location and Speed");
        characteristics.put(LOCATION_NAME, "Location Name");
        characteristics.put(LONGITUDE, "Longitude");
        characteristics.put(MAGNETIC_DECLINATION, "Magnetic Declination");
        characteristics.put(MAGNETIC_FLUX_DENSITY_2D, "Magnetic Flux Density - 2D");
        characteristics.put(MAGNETIC_FLUX_DENSITY_3D, "Magnetic Flux Density - 3D");
        characteristics.put(MANUFACTURER_NAME_STRING, "Manufacturer Name String");
        characteristics.put(MAXIMUM_RECOMMENDED_HEART_RATE, "Maximum Recommended Heart Rate");
        characteristics.put(MEASUREMENT_INTERVAL, "Measurement Interval");
        characteristics.put(MODEL_NUMBER_STRING, "Model Number String");
        characteristics.put(NAVIGATION, "Navigation");
        characteristics.put(NETWORK_AVAILABILITY, "Network Availability");
        characteristics.put(NEW_ALERT, "New Alert");
        characteristics.put(OBJECT_ACTION_CONTROL_POINT, "Object Action Control Point");
        characteristics.put(OBJECT_CHANGED, "Object Changed");
        characteristics.put(OBJECT_FIRST_CREATED, "Object First-Created");
        characteristics.put(OBJECT_ID, "Object ID");
        characteristics.put(OBJECT_LAST_MODIFIED, "Object Last-Modified");
        characteristics.put(OBJECT_LIST_CONTROL_POINT, "Object List Control Point");
        characteristics.put(OBJECT_LIST_FILTER, "Object List Filter");
        characteristics.put(OBJECT_NAME, "Object Name");
        characteristics.put(OBJECT_PROPERTIES, "Object Properties");
        characteristics.put(OBJECT_SIZE, "Object Size");
        characteristics.put(OBJECT_TYPE, "Object Type");
        characteristics.put(OTS_FEATURE, "OTS Feature");
        characteristics.put(PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS, "Peripheral Preferred Connection Parameters");
        characteristics.put(PERIPHERAL_PRIVACY_FLAG, "Peripheral Privacy Flag");
        characteristics.put(PLX_CONTINUOUS_MEASUREMENT, "PLX Continuous Measurement Characteristic");
        characteristics.put(PLX_FEATURES, "PLX Features");
        characteristics.put(PLX_SPOT_CHECK_MEASUREMENT, "PLX Spot-Check Measurement");
        characteristics.put(PNP_ID, "PnP ID");
        characteristics.put(POLLEN_CONCENTRATION, "Pollen Concentration");
        characteristics.put(POSITION_2D, "Position 2D");
        characteristics.put(POSITION_3D, "Position 3D");
        characteristics.put(POSITION_QUALITY, "Position Quality");
        characteristics.put(PRESSURE, "Pressure");
        characteristics.put(PROTOCOL_MODE, "Protocol Mode");
        characteristics.put(PULSE_OXIMETRY_CONTROL_POINT, "Pulse Oximetry Control Point");
        characteristics.put(RAINFALL, "Rainfall");
        characteristics.put(RC_FEATURE, "RC Feature");
        characteristics.put(RC_SETTINGS, "RC Settings");
        characteristics.put(RECONNECTION_ADDRESS, "Reconnection Address");
        characteristics.put(RECONNECTION_CONFIGURATION_CONTROL_POINT, "Reconnection Configuration Control Point");
        characteristics.put(RECORD_ACCESS_CONTROL_POINT, "Record Access Control Point");
        characteristics.put(REFERENCE_TIME_INFORMATION, "Reference Time Information");
        characteristics.put(REMOVABLE, "Removable");
        characteristics.put(REPORT, "Report");
        characteristics.put(REPORT_MAP, "Report Map");
        characteristics.put(RESOLVABLE_PRIVATE_ADDRESS_ONLY, "Resolvable Private Address Only");
        characteristics.put(RESTING_HEART_RATE, "Resting Heart Rate");
        characteristics.put(RINGER_CONTROL_POINT, "Ringer Control Point");
        characteristics.put(RINGER_SETTING, "Ringer Setting");
        characteristics.put(ROWER_DATA, "Rower Data");
        characteristics.put(RSC_FEATURE, "RSC Feature");
        characteristics.put(RSC_MEASUREMENT, "RSC Measurement");
        characteristics.put(SC_CONTROL_POINT, "SC Control Point");
        characteristics.put(SCAN_INTERVAL_WINDOW, "Scan Interval Window");
        characteristics.put(SCAN_REFRESH, "Scan Refresh");
        characteristics.put(SCIENTIFIC_TEMPERATURE_CELSIUS, "Scientific Temperature Celsius");
        characteristics.put(SECONDARY_TIME_ZONE, "Secondary Time Zone");
        characteristics.put(SENSOR_LOCATION, "Sensor Location");
        characteristics.put(SERIAL_NUMBER_STRING, "Serial Number String");
        characteristics.put(SERVICE_CHANGED, "Service Changed");
        characteristics.put(SERVICE_REQUIRED, "Service Required");
        characteristics.put(SOFTWARE_REVISION_STRING, "Software Revision String");
        characteristics.put(SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS, "Sport Type for Aerobic and Anaerobic Thresholds");
        characteristics.put(STAIR_CLIMBER_DATA, "Stair Climber Data");
        characteristics.put(STEP_CLIMBER_DATA, "Step Climber Data");
        characteristics.put(STRING, "String");
        characteristics.put(SUPPORTED_HEART_RATE_RANGE, "Supported Heart Rate Range");
        characteristics.put(SUPPORTED_INCLINATION_RANGE, "Supported Inclination Range");
        characteristics.put(SUPPORTED_NEW_ALERT_CATEGORY, "Supported New Alert Category");
        characteristics.put(SUPPORTED_POWER_RANGE, "Supported Power Range");
        characteristics.put(SUPPORTED_RESISTANCE_LEVEL_RANGE, "Supported Resistance Level Range");
        characteristics.put(SUPPORTED_SPEED_RANGE, "Supported Speed Range");
        characteristics.put(SUPPORTED_UNREAD_ALERT_CATEGORY, "Supported Unread Alert Category");
        characteristics.put(SYSTEM_ID, "System ID");
        characteristics.put(TDS_CONTROL_POINT, "TDS Control Point");
        characteristics.put(TEMPERATURE, "Temperature");
        characteristics.put(TEMPERATURE_CELSIUS, "Temperature Celsius");
        characteristics.put(TEMPERATURE_FAHRENHEIT, "Temperature Fahrenheit");
        characteristics.put(TEMPERATURE_MEASUREMENT, "Temperature Measurement");
        characteristics.put(TEMPERATURE_TYPE, "Temperature Type");
        characteristics.put(THREE_ZONE_HEART_RATE_LIMITS, "Three Zone Heart Rate Limits");
        characteristics.put(TIME_ACCURACY, "Time Accuracy");
        characteristics.put(TIME_BROADCAST, "Time Broadcast");
        characteristics.put(TIME_SOURCE, "Time Source");
        characteristics.put(TIME_UPDATE_CONTROL_POINT, "Time Update Control Point");
        characteristics.put(TIME_UPDATE_STATE, "Time Update State");
        characteristics.put(TIME_WITH_DST, "Time with DST");
        characteristics.put(TIME_ZONE, "Time Zone");
        characteristics.put(TRAINING_STATUS, "Training Status");
        characteristics.put(TREADMILL_DATA, "Treadmill Data");
        characteristics.put(TRUE_WIND_DIRECTION, "True Wind Direction");
        characteristics.put(TRUE_WIND_SPEED, "True Wind Speed");
        characteristics.put(TWO_ZONE_HEART_RATE_LIMIT, "Two Zone Heart Rate Limit");
        characteristics.put(TX_POWER_LEVEL, "Tx Power Level");
        characteristics.put(UNCERTAINTY, "Uncertainty");
        characteristics.put(UNREAD_ALERT_STATUS, "Unread Alert Status");
        characteristics.put(URI, "URI");
        characteristics.put(USER_CONTROL_POINT, "User Control Point");
        characteristics.put(USER_INDEX, "User Index");
        characteristics.put(UV_INDEX, "UV Index");
        characteristics.put(VO2_MAX, "VO2 Max");
        characteristics.put(WAIST_CIRCUMFERENCE, "Waist Circumference");
        characteristics.put(WEIGHT, "Weight");
        characteristics.put(WEIGHT_MEASUREMENT, "Weight Measurement");
        characteristics.put(WEIGHT_SCALE_FEATURE, "Weight Scale Feature");
        characteristics.put(WIND_CHILL, "Wind Chill");

        // Descriptors
        descriptors.put(CHARACTERISTIC_AGGREGATE_FORMAT, "Characteristic Aggregate Format");
        descriptors.put(CHARACTERISTIC_EXTENDED_PROPERTIES, "Characteristic Extended Properties");
        descriptors.put(CHARACTERISTIC_PRESENTATION_FORMAT, "Characteristic Presentation Format");
        descriptors.put(CHARACTERISTIC_USER_DESCRIPTION, "Characteristic User Description");
        descriptors.put(CLIENT_CHARACTERISTIC_CONFIGURATION, "Client Characteristic Configuration");
        descriptors.put(ENVIRONMENTAL_SENSING_CONFIGURATION, "Environmental Sensing Configuration");
        descriptors.put(ENVIRONMENTAL_SENSING_MEASUREMENT, "Environmental Sensing Measurement");
        descriptors.put(ENVIRONMENTAL_SENSING_TRIGGER_SETTING, "Environmental Sensing Trigger Setting");
        descriptors.put(EXTERNAL_REPORT_REFERENCE, "External Report Reference");
        descriptors.put(NUMBER_OF_DIGITALS, "Number of Digitals");
        descriptors.put(REPORT_REFERENCE, "Report Reference");
        descriptors.put(SERVER_CHARACTERISTIC_CONFIGURATION, "Server Characteristic Configuration");
        descriptors.put(TIME_TRIGGER_SETTING, "Time Trigger Setting");
        descriptors.put(VALID_RANGE, "Valid Range");
        descriptors.put(VALUE_TRIGGER_SETTING, "Value Trigger Setting");
    }

    public static String lookupService(UUID uuid) {
        String name = services.get(uuid);
        return name == null ? "<unknown: " + uuid + ">" : name;
    }

    public static String lookupCharacteristic(UUID uuid) {
        String name = characteristics.get(uuid);
        return name == null ? "<unknown: " + uuid + ">" : name;
    }

    public static String lookupDescriptor(UUID uuid) {
        String name = descriptors.get(uuid);
        return name == null ? "<unknown: " + uuid + ">" : name;
    }

    public static void addService(UUID key, String value) {
        services.put(key, value);
    }

    public static void addCharacteristic(UUID key, String value) {
        characteristics.put(key, value);
    }

    public static void addDescriptor(UUID key, String value) {
        descriptors.put(key, value);
    }


    public static Long valueToInt64(BluetoothGattCharacteristic chara) {
        ByteBuffer bb = ByteBuffer.wrap(chara.getValue());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getLong();
    }

}
