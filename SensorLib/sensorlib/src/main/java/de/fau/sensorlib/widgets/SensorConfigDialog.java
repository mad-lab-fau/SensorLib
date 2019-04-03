/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

import de.fau.sensorlib.Constants;
import de.fau.sensorlib.R;
import de.fau.sensorlib.sensors.configs.ConfigItem;

public class SensorConfigDialog extends DialogFragment implements View.OnClickListener, SensorConfigBuilder.OnSensorConfigSelectedListener {

    private static final String TAG = SensorConfigDialog.class.getSimpleName();

    private Context mContext;

    private RecyclerView mRecyclerView;
    private Button mCancelButton;
    private Button mOkButton;

    private SensorConfigAdapter mAdapter;
    private HashMap<String, Object> mSelectedConfigValues = new HashMap<>();
    private HashMap<String, Object> mDefaultConfigValues = new HashMap<>();
    private HashMap<String, ConfigItem> mConfigItems = new HashMap<>();

    private SensorConfigBuilder mSensorConfigBuilder;
    private SensorConfigListener mSensorConfigListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_sensor_config_dialog, container);
        mContext = getContext();

        mConfigItems = (LinkedHashMap<String, ConfigItem>) getArguments().getSerializable(Constants.KEY_SENSOR_CONFIG);
        mDefaultConfigValues = (LinkedHashMap<String, Object>) getArguments().getSerializable(Constants.KEY_SENSOR_CONFIG_DEFAULT);
        mSelectedConfigValues = (HashMap<String, Object>) mDefaultConfigValues.clone();

        if (mSensorConfigBuilder == null) {
            mSensorConfigBuilder = new SensorConfigBuilder(mContext);
            mSensorConfigBuilder.setOnSensorConfigSelectedListener(this);
        }

        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mAdapter = new SensorConfigAdapter(mContext, mConfigItems);
        mRecyclerView.setAdapter(mAdapter);

        mOkButton = rootView.findViewById(R.id.button_ok);
        mOkButton.setOnClickListener(this);
        mCancelButton = rootView.findViewById(R.id.button_cancel);
        mCancelButton.setOnClickListener(this);

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
        Objects.requireNonNull(getDialog().getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void setSensorConfigBuilder(SensorConfigBuilder builder) {
        mSensorConfigBuilder = builder;
    }

    public void setSensorConfigListener(SensorConfigListener listener) {
        mSensorConfigListener = listener;
    }

    @Override
    public void onConfigItemSelected(String key, Object item) {
        mSelectedConfigValues.put(key, item);
    }


    public static class SensorConfigViewHolder extends RecyclerView.ViewHolder {

        private Context mContext;
        private String mKey;
        private TextView mTitleTextView;
        private FrameLayout mConfigView;

        public SensorConfigViewHolder(Context context, View itemView) {
            super(itemView);
            mContext = context;
            mTitleTextView = itemView.findViewById(R.id.tv_config_title);
            mConfigView = itemView.findViewById(R.id.config_container);
        }

        public void setTitle(String title) {
            mTitleTextView.setText(title);
        }

        public void setConfigLayout(RelativeLayout configLayout) {
            mConfigView.addView(configLayout);
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
            return new SensorConfigViewHolder(mContext, layout);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorConfigViewHolder holder, int position) {
            holder.setKey(mConfigKeys.get(position));
            ConfigItem item = mConfigItems.get(position);
            mSensorConfigBuilder.buildConfigView(holder, item, mDefaultConfigValues.get(mConfigKeys.get(position)));
        }

        @Override
        public int getItemCount() {
            return mConfigItems.size();
        }
    }

}
