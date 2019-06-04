/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets.config;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.xeoh.android.checkboxgroup.CheckBoxGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.fau.sensorlib.R;

public class SensorConfigBuilder {

    private static final String TAG = SensorConfigBuilder.class.getSimpleName();

    public interface OnSensorConfigSelectedListener {
        void onConfigItemSelected(String key, Object item);
    }

    public abstract class BaseConfig extends RelativeLayout {

        public BaseConfig(Context context) {
            super(context);
        }

        public abstract void setConfig(String key, ConfigItem configItem, Object defaultConfig);
    }

    public class DropdownConfig extends BaseConfig implements AdapterView.OnItemSelectedListener {

        private String mKey;
        private Spinner mSpinner;
        private ConfigItem mConfigItem;
        private Object mDefaultConfig;

        public DropdownConfig(Context context) {
            super(context);
            inflate(context, R.layout.layout_dropdown_config, this);
            mSpinner = findViewById(R.id.spinner);
            mSpinner.setOnItemSelectedListener(this);
        }

        @Override
        public void setConfig(String key, ConfigItem configItem, Object defaultConfig) {
            mKey = key;
            mConfigItem = configItem;
            mDefaultConfig = defaultConfig;
            mSpinner.setAdapter(new ArrayAdapter<>(getContext(), R.layout.item_dropdown, configItem.getConfigValues()));
            mSpinner.setSelection(configItem.getConfigValues().indexOf(defaultConfig));
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mListener.onConfigItemSelected(mKey, mConfigItem.getConfigValues().get(position));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            mListener.onConfigItemSelected(mKey, mDefaultConfig);
        }
    }

    public class SelectConfig extends BaseConfig implements RadioGroup.OnCheckedChangeListener {

        private String mKey;
        private RadioGroup mRadioGroup;
        private ConfigItem mConfigItem;
        private ArrayList<String> mConfigValuesString = new ArrayList<>();

        public SelectConfig(Context context) {
            super(context);
            inflate(context, R.layout.layout_select_config, this);
            mRadioGroup = findViewById(R.id.radio_group);
            mRadioGroup.setOnCheckedChangeListener(this);
        }

        @Override
        public void setConfig(String key, ConfigItem configItem, Object defaultConfig) {
            mKey = key;
            mConfigItem = configItem;
            for (Object value : mConfigItem.getConfigValues()) {
                RadioButton radioButton = new RadioButton(getContext());
                mConfigValuesString.add(value.toString());
                radioButton.setText(value.toString());
                mRadioGroup.addView(radioButton);
                if (value.equals(defaultConfig)) {
                    radioButton.setChecked(true);
                }
            }
        }

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            Log.e(TAG, "ON CHECKED CHANGED");
            RadioButton radioButton = group.findViewById(checkedId);
            int idx = mConfigValuesString.indexOf(radioButton.getText().toString());
            mListener.onConfigItemSelected(mKey, mConfigItem.getConfigValues().get(idx));
        }
    }

    public class MultiSelectConfig extends BaseConfig {

        private String mKey;
        private LinearLayout mCheckboxContainer;
        private ConfigItem mConfigItem;
        private HashMap<CheckBox, Object> checkBoxMap = new HashMap<>();

        public MultiSelectConfig(Context context) {
            super(context);
            inflate(context, R.layout.layout_multi_select_config, this);
            mCheckboxContainer = findViewById(R.id.checkbox_container);
        }

        @Override
        public void setConfig(String key, ConfigItem configItem, Object defaultConfig) {
            mKey = key;
            List<Object> defaultConfigList = (List<Object>) defaultConfig;
            mConfigItem = configItem;
            for (Object value : mConfigItem.getConfigValues()) {
                CheckBox checkBox = new CheckBox(getContext());
                checkBox.setText(value.toString());
                if (defaultConfigList != null && defaultConfigList.contains(value)) {
                    checkBox.setChecked(true);
                }
                mCheckboxContainer.addView(checkBox);
                checkBoxMap.put(checkBox, value);
            }

            new CheckBoxGroup<>(checkBoxMap, new CheckBoxGroup.CheckedChangeListener<Object>() {
                @Override
                public void onCheckedChange(ArrayList<Object> values) {
                    mListener.onConfigItemSelected(mKey, values);
                }
            });
        }
    }


    private Context mContext;
    private OnSensorConfigSelectedListener mListener;

    public SensorConfigBuilder(Context context) {
        mContext = context;
    }

    public void setOnSensorConfigSelectedListener(OnSensorConfigSelectedListener listener) {
        mListener = listener;
    }


    public BaseConfig buildConfigView(String key, ConfigItem item, Object defaultConfig) {
        BaseConfig config = null;

        switch (item.getType()) {
            case TYPE_DROPDOWN:
                config = new DropdownConfig(mContext);
                config.setConfig(key, item, defaultConfig);
                break;
            case TYPE_SELECT:
                config = new SelectConfig(mContext);
                config.setConfig(key, item, defaultConfig);
                break;
            case TYPE_MULTI_SELECT:
                config = new MultiSelectConfig(mContext);
                config.setConfig(key, item, defaultConfig);
                break;
        }
        return config;
    }

}
