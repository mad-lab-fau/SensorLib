# SensorLib
The SensorLib is a Java-based Android library that provides an easy-to-use interface to access various retail and development sensor systems in a unified way.

## Structure
The SensorLib is composed of the main library ("sensorlib") and multiple submodules, one for each sensor, each starting with the tag "sls-" (e.g. "sls-shimmer", "sls-tek" or "sls-empatica").

## Compiling the library
The library should be compiled against the latest Android SDK and should work right out of the box. For some sensors an additional external library (SDK) is necessary. You can usually download these from the sensor's specific SDK website.

Right now, the following external libraries are required to compile all sensor modules:
- myosdk-0.10.0.aar from the official Myo SDK website (required by sls-myo)
- empalink-2.0.aar from the official Empatica SDK website (required by sls-empatica)

## Minimal example
Using the SensorLib is very easy.

First, you have to include the sensorlib.aar and the respective sensor-submodule you want to use into your build.gradle file. To do this, just add the following line to your build.gradle:
```
repositories {
    flatDir {
        dirs 'libs'
    }
}

...

dependencies {
    ...
    compile project(path: ':sensorlib')
    compile project(':sls-tek')
    ...
}
```


Afterwards, just insert the following code into your app activity for a minimal working example:
```
InternalSensor sensor = new InternalSensor(this, new SensorDataProcessor() {
    @Override
    public void onNewData(SensorDataFrame sensorDataFrame) {
        Log.d("Example", "new data: " + sensorDataFrame);
    }
});
sensor.connect();
sensor.startStreaming();
```

When you are done using the sensor, just call:
```
sensor.disconnect();
```

## Using BLE (Bluetooth Low Energy) sensors in Android 6+
If you wish to use BLE sensors (e.g. the DsBleSensor or extensions of it) you need to ask for all location and BT permissions during runtime of the App.
This can be easily done by calling
```
DsSensorManager.checkBtLePermissions(this, true);
```
The first argument is the Activity/Context, the second argument specifies whether the method should also ask the user to grant the app the required permissions.


## Adding additional data listeners
In the above minimal example, we used an implicit implementation of the SensorDataProcessor class. This class is used for callback notifications of anything that happens with the sensor (new data frames arrive or connection/disconnection events).

If you implement more extensive event handling for a sensor, it is ofte desired to have multiple different event handlers. For example you can create a handler that is just used to store new sensor data in a csv file, or one that handles plotting of the data in the UI.

To do that you create an additional class that extends SensorDataProcessor and override the callbacks that you wish to use. You then instantiate your extended class and add it to your sensor
```
public class CustomDataHandler extends SensorDataProcessor {
    @Override
    public void onNewData(SensorDataFrame sensorDataFrame) {
        // handle the new data
    }

    @Override
    public void onConnected(DsSensor sensor) {
        super.onConnected(sensor);
        // do something when the sensor connects.
    }
}

...
SensorDataProcessor handler = new CustomDataHandler();
sensor.addDataHandler(handler)
...
```

## How to use sensor data frames
You can check whether a data frame contains desired data by using Java reflection:
```
    @Override
    public void onNewData(SensorDataFrame sensorDataFrame) {
        // check if the data frame contains accelerometer data
        if (sensorDataFrame instanceof AccelDataFrame) {
            AccelDataFrame adf = (AccelDataFrame)sensorDataFrame;
            Log.d(TAG, "X acceleration: " + adf.getAccelX());
        }
    }
```