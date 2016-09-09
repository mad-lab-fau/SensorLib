package de.fau.teksample;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.AmbientDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.MagDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensors.TEK;

/**
 * An instance of the SensorDataProcessor for logging all sensor data to two different csv files. One for ambient and one for inertial sensor data.
 */
public class CsvDataLogger extends SensorDataProcessor {

    private static final String TAG = "CsvDataLogger";

    FileWriter mCsvWriterInertial;
    FileWriter mCsvWriterAmbient;

    /**
     * Requests the write permission to external storage for Android 6+ devices.
     *
     * @param activity
     */
    public static void requestStorageWritePermission(Activity activity) {
        if (Build.VERSION.SDK_INT < 23)
            return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
        }
    }

    @Override
    public void onNewData(SensorDataFrame sensorDataFrame) {
// We first cast the dataframe to our TekDataFrame
        TEK.TekDataFrame df = (TEK.TekDataFrame) sensorDataFrame;

        // We can now access the elements of the dataframe
        Log.d(TAG, "DataFrame (" + df.getCounter() + "): " + df.toString());

        try {
            // check if this data frame contains environment information
            if (mCsvWriterAmbient != null && df instanceof AmbientDataFrame) {
                AmbientDataFrame adf = (AmbientDataFrame) df;
                mCsvWriterAmbient.write(adf.getHumidity() + "," + adf.getLight() + "," + adf.getNoise() + "," + adf.getPressure() + "," + adf.getTemperature() + "\n");
                mCsvWriterAmbient.flush();
            }
            // check if this data frame contains inertial sensor information
            if (mCsvWriterInertial != null && df instanceof AccelDataFrame && df instanceof GyroDataFrame && df instanceof MagDataFrame) {
                AccelDataFrame adf = (AccelDataFrame) df;
                GyroDataFrame gdf = (GyroDataFrame) df;
                MagDataFrame mdf = (MagDataFrame) df;
                mCsvWriterInertial.write(adf.getAccelX() + "," + adf.getAccelY() + "," + adf.getAccelZ() + "," + gdf.getGyroX() + "," + gdf.getGyroY() + "," + gdf.getGyroZ() + "," + mdf.getMagX() + "," + mdf.getMagY() + "," + mdf.getMagZ() + "\n");
                mCsvWriterInertial.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStopStreaming(DsSensor sensor) {
        super.onStopStreaming(sensor);

        try {
            mCsvWriterInertial.close();
            mCsvWriterAmbient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(DsSensor sensor) {
        super.onConnected(sensor);

        // Get a filename for the csv file in the public download directory
        File dataDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File csvFileInertial = new File(dataDir, Calendar.getInstance().getTime().toString() + "-inertial.csv");
        File csvFileAmbient = new File(dataDir, Calendar.getInstance().getTime().toString() + "-ambient.csv");
        Log.d(TAG, "csv: " + csvFileInertial.getAbsolutePath());
        try {
            // Create the csv file
            mCsvWriterInertial = new FileWriter(csvFileInertial);
            mCsvWriterAmbient = new FileWriter(csvFileAmbient);
            mCsvWriterAmbient.write("Humidity, Light, Noise, Pressure, Temperature\n");
            mCsvWriterInertial.write("AccelX, AccelY, AccelZ, GyroX, GyroY, GyroZ, MagX, MagY, MagZ\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
