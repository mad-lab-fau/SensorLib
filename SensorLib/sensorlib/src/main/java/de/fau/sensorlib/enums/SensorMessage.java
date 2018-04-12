/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.enums;

/**
 * Possible messages that a sensor can issue.
 */
public enum SensorMessage {

    BATTERY_LEVEL_CHANGED("Battery Level Changed");

    private String name;

    SensorMessage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
