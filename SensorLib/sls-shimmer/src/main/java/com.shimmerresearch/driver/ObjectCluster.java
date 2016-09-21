package com.shimmerresearch.driver;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Iterator;

public class ObjectCluster {
    public Multimap<String, FormatCluster> mPropertyCluster = HashMultimap.create();
    public String mMyName;
    public String mBluetoothAddress;

    public ObjectCluster(String myName) {
        mMyName = myName;
    }

    public ObjectCluster(String myName, String myBlueAdd) {
        mMyName = myName;
        mBluetoothAddress = myBlueAdd;
    }

    /**
     * Takes in a collection of Format Clusters and returns the Format Cluster specified by the string format
     *
     * @param collectionFormatCluster
     * @param format
     * @return FormatCluster
     */
    public static FormatCluster returnFormatCluster(Collection<FormatCluster> collectionFormatCluster, String format) {
        Iterator<FormatCluster> iFormatCluster = collectionFormatCluster.iterator();
        FormatCluster formatCluster;
        FormatCluster returnFormatCluster = null;

        while (iFormatCluster.hasNext()) {
            formatCluster = (FormatCluster) iFormatCluster.next();
            if (formatCluster.mFormat.equals(format)) {
                returnFormatCluster = formatCluster;
            }
        }
        return returnFormatCluster;
    }


}
