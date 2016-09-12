package de.fau.teksample;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import de.fau.sensorlib.DsBleSensor;
import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.DsSensorManager;
import de.fau.sensorlib.KnownSensor;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorFoundCallback;
import de.fau.sensorlib.sensors.TEK;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    /**
     * Reference to our connected sensor.
     */
    DsBleSensor mSensor;

    /**
     * The data logger to write sensor data into a csv file.
     */
    CsvDataLogger mCsvDataLogger = new CsvDataLogger();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // For Android 6+ we have to make sure that we have the BLE permissions
        DsSensorManager.checkBtLePermissions(this, true);
        // ...and permissions to write on the storage for csv file logging.
        CsvDataLogger.requestStorageWritePermission(this);
    }


    /**
     * The data handler for our sensor which will receive callbacks whenever new data is available
     */
    SensorDataProcessor mDataHandler = new CustomDataProcessor();

    /**
     * Helper method to retrieve instance pointer in nested classes.
     *
     * @return
     */
    Activity getThis() {
        return this;
    }

    /**
     * Connect to a TEK sensor directly using the sensor's MAC address.
     *
     * @param macAddress MAC address of the sensor.
     */
    private void connectTekWithMac(String macAddress) {
        Log.d(TAG, "Connecting to " + macAddress);
        mSensor = new TEK(this, macAddress, mDataHandler);
        mSensor.useHardwareSensor(DsSensor.HardwareSensor.ACCELEROMETER);
        mSensor.useHardwareSensor(DsSensor.HardwareSensor.LIGHT);
        //mSensor.addDataHandler(mCsvDataLogger);
        try {
            if (mSensor.connect()) {
                Log.d(TAG, "Connected");
                mSensor.startStreaming();
            } else {
                Log.d(TAG, "Connection failed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Connect to the first TEK sensor that is found when searching all BLE devices in the area.
     */
    private void connectFirstFoundTek() {
        try {
            DsSensorManager.searchBleDevices(this, new SensorFoundCallback() {
                public boolean onKnownSensorFound(KnownSensor sensor) {
                    // This is called whenever a new BLE sensor was found that can be accessed via the SensorLib.
                    Log.d(TAG, "BLE Sensor found: " + sensor.getDeviceName());

                    // Check if it is a TEK sensor
                    if (sensor == KnownSensor.TEK) {
                        // It is a TEK: create the sensor...
                        mSensor = new TEK(getThis(), sensor, mDataHandler);
                        // ...select the desired hardware sensors...
                        mSensor.useHardwareSensor(DsSensor.HardwareSensor.ACCELEROMETER);
                        mSensor.useHardwareSensor(DsSensor.HardwareSensor.LIGHT);
                        //mSensor.addDataHandler(mCsvDataLogger);
                        try {
                            // ...connect to it...
                            mSensor.connect();
                            // ...and start streaming data.
                            // New data will now appear in the callback above.
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
    protected void onResume() {
        super.onResume();
        // This callback is called by Android every time the app resumes control

        // This codeblock allows to connect to a sensor directly.
        String tekMac = "52:4D:4B:5F:01:23";
        connectTekWithMac(tekMac);


        // Search for available BT LE devices
        //connectFirstFoundTek();
    }

    @Override
    protected void onPause() {
        // If we lose control we disconnect the sensor.
        if (mSensor != null) {
            Log.d(TAG, "Disconnecting sensor.");
            mSensor.stopStreaming();
            mSensor.disconnect();
        }
        super.onPause();
    }
}
