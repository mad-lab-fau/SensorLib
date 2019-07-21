/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import java.util.List;

import de.fau.sensorlib.sensors.NilsPodSensor;

public interface NilsPodLoggingCallback {

    void onSessionListRead(NilsPodSensor sensor, List<Session> sessionList);

    void onClearSessions(NilsPodSensor sensor);

    void onSessionDownloadStarted(NilsPodSensor sensor, Session session);

    void onSessionDownloadProgress(NilsPodSensor sensor, SessionDownloader sessionDownloader);

    void onSessionDownloaded(NilsPodSensor sensor, Session session);

}
