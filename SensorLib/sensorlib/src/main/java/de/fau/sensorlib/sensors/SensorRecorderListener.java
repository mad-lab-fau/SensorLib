package de.fau.sensorlib.sensors;

import de.fau.sensorlib.SensorDataRecorder;

public interface SensorRecorderListener {

    void onSensorRecordingStarted(SensorDataRecorder recorder);

    void onSensorRecordingFinished(SensorDataRecorder recorder);
}
