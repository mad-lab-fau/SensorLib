package de.fau.sensorlib.sensors;

public interface Resettable {

    /**
     * Attempts a sensor reset.
     *
     * @return true if reset message was successfully sent.
     */
    boolean reset();
}
