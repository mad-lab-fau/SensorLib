package de.fau.sensorlib.widgets;

import java.util.HashMap;

public interface SensorConfigListener {

    void onSensorConfigSelected(HashMap<String, Object> configMap);
}
