package de.fau.sensorlib.sensors;

public interface Loggable {

    /**
     * Enables data logging for this sensor
     */
    void setLoggerEnabled();

    /**
     * Disables data logging for this sensor
     */
    void setLoggerDisabled();
}
