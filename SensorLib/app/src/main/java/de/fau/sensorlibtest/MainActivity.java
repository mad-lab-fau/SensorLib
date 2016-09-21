/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlibtest;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.androidplot.util.PlotStatistics;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYPlot;

import java.util.List;

import de.fau.sensorlib.DsBleSensor;
import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.DsSensorManager;
import de.fau.sensorlib.KnownSensor;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorFoundCallback;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.AmbientDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensors.TEK;

/**
 * A simple example application to demonstrate the use of the SensorLib.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SensorLib::TestApp";

    DsBleSensor mSensor;

    XYPlot mPlot;
    SimpleXYSeries mPlotData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DsSensorManager.checkBtLePermissions(this, true);

        mPlot = (XYPlot) findViewById(R.id.plotViewA);
        mPlotData = new SimpleXYSeries("Sensor Values");
        mPlotData.useImplicitXVals();
        mPlot.addSeries(mPlotData, new LineAndPointFormatter(Color.rgb(100, 100, 200), null, null, new PointLabelFormatter(Color.DKGRAY)));
        mPlot.setDomainLabel("Sample Index");
        mPlot.setRangeLabel("Value");
    }

    SensorDataProcessor mDataHandler = new SensorDataProcessor() {
        @Override
        public void onNewData(SensorDataFrame data) {
            if (mPlotData.size() > 99) {
                mPlotData.removeFirst();
            }

            if (data instanceof AccelDataFrame) {
                AccelDataFrame adf = (AccelDataFrame) data;
                mPlotData.addLast(null, adf.getAccelX());
            }
            if (data instanceof AmbientDataFrame) {
                AmbientDataFrame adf = (AmbientDataFrame) data;
                mPlotData.addLast(null, adf.getLight());
            }

            TEK.TekDataFrame df = (TEK.TekDataFrame) data;
            Log.d(TAG, "DataFrame (" + df.getCounter() + "): " + df.toString());

            // redraw the Plots:
            mPlot.redraw();
        }
    };

    Activity getThis() {
        return this;
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*String tekMac = "52:4D:4B:5F:01:55";
        mSensor = new TEK(this, tekMac, mDataHandler);
        mSensor.useHardwareSensor(DsSensor.HardwareSensor.ACCELEROMETER);
        mSensor.useHardwareSensor(DsSensor.HardwareSensor.LIGHT);
        try {
            mSensor.connect();
            mSensor.startStreaming();
        } catch (Exception e) {
            e.printStackTrace();
        }*/


        List<KnownSensor> list = DsSensorManager.getConnectableSensors();
        for (KnownSensor s : list) {
            Log.d(TAG, "Sensor found: " + s.getDeviceName());
        }

        try {
            DsSensorManager.searchBleDevices(this, new SensorFoundCallback() {
                public boolean onKnownSensorFound(KnownSensor sensor) {
                    Log.d(TAG, "BLE Sensor found: " + sensor.getDeviceName());

                    // we check what kind of sensor we found
                    if (sensor == KnownSensor.GENERIC_BLE) {
                        // ignore default/unknown BLE sensors
                        //if (sensor.getDeviceName().contains("miCoach")) {

                    } else if (sensor == KnownSensor.TEK) {
                        // this is a TEK sensor, create and connect it.
                        mSensor = new TEK(getThis(), sensor, mDataHandler);
                        mSensor.useHardwareSensor(DsSensor.HardwareSensor.ACCELEROMETER);
                        mSensor.useHardwareSensor(DsSensor.HardwareSensor.LIGHT);
                        try {
                            mSensor.connect();
                            mSensor.startStreaming();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        if (mSensor != null) {
            mSensor.disconnect();
        }
        super.onPause();
    }
}
