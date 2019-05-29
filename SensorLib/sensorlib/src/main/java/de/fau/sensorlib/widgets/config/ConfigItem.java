/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets.config;

import java.io.Serializable;
import java.util.ArrayList;

public class ConfigItem implements Serializable {

    // TODO find better name
    public enum UiType {
        TYPE_SELECT,
        TYPE_MULTI_SELECT,
        TYPE_DROPDOWN,
        TYPE_UNKNOWN
    }

    private static final String TAG = ConfigItem.class.getSimpleName();

    protected UiType mType;

    protected String mTitle;

    protected ArrayList<Object> mConfigValues;

    //protected Object mDefaultConfigValue;


    public ConfigItem() {
        this("", null, UiType.TYPE_UNKNOWN);
    }

    public ConfigItem(String title, ArrayList<Object> values, UiType type) {
        mTitle = title;
        mConfigValues = values;
        mType = type;
    }

    /*public void setDefaultConfigValue(Object defaultConfigValue) {
        if (mConfigValues.contains(defaultConfigValue)) {
            mDefaultConfigValue = defaultConfigValue;
        } else {
            Log.e(TAG, "Default Config value not in set of possible config values!");
        }
    }*/

    public String getTitle() {
        return mTitle;
    }

    public UiType getType() {
        return mType;
    }

    public ArrayList<Object> getConfigValues() {
        return mConfigValues;
    }

    /*public Object getDefaultConfigValue() {
        return mDefaultConfigValue;
    }*/


    /*public void setSelectedConfigValue(Object selectedConfigValue) {
        if (mConfigValues.contains(selectedConfigValue)) {
            mSelectedConfigValue = selectedConfigValue;
            Log.d(TAG, "item selected: " + mSelectedConfigValue);
        } else {
            Log.e(TAG, "Selected Config value not in set of possible config values!");
        }
    }*/

}
