package de.fau.sensorlib.sensors.enums;

import androidx.annotation.NonNull;

import org.apache.commons.text.WordUtils;

/**
 * Enum indicating whether alarm is enabled on NilsPod or not.
 */
public enum NilsPodTimerMode {
    TIMER_DISABLED,
    TIMER_ENABLED;

    @NonNull
    @Override
    public String toString() {
        return WordUtils.capitalizeFully(name().replace("TIMER", "").replace('_', ' '));
    }
}
