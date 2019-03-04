/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.sensors.AbstractSensor;

public class SessionByteWriter {

    private static final String TAG = SessionByteWriter.class.getSimpleName();

    private Context mContext;

    /**
     * Directory name where data will be stored on the external storage
     */
    private String mDirName = "SensorLibRecordings/NilsPodSessionDownloads";

    private Session mSession;

    private String mFilename;
    private BufferedOutputStream mBufferedOutputStream;
    private File mFileHandler;

    private boolean mStorageWritable;
    private boolean mFileCreated;


    public SessionByteWriter(AbstractSensor sensor, Session session, Context context) throws SensorException {
        mContext = context;
        mSession = session;

        mFilename = sensor.getDeviceName() + "_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(session.getStartDate()) + ".bin";


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

    private void prepareWriter() {
        FileOutputStream fos;
        if (mStorageWritable && mFileCreated) {
            try {
                // open buffered writer and write header line
                fos = new FileOutputStream(mFileHandler);
                mBufferedOutputStream = new BufferedOutputStream(fos);
            } catch (Exception e) {
                Log.e(TAG, "Exception on dir and file create!", e);
                mFileCreated = false;
            }
        }
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
                mFileCreated = path.mkdir();
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

        Log.d(TAG, "File successfully created!");
    }


    public void writeData(byte[] data) {
        //Log.d(TAG, mBytes + "/" + mSession.getSessionSize() + " Bytes written!");
        try {
            mBufferedOutputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Closes file after data recording has been completed
     */
    public void completeWriter() {
        if (isWritable()) {
            try {
                // flush and close writer
                mBufferedOutputStream.flush();
                mBufferedOutputStream.close();
                mBufferedOutputStream = null;
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
        return (mStorageWritable && mFileCreated && (mBufferedOutputStream != null));
    }

    public Session getSession() {
        return mSession;
    }

    public String getFilename() {
        return mFilename;
    }
}
