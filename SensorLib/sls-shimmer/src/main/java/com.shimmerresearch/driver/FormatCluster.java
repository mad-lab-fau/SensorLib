package com.shimmerresearch.driver;


public class FormatCluster {
    public String mFormat;
    public String mUnits;
    public double mData;

    public FormatCluster(String format, String units, double data) {
        mFormat = format;
        mUnits = units;
        mData = data;
    }

    public FormatCluster(String format, String units) {
        mFormat = format;
        mUnits = units;
    }


}
