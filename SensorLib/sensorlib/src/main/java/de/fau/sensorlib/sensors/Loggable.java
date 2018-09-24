package de.fau.sensorlib.sensors;

public interface Loggable {

    /**
     * Enables data logging for this sensor
     */
    void enableDataLogger();

    /**
     * Disables data logging for this sensor
     */
    void disableDataLogger();
}
