package de.fau.teksample;

import android.util.Log;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.AmbientDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.MagDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensors.TEK;

/**
 * This is an implementation of a SensorDataProcessor and receives all the data and information from our connected TEK sensor.
 */
public class CustomDataProcessor extends SensorDataProcessor {
    private static final String TAG = "CustomDataProcessor";

    @Override
    public void onNewData(SensorDataFrame sensorDataFrame) {
        // This callback is called every time a new dataframe is available from the sensor.

        // We first cast the dataframe to our TekDataFrame
        TEK.TekDataFrame df = (TEK.TekDataFrame) sensorDataFrame;

        // We can now access the elements of the dataframe
        Log.d(TAG, "DataFrame (" + df.getCounter() + "): " + df.toString());

        if (sensorDataFrame instanceof AccelDataFrame) {
            AccelDataFrame adf = (AccelDataFrame) sensorDataFrame;
            // YOUR CODE HERE
        }
        if (sensorDataFrame instanceof GyroDataFrame) {
            // YOUR CODE HERE
        }
        if (sensorDataFrame instanceof MagDataFrame) {
            // YOUR CODE HERE
        }
        if (sensorDataFrame instanceof AmbientDataFrame) {
            // YOUR CODE HERE
        }
        if (sensorDataFrame instanceof TEK.TekFusionDataFrame) {
            // YOUR CODE HERE
        }
    }
}
