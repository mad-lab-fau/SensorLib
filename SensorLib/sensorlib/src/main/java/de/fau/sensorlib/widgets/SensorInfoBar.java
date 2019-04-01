/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.widgets;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import de.fau.sensorlib.Constants;
import de.fau.sensorlib.R;
import de.fau.sensorlib.SensorEventGenerator;
import de.fau.sensorlib.SensorEventListener;
import de.fau.sensorlib.enums.SensorMessage;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Provides a grid layout that displays sensor information. This Widget implements the
 * {@link SensorEventListener}, so it can subscribe to the {@link SensorEventGenerator}.
 */
public class SensorInfoBar extends RecyclerView implements SensorEventListener {

    private static final String TAG = SensorInfoBar.class.getSimpleName();

    private SensorInfoGridAdapter mAdapter;

    public SensorInfoBar(Context context) {
        this(context, null);
    }

    public SensorInfoBar(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SensorInfoBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutManager(new GridLayoutManager(getContext(), 2));
        addItemDecoration(new ItemSpacing(getContext()));
        setBackgroundColor(getResources().getColor(R.color.transparent));
        mAdapter = new SensorInfoGridAdapter(context);
        setAdapter(mAdapter);
    }

    public void setSensorActionBarEnabled(boolean enable) {
        mAdapter.setSensorActionBarEnabled(enable);
    }

    private AppCompatActivity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) {
                return (AppCompatActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Override
    public void onSensorStateChange(AbstractSensor sensor, SensorState state) {
        switch (state) {
            case CONNECTED:
                if (sensor != null) {
                    mAdapter.addSensor(sensor);
                }
                break;
            case CONNECTION_LOST:
                // fall through
            case DISCONNECTED:
                mAdapter.clear();
                break;
        }
    }

    @Override
    public void onSensorMessage(AbstractSensor sensor, SensorMessage messageType, String message) {
        switch (messageType) {
            case OPERATION_STATE_CHANGED:
                mAdapter.updateAdditionalInfo(sensor, message);
            case BATTERY_LEVEL_CHANGED:
                mAdapter.updateSensor(sensor);
                break;
        }
    }


    private class SensorInfoGridAdapter extends Adapter<SensorInfoViewHolder> implements SensorInfoViewHolder.ItemClickListener {

        private Context mContext;
        private ArrayList<AbstractSensor> mAttachedSensors;
        private ArrayList<String> mAdditionalInfos;

        private boolean mSensorActionBarEnabled;

        private SensorInfoGridAdapter(Context context) {
            mContext = context;
            mAttachedSensors = new ArrayList<>();
            mAdditionalInfos = new ArrayList<>();
        }

        private void updateSensor(AbstractSensor sensor) {
            int idx = mAttachedSensors.indexOf(sensor);
            if (idx != -1) {
                mAttachedSensors.set(idx, sensor);
                notifyDataSetChanged();
            }
        }

        private void updateAdditionalInfo(AbstractSensor sensor, String message) {
            int position = mAttachedSensors.indexOf(sensor);
            if (position != -1) {
                mAdditionalInfos.set(position, message);
                notifyDataSetChanged();
            }
        }

        private void addSensor(AbstractSensor sensor) {
            if (!mAttachedSensors.contains(sensor)) {
                mAttachedSensors.add(sensor);
                mAdditionalInfos.add("");
                notifyDataSetChanged();
            }
        }

        private void clear() {
            mAttachedSensors.clear();
            mAdditionalInfos.clear();
            notifyDataSetChanged();
        }

        private void setSensorActionBarEnabled(boolean enable) {
            mSensorActionBarEnabled = enable;
        }

        @NonNull
        @Override
        public SensorInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View layout = LayoutInflater.from(mContext).inflate(R.layout.item_sensor_info_bar, parent, false);
            return new SensorInfoViewHolder(mContext, layout, this);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorInfoViewHolder holder, int position) {
            holder.setSensorName(mAttachedSensors.get(position).getDeviceName());
            holder.updateBatteryLevel(mAttachedSensors.get(position).getBatteryLevel());
            holder.updateAdditionalInfo(mAdditionalInfos.get(position));
        }

        @Override
        public void onItemClick(View view, int position) {
            if (!mSensorActionBarEnabled) {
                return;
            }
            
            AbstractSensor sensor = mAttachedSensors.get(position);

            Bundle bundle = new Bundle();
            bundle.putSerializable(Constants.KEY_SENSOR, sensor);

            SensorActionDialog dialog = new SensorActionDialog();
            dialog.setArguments(bundle);

            AppCompatActivity activity = getActivity();
            if (activity != null) {
                FragmentManager fm = activity.getSupportFragmentManager();
                if (fm != null) {
                    dialog.show(fm, "sensor_action");
                }
            }
        }

        @Override
        public int getItemCount() {
            return mAttachedSensors.size();
        }

    }

    private static class SensorInfoViewHolder extends ViewHolder implements View.OnClickListener {

        private Context mContext;
        private TextView mSensorNameTextView;
        private TextView mBatteryLevelTextView;
        private TextView mAdditionalInfoTextView;
        private ItemClickListener mItemClickListener;

        private SensorInfoViewHolder(Context context, View itemView, ItemClickListener listener) {
            super(itemView);
            mContext = context;
            mSensorNameTextView = itemView.findViewById(R.id.tv_sensor_name);
            mBatteryLevelTextView = itemView.findViewById(R.id.tv_battery_level);
            mAdditionalInfoTextView = itemView.findViewById(R.id.tv_additional_info);
            mItemClickListener = listener;
            itemView.setOnClickListener(this);
        }

        public void setSensorName(String sensorName) {
            mSensorNameTextView.setText(sensorName);
            TextViewCompat.setAutoSizeTextTypeUniformWithPresetSizes(mSensorNameTextView, new int[]{8, 10, 12}, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        }

        public void updateBatteryLevel(int batteryLevel) {
            mBatteryLevelTextView.setText(mContext.getString(R.string.placeholder_battery_level, batteryLevel));
        }

        public void updateAdditionalInfo(String message) {
            mAdditionalInfoTextView.setText(message);
            TextViewCompat.setAutoSizeTextTypeUniformWithPresetSizes(mAdditionalInfoTextView, new int[]{8, 10, 12}, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getAdapterPosition());
        }

        interface ItemClickListener {
            void onItemClick(View view, int position);
        }
    }

    private static class ItemSpacing extends ItemDecoration {

        private int spacing;

        private ItemSpacing(Context context) {
            spacing = context.getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            outRect.set(spacing, spacing, spacing, spacing);
        }
    }
}