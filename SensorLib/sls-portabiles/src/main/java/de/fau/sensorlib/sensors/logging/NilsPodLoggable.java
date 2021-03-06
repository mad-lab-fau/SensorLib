/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.sensors.Erasable;
import de.fau.sensorlib.sensors.Loggable;

public interface NilsPodLoggable extends Loggable, Erasable {

    void readSessionList();

    void downloadSession(int sessionId) throws SensorException;

    default void setCsvExportEnabled(boolean enable) {

    }

}
