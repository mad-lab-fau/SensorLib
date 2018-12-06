/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.fau.sensorlib.R;
import de.fau.sensorlib.SensorEventGenerator;
import de.fau.sensorlib.SensorEventListener;
import de.fau.sensorlib.enums.SensorMessage;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Provides a grid layout that displays information about the sensors. This Widget implements the {@link SensorEventListener},
 * so can subscribe to the {@link SensorEventGenerator}.
 */
public class BatteryBar extends RecyclerView implements SensorEventListener {

    private static final String TAG = BatteryBar.class.getSimpleName();

    private BatteryGridAdapter mAdapter;

    public BatteryBar(Context context) {
        this(context, null);
    }

    public BatteryBar(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public BatteryBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutManager(new GridLayoutManager(getContext(), 2));
        addItemDecoration(new ItemSpacing(getContext()));
        mAdapter = new BatteryGridAdapter(context);
        setAdapter(mAdapter);
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
    public void onSensorMessage(AbstractSensor sensor, SensorMessage message) {
        switch (message) {
            case BATTERY_LEVEL_CHANGED:
                mAdapter.updateBatteryLevel(sensor);
                break;
        }
    }


    private class BatteryGridAdapter extends Adapter<BatteryViewHolder> implements BatteryViewHolder.ItemClickListener {

        private Context mContext;
        private ArrayList<AbstractSensor> mAttachedSensors;

        private BatteryGridAdapter(Context context) {
            mContext = context;
            mAttachedSensors = new ArrayList<>();
        }

        private void updateBatteryLevel(AbstractSensor sensor) {
            int idx = mAttachedSensors.indexOf(sensor);
            if (idx != -1) {
                mAttachedSensors.set(idx, sensor);
                notifyDataSetChanged();
            }
        }

        private void addSensor(AbstractSensor sensor) {
            mAttachedSensors.add(sensor);
            notifyDataSetChanged();
        }

        private void clear() {
            mAttachedSensors.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BatteryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View layout = LayoutInflater.from(mContext).inflate(R.layout.item_battery_bar, parent, false);
            return new BatteryViewHolder(mContext, layout, this);
        }

        @Override
        public void onBindViewHolder(@NonNull BatteryViewHolder holder, int position) {
            holder.setSensorName(mAttachedSensors.get(position).getDeviceName());
            holder.updateBatteryLevel(mAttachedSensors.get(position).getBatteryLevel());
        }


        @Override
        public void onItemClick(View view, int position) {
            BatteryViewHolder viewHolder = (BatteryViewHolder) findViewHolderForAdapterPosition(position);
        }

        @Override
        public int getItemCount() {
            return mAttachedSensors.size();
        }

    }

    private static class BatteryViewHolder extends ViewHolder implements View.OnClickListener {

        private Context mContext;
        private TextView mSensorNameTextView;
        private TextView mBatteryLevelTextView;
        private ItemClickListener mItemClickListener;

        private BatteryViewHolder(Context context, View itemView, ItemClickListener listener) {
            super(itemView);
            mContext = context;
            mSensorNameTextView = itemView.findViewById(R.id.tv_sensor_name);
            mBatteryLevelTextView = itemView.findViewById(R.id.tv_battery_level);
            mItemClickListener = listener;
            itemView.setOnClickListener(this);
        }

        public void setSensorName(String sensorName) {
            mSensorNameTextView.setText(sensorName);
            TextViewCompat.setAutoSizeTextTypeWithDefaults(mSensorNameTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        }

        public void updateBatteryLevel(int batteryLevel) {
            mBatteryLevelTextView.setText(mContext.getString(R.string.placeholder_battery_level, batteryLevel));
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