package de.fau.sensorlib;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.core.content.ContextCompat;
import de.fau.sensorlib.dataframe.RealTimeTimestampDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Dynamic data recorder for sensor data. If possible, data is stored onto the device's SD card. If not,
 * data is stores to the device's external storage.
 */
public class SensorDataRecorder {

    private static final String TAG = SensorDataRecorder.class.getSimpleName();

    /**
     * Value separator
     */
    private static final String SEPARATOR = ",";
    /**
     * Line delimiter
     */
    private static final String DELIMITER = "\r\n";
    /**
     * Directory name where data will be stored on the external storage
     */
    private String mDirName = "SensorLibRecordings";
    /**
     * File header
     */
    private String mHeader;
    private String mFilename;
    private String mAbsolutePath;
    private BufferedWriter mBufferedWriter;
    private File mFileHandler;
    private boolean mStorageWritable;
    private boolean mFileCreated;
    private Context mContext;

    private List<Method> mMethodList;

    private AbstractSensor mSensor;


    /**
     * Creates a new data recorder instance
     */
    public SensorDataRecorder(AbstractSensor sensor, Context context) throws SensorException {
        this(sensor, context, null, null, null);
    }

    /**
     * Creates a new data recorder instance
     */
    public SensorDataRecorder(AbstractSensor sensor, Context context, String header, String subDir, Date date) throws SensorException {
        mContext = context;
        mSensor = sensor;

        String time;
        if (date == null) {
            date = new Date();
        }
        time = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(date);

        // Filename consists of sensor device name and start time of data recording
        mFilename = mSensor.getDeviceName() + "_" + time + ".csv";

        StringBuilder headerBuilder = new StringBuilder();
        if (header == null) {
            headerBuilder.append("samplingrate" + SEPARATOR).append(sensor.getSamplingRate()).append(DELIMITER);
        } else {
            headerBuilder.append(header).append(DELIMITER);
        }

        if (subDir != null) {
            mDirName += ("/" + subDir);
        }

        headerBuilder.append("timestamp").append(SEPARATOR);

        List<String> mColumnList = new ArrayList<>();
        mMethodList = new ArrayList<>();

        for (HardwareSensor hwSensor : mSensor.getDeviceClass().getAvailableSensors()) {
            try {
                String[] cols = (String[]) hwSensor.getDataFrameClass().getDeclaredField("COLUMNS").get("null");
                mColumnList.addAll(Arrays.asList(cols));
                mMethodList.addAll(Arrays.asList(hwSensor.getDataFrameClass().getDeclaredMethods()));
            } catch (Exception ignored) {

            }
        }

        for (int i = 0; i < mColumnList.size(); i++) {
            headerBuilder.append(mColumnList.get(i));
            if (i != mColumnList.size() - 1) {
                headerBuilder.append(SEPARATOR);
            }
        }
        headerBuilder.append(DELIMITER);

        mHeader = headerBuilder.toString();
        Log.d(TAG, mHeader);

        if (checkPermissions()) {
            createFile();
            prepareWriter();
        } else {
            throw new SensorException(SensorException.SensorExceptionType.permissionsMissing);
        }

        Log.d(TAG, getClass().getSimpleName() + " \"" + mFilename + "\" successfully created!");
    }


    public AbstractSensor getOriginatingSensor() {
        return mSensor;
    }

    public String getFilename() {
        return mFilename;
    }

    public String getAbsoluteFilePath() {
        return mAbsolutePath;
    }

    /**
     * Creates a new data recorder instance
     */
    public SensorDataRecorder(AbstractSensor sensor, HardwareSensor hwSensor, Context context) throws SensorException {
        mContext = context;
        mSensor = sensor;

        String currTime = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        // Filename consists of sensor device name and start time of data recording
        mFilename = mSensor.getDeviceName() + "_" + hwSensor.getShortDescription() + "_" + currTime + ".csv";

        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("samplingrate" + SEPARATOR).append(sensor.getSamplingRate()).append(DELIMITER);

        headerBuilder.append("timestamp").append(SEPARATOR);

        List<String> mColumnList = new ArrayList<>();
        mMethodList = new ArrayList<>();

        try {
            String[] cols = (String[]) hwSensor.getDataFrameClass().getDeclaredField("COLUMNS").get("null");
            mColumnList.addAll(Arrays.asList(cols));
            mMethodList.addAll(Arrays.asList(hwSensor.getDataFrameClass().getDeclaredMethods()));
        } catch (Exception ignored) {
        }

        for (int i = 0; i < mColumnList.size(); i++) {
            headerBuilder.append(mColumnList.get(i));
            if (i != mColumnList.size() - 1) {
                headerBuilder.append(SEPARATOR);
            }
        }
        headerBuilder.append(DELIMITER);

        mHeader = headerBuilder.toString();
        Log.d(TAG, mHeader);

        if (checkPermissions()) {
            createFile();
            prepareWriter();
        } else {
            throw new SensorException(SensorException.SensorExceptionType.permissionsMissing);
        }

        Log.d(TAG, getClass().getSimpleName() + " \"" + mFilename + "\" successfully created!");
    }

    /**
     * Checks if permissions to read and write external storage have been granted by the user
     *
     * @return true if permissions have been granted, false otherwise
     */
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void createFile() {
        String state;
        File root = null;
        File path;

        // try to write on SD card
        state = Environment.getExternalStorageState();
        switch (state) {
            case Environment.MEDIA_MOUNTED:
                // media readable and writable
                root = Environment.getExternalStorageDirectory();
                mStorageWritable = true;
                break;
            case Environment.MEDIA_MOUNTED_READ_ONLY:
                // media only readable
                mStorageWritable = false;
                Log.e(TAG, "SD card only readable!");
                break;
            default:
                // not readable or writable
                mStorageWritable = false;
                Log.e(TAG, "SD card not readable and writable!");
                break;
        }

        if (!mStorageWritable) {
            // try to write on external storage
            root = Environment.getDataDirectory();
            if (root.canWrite()) {
                mStorageWritable = true;
            } else {
                Log.e(TAG, "External storage not readable and writable!");
                Toast.makeText(mContext, "External storage not readable and writable!", Toast.LENGTH_SHORT).show();
            }
        }

        if (mStorageWritable) {
            try {
                // create directory
                path = new File(root, mDirName);
                mFileCreated = path.mkdirs();
                if (!mFileCreated) {
                    mFileCreated = path.exists();
                    if (!mFileCreated) {
                        Log.e(TAG, "File could not be created!");
                        return;
                    } else {
                        Log.i(TAG, "Working directory is " + path.getAbsolutePath());
                    }
                }
                // create files
                mFileHandler = new File(path + "/" + mFilename);
                mFileCreated = mFileHandler.createNewFile();
                if (!mFileCreated) {
                    mFileCreated = mFileHandler.exists();
                    if (!mFileCreated) {
                        Log.e(TAG, "File could not be created!");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception on dir and file create!", e);
                mFileCreated = false;
            }
        }

        mAbsolutePath = mFileHandler.getAbsolutePath();
        Log.d(TAG, "File successfully created!");
    }

    private void prepareWriter() {
        FileWriter fw;
        if (mStorageWritable && mFileCreated) {
            try {
                // open buffered writer and write header line
                fw = new FileWriter(mFileHandler);
                mBufferedWriter = new BufferedWriter(fw);
                mBufferedWriter.write(mHeader);
            } catch (Exception e) {
                Log.e(TAG, "Exception on dir and file create!", e);
                mFileCreated = false;
            }
        }
    }

    /**
     * Writes next line of sensor data
     *
     * @param data data frame from Hoop Sensor
     */
    public void writeData(SensorDataFrame data) {
        StringBuilder sb = new StringBuilder();
        if (isWritable()) {
            // write timestamp
            if (data instanceof RealTimeTimestampDataFrame) {
                sb.append(((RealTimeTimestampDataFrame) data).getRealTimeTimestamp()).append(SEPARATOR);
            } else {
                sb.append(data.getTimestamp()).append(SEPARATOR);
            }
            // write sensor data
            for (int i = 0; i < mMethodList.size(); i++) {
                try {
                    Object val = mMethodList.get(i).invoke(data);
                    sb.append(val);

                } catch (Exception ignore) {
                    // method call failed, which means that the current data frame is
                    // supported by the sensor, but not streamed at the moment
                    // => skip column in finally block
                } finally {
                    if (i < mMethodList.size() - 1) {
                        sb.append(SEPARATOR);
                    }
                }
            }
            sb.append(DELIMITER);
            try {
                Log.d(TAG, sb.toString());
                mBufferedWriter.write(sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Storage not writable!");
        }
    }

    /**
     * Closes file after data recording has been completed
     */
    public void completeRecorder() {
        if (isWritable()) {
            try {
                // flush and close writer
                mBufferedWriter.flush();
                mBufferedWriter.close();
                mBufferedWriter = null;
            } catch (Exception e) {
                Log.e(TAG, "Error on completing recorder!");
            }
        }
    }

    /**
     * Checks if data can be written to the device
     *
     * @return true if data can be written, false otherwise
     */
    private boolean isWritable() {
        return (mStorageWritable && mFileCreated && (mBufferedWriter != null));
    }

}
