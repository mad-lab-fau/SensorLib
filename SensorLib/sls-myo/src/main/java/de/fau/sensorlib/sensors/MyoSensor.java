/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.content.Context;
import android.util.Log;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;

import java.util.EnumSet;

import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.GestureDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.OrientationDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;

import static de.fau.sensorlib.dataframe.GestureDataFrame.Gesture;

/**
 * Implementation of the Myo band sensor (https://www.myo.com/).
 */
public class MyoSensor extends DsSensor {

    public static class MyoAccelDataFrame extends SensorDataFrame implements AccelDataFrame {

        double ax, ay, az;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MyoAccelDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MyoAccelDataFrame(DsSensor fromSensor, double timestamp, double x, double y, double z) {
            super(fromSensor, timestamp);
            ax = x;
            ay = y;
            az = z;
        }


        @Override
        public double getAccelX() {
            return ax;
        }

        @Override
        public double getAccelY() {
            return ay;
        }

        @Override
        public double getAccelZ() {
            return az;
        }
    }


    public static class MyoGyroDataFrame extends SensorDataFrame implements GyroDataFrame {

        double gx, gy, gz;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MyoGyroDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MyoGyroDataFrame(DsSensor fromSensor, double timestamp, double x, double y, double z) {
            super(fromSensor, timestamp);
            gx = x;
            gy = y;
            gz = z;
        }

        @Override
        public double getGyroX() {
            return gx;
        }

        @Override
        public double getGyroY() {
            return gy;
        }

        @Override
        public double getGyroZ() {
            return gz;
        }
    }

    public static class MyoOrientationDataFrame extends SensorDataFrame implements OrientationDataFrame {

        double roll, pitch, yaw;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MyoOrientationDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MyoOrientationDataFrame(DsSensor fromSensor, double timestamp, double roll, double pitch, double yaw) {
            super(fromSensor, timestamp);
            this.roll = roll;
            this.pitch = pitch;
            this.yaw = yaw;
        }

        @Override
        public double getRoll() {
            return roll;
        }

        @Override
        public double getPitch() {
            return pitch;
        }

        @Override
        public double getYaw() {
            return yaw;
        }
    }

    public static class MyoGestureDataFrame extends SensorDataFrame implements GestureDataFrame {

        Gesture gesture;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MyoGestureDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MyoGestureDataFrame(DsSensor fromSensor, double timestamp, Gesture gesture) {
            super(fromSensor, timestamp);
            this.gesture = gesture;
        }

        @Override
        public Gesture getGesture() {
            return gesture;
        }
    }


    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private DeviceListener mListener = new AbstractDeviceListener() {
        // onConnect() is called whenever a Myo has been connected.
        @Override
        public void onConnect(Myo myo, long timestamp) {
            // Set the text color of the text view to cyan when a Myo connects.
            //mTextView.setTextColor(Color.CYAN);
            MyoSensor.this.mDeviceAddress = myo.getMacAddress();
            sendNotification("Connected to " + myo.getMacAddress());
            sendConnected();
        }

        // onDisconnect() is called whenever a Myo has been disconnected.
        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            // Set the text color of the text view to red when a Myo disconnects.
            //mTextView.setTextColor(Color.RED);
            MyoSensor.this.mDeviceAddress = null;
            sendNotification("Disconnected from " + myo.getMacAddress());

            // TODO: Check that!
            // we unregistered the listener in MyoSensor.disconnect(),
            // so we should only receive a callback if there was any failure, not
            // if the user terminated the connection
            sendConnectionLost();
        }

        // onArmSync() is called whenever Myo has recognized a Sync Gesture after someone has put it on their
        // arm. This lets Myo know which arm it's on and which way it's facing.
        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            sendNotification("Myo synced to " + (myo.getArm() == Arm.LEFT ? "left" : (myo.getArm() == Arm.RIGHT ? "right" : "unknown")) + " arm");
        }

        // onArmUnsync() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
        // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
        // when Myo is moved around on the arm.
        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            sendNotification("Myo unsynced.");

        }

        // onUnlock() is called whenever a synced Myo has been unlocked. Under the standard locking
        // policy, that means poses will now be delivered to the listener.
        @Override
        public void onUnlock(Myo myo, long timestamp) {
            //mLockStateView.setText(R.string.unlocked);
        }

        // onLock() is called whenever a synced Myo has been locked. Under the standard locking
        // policy, that means poses will no longer be delivered to the listener.
        @Override
        public void onLock(Myo myo, long timestamp) {
            //mLockStateView.setText(R.string.locked);
        }

        // onOrientationData() is called whenever a Myo provides its current orientation,
        // represented as a quaternion.
        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            double roll = Math.toDegrees(Quaternion.roll(rotation));
            double pitch = Math.toDegrees(Quaternion.pitch(rotation));
            double yaw = Math.toDegrees(Quaternion.yaw(rotation));
            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                pitch *= -1;
            }

            MyoOrientationDataFrame frame = new MyoOrientationDataFrame(MyoSensor.this, timestamp, roll, pitch, yaw);

            sendNewData(frame);
        }

        @Override
        public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
            MyoAccelDataFrame frame = new MyoAccelDataFrame(MyoSensor.this, timestamp, accel.x(), accel.y(), accel.z());

            sendNewData(frame);
        }

        @Override
        public void onGyroscopeData(Myo myo, long timestamp, Vector3 gyro) {
            MyoGyroDataFrame frame = new MyoGyroDataFrame(MyoSensor.this, timestamp, gyro.x(), gyro.y(), gyro.z());

            sendNewData(frame);
        }

        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration
            Gesture gesture = null;
            switch (pose) {
                case UNKNOWN:
                    gesture = Gesture.UNKNOWN;
                    break;
                case REST:
                    gesture = Gesture.REST;
                    break;
                case DOUBLE_TAP:
                    gesture = Gesture.DOUBLE_TAP;
                    break;
                case FIST:
                    gesture = Gesture.FIST;
                    break;
                case WAVE_IN:
                    gesture = Gesture.WAVE_IN;
                    break;
                case WAVE_OUT:
                    gesture = Gesture.WAVE_OUT;
                    break;
                case FINGERS_SPREAD:
                    gesture = Gesture.FINGERS_SPREAD;
                    break;
            }

            MyoGestureDataFrame frame = new MyoGestureDataFrame(MyoSensor.this, timestamp, gesture);
            sendNewData(frame);

            if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                // Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                // hold the poses without the Myo becoming locked.
                myo.unlock(Myo.UnlockType.HOLD);
                // Notify the Myo that the pose has resulted in an action, in this case changing
                // the text on the screen. The Myo will vibrate.
                myo.notifyUserAction();
            } else {
                // Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                // stay unlocked while poses are being performed, but lock after inactivity.
                myo.unlock(Myo.UnlockType.TIMED);
            }
        }

    };


    public MyoSensor(Context context, SensorDataProcessor dataHandler) {
        super(context, "Myo", null, dataHandler);
    }

    @Override
    public boolean connect() throws Exception {
        Log.d("", "preConnect");
        super.connect();
        // First, we initialize the Hub singleton with an application identifier.
        Hub hub = Hub.getInstance();
        if (!hub.init(mContext, mContext.getPackageName())) {
            // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
            //Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
            //finish();
            return false;
        }
        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);
        hub.attachToAdjacentMyo();

        sendConnected();

        return true;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);
        // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
        Hub.getInstance().shutdown();

        sendDisconnected();
    }

    @Override
    public void startStreaming() {
        sendStartStreaming();
    }

    @Override
    public void stopStreaming() {
        sendStopStreaming();
    }

}
