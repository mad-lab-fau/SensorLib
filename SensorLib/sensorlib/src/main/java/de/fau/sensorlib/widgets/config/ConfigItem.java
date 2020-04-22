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

    private static final String TAG = ConfigItem.class.getSimpleName();

    public enum UiType {
        TYPE_SELECT,
        TYPE_MULTI_SELECT,
        TYPE_DROPDOWN,
        TYPE_TIME,
        TYPE_UNKNOWN
    }


    private UiType mType;

    private String mTitle;

    private ArrayList<Object> mConfigValues;


    public ConfigItem() {
        this("", null, UiType.TYPE_UNKNOWN);
    }

    public ConfigItem(String title, ArrayList<Object> values, UiType type) {
        mTitle = title;
        mConfigValues = values;
        mType = type;
    }

    public String getTitle() {
        return mTitle;
    }

    public UiType getType() {
        return mType;
    }

    public ArrayList<Object> getConfigValues() {
        return mConfigValues;
    }

}
