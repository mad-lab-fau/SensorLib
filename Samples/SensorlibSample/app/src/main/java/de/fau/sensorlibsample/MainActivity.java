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

import java.util.Arrays;
import java.util.EnumSet;

import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.SensorFoundCallback;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.sensorpicker.DsSensorPickerFragment;

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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                final FragmentManager fm = getSupportFragmentManager();
                final DsSensorPickerFragment fragment = new DsSensorPickerFragment();
                fragment.setSensorFoundCallback(new SensorFoundCallback() {
                    @Override
                    public boolean onKnownSensorFound(SensorInfo sensor) {
                        Log.d(TAG, "Known sensor: " + sensor.getName());
                        return false;
                    }

                    @Override
                    public void onSensorsSelected(EnumSet<DsSensor.HardwareSensor> selectedSensors) {
                        super.onSensorsSelected(selectedSensors);
                        Log.d(TAG, Arrays.toString(selectedSensors.toArray()));
                    }
                });
                fragment.show(fm, "sensor_picker");
                break;
        }
    }
}
