# SensorLib
The SensorLib is a Java-based Android library that provides an easy-to-use interface to access various retail and development sensor systems in a unified way.

## Minimal example
Using the SensorLib is very easy, insert the following code into your app activity for a minimal working example:
```
InternalSensor is = new InternalSensor(this, new SensorDataProcessor() {
    @Override
    public void onNewData(SensorDataFrame sensorDataFrame) {
        Log.d("Example", "new data: " + sensorDataFrame);
    }
});
is.connect();
is.startStreaming();
```

When you are done using the sensor, just call:
```
is.disconnect();
```

## Using BLE (Bluetooth Low Energy) sensors in Android 6+
If you wish to use BLE sensors (e.g. the DsBleSensor or extensions of it) you need to ask for all location and BT permissions during runtime of the App.
This can be easily done by calling
```
DsSensorManager.checkBtLePermissions(this, true);
```
The first argument is the Activity/Context, the second argument specifies whether the method should also ask the user to grant the app the required permissions.
