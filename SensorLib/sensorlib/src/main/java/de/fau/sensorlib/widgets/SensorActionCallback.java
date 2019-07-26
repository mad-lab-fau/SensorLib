/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets;

import de.fau.sensorlib.enums.SensorAction;
import de.fau.sensorlib.sensors.AbstractSensor;

public interface SensorActionCallback {

    void onSensorActionSelected(AbstractSensor sensor, SensorAction action);

    default void onSensorActionSelected(AbstractSensor sensor, SensorAction action, Object obj) {

    }
}
