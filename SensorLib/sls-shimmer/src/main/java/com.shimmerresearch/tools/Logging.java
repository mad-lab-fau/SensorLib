package com.shimmerresearch.tools;

import android.os.Environment;
import android.util.Log;

import com.google.common.collect.Multimap;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public class Logging {
    boolean mFirstWrite = true;
    String[] mSensorNames;
    String[] mSensorFormats;
    String[] mSensorUnits;
    String mFileName = "";
    BufferedWriter writer = null;
    File outputFile;
    String mDelimiter = ","; //default is comma

    /**
     * @param myName is the file name which will be used
     */
    public Logging(String myName) {
        mFileName = myName;
        File root = Environment.getExternalStorageDirectory();
        Log.d("AbsolutePath", root.getAbsolutePath());
        outputFile = new File(root, mFileName + ".dat");
    }

    public Logging(String myName, String delimiter) {
        mFileName = myName;
        mDelimiter = delimiter;
        File root = Environment.getExternalStorageDirectory();
        Log.d("AbsolutePath", root.getAbsolutePath());
        outputFile = new File(root, mFileName + ".dat");
    }

    /**
     * @param myName
     * @param delimiter
     * @param folderName will create a new folder if it does not exist
     */
    public Logging(String myName, String delimiter, String folderName) {
        mFileName = myName;
        mDelimiter = delimiter;

        File root = new File(Environment.getExternalStorageDirectory() + "/" + folderName);

        if (!root.exists()) {
            if (root.mkdir()) ; //directory is created;
        }
        outputFile = new File(root, mFileName + ".dat");
    }


    /**
     * This function takes an object cluster and logs all the data within it. User should note that the function will write over prior files with the same name.
     *
     * @param objectCluster data which will be written into the file
     */
    public void logData(ObjectCluster objectCluster) {
        ObjectCluster objectClusterLog = objectCluster;
        try {


            if (mFirstWrite == true) {
                writer = new BufferedWriter(new FileWriter(outputFile, true));

                //First retrieve all the unique keys from the objectClusterLog
                Multimap<String, FormatCluster> m = objectClusterLog.mPropertyCluster;

                int size = m.size();
                System.out.print(size);
                mSensorNames = new String[size];
                mSensorFormats = new String[size];
                mSensorUnits = new String[size];
                int i = 0;
                int p = 0;
                for (String key : m.keys()) {
                    //first check that there are no repeat entries

                    if (compareStringArray(mSensorNames, key) == true) {
                        for (FormatCluster formatCluster : m.get(key)) {
                            mSensorFormats[p] = formatCluster.mFormat;
                            mSensorUnits[p] = formatCluster.mUnits;
                            //Log.d("Shimmer",key + " " + mSensorFormats[p] + " " + mSensorUnits[p]);
                            p++;
                        }

                    }

                    mSensorNames[i] = key;
                    i++;
                }


                // write header to a file

                writer = new BufferedWriter(new FileWriter(outputFile, false));

                for (int k = 0; k < mSensorNames.length; k++) {
                    writer.write(objectClusterLog.mMyName);
                    writer.write(mDelimiter);
                }
                writer.newLine(); // notepad recognized new lines as \r\n

                for (int k = 0; k < mSensorNames.length; k++) {
                    writer.write(mSensorNames[k]);
                    writer.write(mDelimiter);
                }
                writer.newLine();

                for (int k = 0; k < mSensorFormats.length; k++) {
                    writer.write(mSensorFormats[k]);

                    writer.write(mDelimiter);
                }
                writer.newLine();

                for (int k = 0; k < mSensorUnits.length; k++) {
                    if (mSensorUnits[k] == "u8") {
                        writer.write("");
                    } else if (mSensorUnits[k] == "i8") {
                        writer.write("");
                    } else if (mSensorUnits[k] == "u12") {
                        writer.write("");
                    } else if (mSensorUnits[k] == "u16") {
                        writer.write("");
                    } else if (mSensorUnits[k] == "i16") {
                        writer.write("");
                    } else if (mSensorUnits[k] == "no units") {
                        writer.write("");
                    } else {
                        writer.write(mSensorUnits[k]);
                    }
                    writer.write(mDelimiter);
                }
                writer.newLine();
                Log.d("Shimmer", "Data Written");
                mFirstWrite = false;
            }

            //now print data
            for (int r = 0; r < mSensorNames.length; r++) {
                Collection<FormatCluster> dataFormats = objectClusterLog.mPropertyCluster.get(mSensorNames[r]);
                FormatCluster formatCluster = (FormatCluster) returnFormatCluster(dataFormats, mSensorFormats[r], mSensorUnits[r]);  // retrieve the calibrated data
                Log.d("Shimmer", "Data : " + mSensorNames[r] + formatCluster.mData + " " + formatCluster.mUnits);
                writer.write(Double.toString(formatCluster.mData));
                writer.write(mDelimiter);
            }
            writer.newLine();


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d("Shimmer", "Error with bufferedwriter");
        }
    }

    public void closeFile() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public String getName() {
        return mFileName;
    }

    public String getAbsoluteName() {
        return outputFile.getAbsolutePath();
    }

    private boolean compareStringArray(String[] stringArray, String string) {
        boolean uniqueString = true;
        int size = stringArray.length;
        for (int i = 0; i < size; i++) {
            if (stringArray[i] == string) {
                uniqueString = false;
            }

        }
        return uniqueString;
    }

    private FormatCluster returnFormatCluster(Collection<FormatCluster> collectionFormatCluster, String format, String units) {
        Iterator<FormatCluster> iFormatCluster = collectionFormatCluster.iterator();
        FormatCluster formatCluster;
        FormatCluster returnFormatCluster = null;

        while (iFormatCluster.hasNext()) {
            formatCluster = (FormatCluster) iFormatCluster.next();
            if (formatCluster.mFormat == format && formatCluster.mUnits == units) {
                returnFormatCluster = formatCluster;
            }
        }
        return returnFormatCluster;
    }

}


