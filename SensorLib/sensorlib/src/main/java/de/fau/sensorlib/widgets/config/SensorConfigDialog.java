/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets.config;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import de.fau.sensorlib.Constants;
import de.fau.sensorlib.R;

public class SensorConfigDialog extends DialogFragment implements View.OnClickListener, SensorConfigBuilder.OnSensorConfigSelectedListener {

    private static final String TAG = SensorConfigDialog.class.getSimpleName();

    private HashMap<String, Object> mSelectedConfigValues = new HashMap<>();
    private HashMap<String, Object> mDefaultConfigValues = new HashMap<>();
    private HashMap<String, ConfigItem> mConfigItems = new HashMap<>();

    private SensorConfigBuilder mSensorConfigBuilder;
    private OnSensorConfigChangedListener mSensorConfigListener;

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_sensor_config_dialog, container);
        Context context = getContext();

        Bundle args = getArguments();
        if (args == null) {
            return rootView;
        }

        if (args.containsKey(Constants.KEY_SENSOR_CONFIG)) {
            mConfigItems = (LinkedHashMap<String, ConfigItem>) args.getSerializable(Constants.KEY_SENSOR_CONFIG);
        }
        if (args.containsKey(Constants.KEY_SENSOR_CONFIG_DEFAULT)) {
            mDefaultConfigValues = (LinkedHashMap<String, Object>) getArguments().getSerializable(Constants.KEY_SENSOR_CONFIG_DEFAULT);
            mSelectedConfigValues = (HashMap<String, Object>) (mDefaultConfigValues != null ? mDefaultConfigValues.clone() : null);
        }

        String sensorName = getArguments().getString(Constants.KEY_SENSOR_NAME, "n/a");
        TextView textView = rootView.findViewById(R.id.tv_header);
        textView.setText(getString(R.string.sensor_config, sensorName));

        if (mSensorConfigBuilder == null) {
            mSensorConfigBuilder = new SensorConfigBuilder(context);
            mSensorConfigBuilder.setOnSensorConfigSelectedListener(this);
        }

        RecyclerView recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        SensorConfigAdapter adapter = new SensorConfigAdapter(context, mConfigItems);
        recyclerView.setAdapter(adapter);

        Button okButton = rootView.findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);
        Button cancelButton = rootView.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_ok) {
            if (mSensorConfigListener != null) {
                mSensorConfigListener.onSensorConfigSelected(mSelectedConfigValues);
            }
            dismiss();
        } else if (id == R.id.button_cancel) {
            dismiss();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    public void setSensorConfigBuilder(SensorConfigBuilder builder) {
        mSensorConfigBuilder = builder;
    }

    public void setSensorConfigListener(OnSensorConfigChangedListener listener) {
        mSensorConfigListener = listener;
    }

    @Override
    public void onConfigItemSelected(String key, Object item) {
        mSelectedConfigValues.put(key, item);
    }


    public static class SensorConfigViewHolder extends RecyclerView.ViewHolder {

        private String mKey;
        private TextView mTitleTextView;
        private FrameLayout mConfigView;

        public SensorConfigViewHolder(View itemView) {
            super(itemView);
            mTitleTextView = itemView.findViewById(R.id.tv_config_title);
            mConfigView = itemView.findViewById(R.id.config_container);
        }

        public void setTitle(String title) {
            mTitleTextView.setText(title);
        }

        public void setConfigLayout(SensorConfigBuilder.BaseConfig configItem) {
            mConfigView.removeAllViews();
            mConfigView.addView(configItem);
        }

        public void setKey(String key) {
            mKey = key;
        }

        public String getKey() {
            return mKey;
        }
    }


    public class SensorConfigAdapter extends RecyclerView.Adapter<SensorConfigViewHolder> {

        private Context mContext;
        private ArrayList<String> mConfigKeys;
        private ArrayList<ConfigItem> mConfigItems;

        private SensorConfigAdapter(Context context, HashMap<String, ConfigItem> configItems) {
            mContext = context;
            mConfigKeys = new ArrayList<>(configItems.keySet());
            mConfigItems = new ArrayList<>(configItems.values());
        }

        @NonNull
        @Override
        public SensorConfigViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View layout = LayoutInflater.from(mContext).inflate(R.layout.item_sensor_config, parent, false);
            return new SensorConfigViewHolder(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorConfigViewHolder holder, int position) {
            holder.setKey(mConfigKeys.get(position));
            ConfigItem item = mConfigItems.get(position);
            holder.setTitle(item.getTitle());
            holder.setConfigLayout(mSensorConfigBuilder.buildConfigView(holder.getKey(), item, mDefaultConfigValues.get(mConfigKeys.get(position))));
        }

        @Override
        public int getItemCount() {
            return mConfigItems.size();
        }
    }

}
