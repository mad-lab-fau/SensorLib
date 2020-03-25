/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets.config;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.xeoh.android.checkboxgroup.CheckBoxGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.fau.sensorlib.R;

public class SensorConfigBuilder {

    private static final String TAG = SensorConfigBuilder.class.getSimpleName();

    public interface OnSensorConfigSelectedListener {
        void onConfigItemSelected(String key, Object item);
    }

    public abstract class BaseConfig extends RelativeLayout {

        protected String mKey;
        protected ConfigItem mConfigItem;

        public BaseConfig(Context context, int resId) {
            super(context);
            inflate(context, resId, this);
        }

        public abstract void setConfig(String key, ConfigItem configItem, Object defaultConfig);
    }

    public class DropdownConfig extends BaseConfig implements AdapterView.OnItemSelectedListener {

        private Spinner mSpinner;
        private Object mDefaultConfig;

        public DropdownConfig(Context context) {
            super(context, R.layout.layout_dropdown_config);
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

        private RadioGroup mRadioGroup;
        private ArrayList<String> mConfigValuesString = new ArrayList<>();

        public SelectConfig(Context context) {
            super(context, R.layout.layout_select_config);
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
            RadioButton radioButton = group.findViewById(checkedId);
            int idx = mConfigValuesString.indexOf(radioButton.getText().toString());
            mListener.onConfigItemSelected(mKey, mConfigItem.getConfigValues().get(idx));
        }
    }

    public class MultiSelectConfig extends BaseConfig {

        private LinearLayout mCheckboxContainer;
        private HashMap<CheckBox, Object> checkBoxMap = new HashMap<>();

        public MultiSelectConfig(Context context) {
            super(context, R.layout.layout_multi_select_config);
            mCheckboxContainer = findViewById(R.id.checkbox_container);
        }

        @Override
        public void setConfig(String key, ConfigItem configItem, Object defaultConfig) {
            mKey = key;
            List<?> defaultConfigList = (List<?>) defaultConfig;

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

            new CheckBoxGroup<>(checkBoxMap, values -> mListener.onConfigItemSelected(mKey, values));
        }
    }


    public class TimePickerConfig extends BaseConfig implements View.OnClickListener {

        private TextView mTimeInfoTextView;
        private Calendar mTime;
        private SimpleDateFormat mTimerFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        public TimePickerConfig(Context context) {
            super(context, R.layout.layout_time_picker_config);
            mTimeInfoTextView = findViewById(R.id.tv_time_info);
            Button timePickerButton = findViewById(R.id.button_time_picker);
            timePickerButton.setOnClickListener(this);
        }

        @Override
        public void setConfig(String key, ConfigItem configItem, Object defaultConfig) {
            mKey = key;
            mConfigItem = configItem;
            mTime = ((Calendar) defaultConfig);
            mTime = (Calendar) ((Calendar) defaultConfig).clone();
            mTime.setTimeZone(TimeZone.getDefault());
            updateTextView(mTime);
        }

        private void updateTextView(Calendar calendar) {
            mTimeInfoTextView.setText(mContext.getString(R.string.time_info, mTimerFormat.format(calendar.getTime())));
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.button_time_picker) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    // getter needs to be there, otherwise time zone will not be converted (don't ask why)
                    calendar.get(Calendar.HOUR_OF_DAY);
                    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
                    updateTextView(calendar);
                    mListener.onConfigItemSelected(mKey, calendar);
                }, mTime.get(Calendar.HOUR_OF_DAY), mTime.get(Calendar.MINUTE), true);
                timePickerDialog.show();
            }
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
            case TYPE_TIME:
                config = new TimePickerConfig(mContext);
                config.setConfig(key, item, defaultConfig);
                break;
        }
        return config;
    }

}
