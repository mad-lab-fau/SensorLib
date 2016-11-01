/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlibsample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.choosemuse.libmuse.MuseDataPacketType;

import java.util.Arrays;
import java.util.EnumSet;

import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.DsSensorManager;
import de.fau.sensorlib.KnownSensor;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorFoundCallback;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensorpicker.DsSensorPickerFragment;
import de.fau.sensorlib.sensors.MuseSensor;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private MuseSensor mMuseSensor;

    private SensorDataProcessor mSensorDataHandler = new SensorDataProcessor() {

        @Override
        public void onSensorCreated(DsSensor sensor) {
            super.onSensorCreated(sensor);
            /*if (sensor instanceof MuseSensor) {
                try {
                    sensor.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }*/
        }

        @Override
        public void onConnected(DsSensor sensor) {
            super.onConnected(sensor);
            sensor.startStreaming();
        }

        @Override
        public void onStopStreaming(DsSensor sensor) {
            super.onStopStreaming(sensor);
            sensor.disconnect();
        }

        @Override
        public void onDisconnected(DsSensor sensor) {
            super.onDisconnected(sensor);
            mMuseSensor = null;
        }

        @Override
        public void onNewData(SensorDataFrame data) {
            if (data instanceof MuseSensor.MuseEegDataFrame) {
                if (((MuseSensor.MuseEegDataFrame) data).getPacketType() == MuseDataPacketType.EEG) {
                    Log.d(TAG, "new MUSE EEG RAW data: " + Arrays.toString(((MuseSensor.MuseEegDataFrame) data).getRawEegChannels()));
                } else {
                    Log.d(TAG, "new MUSE EEG BAND data: " + Arrays.toString(((MuseSensor.MuseEegDataFrame) data).getEegBands()));
                }
            } else if (data instanceof MuseSensor.MuseAccelDataFrame) {
                Log.d(TAG, "new MUSE ACCEL data: " +
                        ((MuseSensor.MuseAccelDataFrame) data).getAccelX() + ", " +
                        ((MuseSensor.MuseAccelDataFrame) data).getAccelY() + ", " +
                        ((MuseSensor.MuseAccelDataFrame) data).getAccelZ());

            }
        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                final FragmentManager fm = getSupportFragmentManager();
                final DsSensorPickerFragment fragment = new DsSensorPickerFragment();
                try {
                    DsSensorManager.checkBtLePermissions(this, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                fragment.setSensorFoundCallback(new SensorFoundCallback() {
                    @Override
                    public boolean onKnownSensorFound(SensorInfo sensor) {
                        Log.d(TAG, "Known sensor: " + sensor.getName());
                        if (KnownSensor.MUSE.equals(sensor.getDeviceClass())) {
                            mMuseSensor = new MuseSensor(MainActivity.this, sensor, mSensorDataHandler);
                        }
                        return false;
                    }

                    @Override
                    public void onSensorsSelected(EnumSet<DsSensor.HardwareSensor> selectedSensors) {
                        super.onSensorsSelected(selectedSensors);
                        Log.d(TAG, "selected sensors: " + Arrays.toString(selectedSensors.toArray()));
                        try {
                            mMuseSensor.useHardwareSensors(selectedSensors);
                            mMuseSensor.connect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                fragment.show(fm, "sensor_picker");
                break;
        }
    }
}
