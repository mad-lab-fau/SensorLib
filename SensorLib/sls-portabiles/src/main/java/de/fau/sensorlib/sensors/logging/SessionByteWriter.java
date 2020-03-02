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
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.sensors.AbstractSensor;

public class SessionByteWriter {

    private static final String TAG = SessionByteWriter.class.getSimpleName();

    private Context mContext;

    /**
     * Directory name where data will be stored on the external storage
     */
    private static final String DIR_NAME = "SensorLibRecordings/NilsPodSessionDownloads";

    private Session mSession;

    private String mFilename;
    private BufferedOutputStream mBufferedOutputStream;
    private File mFileHandler;

    private boolean mFileCreated;


    public SessionByteWriter(AbstractSensor sensor, Session session, Context context) throws SensorException {
        mContext = context;
        mSession = session;

        mFilename = sensor.getDeviceName() + "_" + session.getSessionStartString() + ".bin";

        if (checkPermissions()) {
            File directory = getDirectory();
            if (directory != null) {
                mFileHandler = new File(directory + "/" + mFilename);
                try {
                    mFileCreated = mFileHandler.createNewFile();
                    if (!mFileCreated) {
                        mFileCreated = mFileHandler.exists();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                prepareWriter();
                Log.d(TAG, getClass().getSimpleName() + " \"" + mFilename + "\" successfully created!");
            }
        } else {
            throw new SensorException(SensorException.SensorExceptionType.permissionsMissing);
        }

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
        if (mFileCreated) {
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

    private File getRootDirectory() {
        boolean storageWritable;
        String state;
        File root;

        root = mContext.getExternalFilesDir(null);
        // try to write on SD card
        state = Environment.getExternalStorageState();
        switch (state) {
            case Environment.MEDIA_MOUNTED:
                // media readable and writable
                storageWritable = true;
                break;
            case Environment.MEDIA_MOUNTED_READ_ONLY:
                // media only readable
                storageWritable = false;
                Log.e(TAG, "SD card only readable!");
                break;
            default:
                // not readable or writable
                storageWritable = false;
                Log.e(TAG, "SD card not readable and writable!");
                break;
        }

        if (!storageWritable) {
            // try to write on external storage
            root = ContextCompat.getDataDir(mContext);
            if (root == null || !root.canWrite()) {
                Log.e(TAG, "External storage not readable and writable!");
            }
        }
        return root;
    }

    private File getDirectory() {
        boolean fileCreated;
        File directory;
        File root = getRootDirectory();

        if (root != null) {
            try {
                // create directory
                directory = new File(root, DIR_NAME);
                fileCreated = directory.mkdirs();

                if (!fileCreated) {
                    fileCreated = directory.exists();
                    if (!fileCreated) {
                        Log.e(TAG, "Directory could not be created!");
                        return null;
                    } else {
                        Log.i(TAG, "Working directory is " + directory.getAbsolutePath());
                    }
                } else {
                    Log.i(TAG, "Directory created at " + directory.getAbsolutePath());
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception on dir and file create!", e);
                return null;
            }
            return directory;
        } else {
            return null;
        }
    }


    public void writeData(byte[] data) {
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
        return (mFileCreated && (mBufferedOutputStream != null));
    }

    public Session getSession() {
        return mSession;
    }

    public String getFilename() {
        return mFilename;
    }


    public void checkFileSize() throws SensorException {
        if (mSession.getSessionSize() != mFileHandler.length()) {
            throw new SensorException(SensorException.SensorExceptionType.sessionDownloadError, "Downloaded size does not match session size!\nExpected: " + mSession.getSessionSize() + ", Actual: " + mFileHandler.length());
        }
    }
}
