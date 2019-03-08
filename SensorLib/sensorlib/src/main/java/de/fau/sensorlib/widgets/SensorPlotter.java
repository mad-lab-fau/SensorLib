/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.fau.sensorlib.ProcessingEventGenerator;
import de.fau.sensorlib.ProcessingEventListener;
import de.fau.sensorlib.R;
import de.fau.sensorlib.SensorEventListener;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.SensorMessage;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * A {@link CardView} widget that dynamically plots incoming sensor data.
 */
public class SensorPlotter extends CardView implements SensorEventListener {

    private static final String TAG = SensorPlotter.class.getSimpleName();
    /**
     * Range of displayed data in seconds.
     */
    private int mWindowSize;
    /**
     * Plot refresh rate in Hz.
     */
    private int mRefreshRate;
    private long lastTimestamp = System.currentTimeMillis();

    private long offset = 0;
    private long pauseOffset = 0;
    private long oldPauseOffset = 0;
    private SensorState previousState = SensorState.UNDEFINED;


    private Context mContext;
    private RecyclerView mRecyclerView;
    private SensorPlotterRecyclerAdapter mAdapter;

    private ArrayList<SensorBundle> mSensorBundles = new ArrayList<>();

    private ArrayList<LineData> mLineData = new ArrayList<>();
    private ArrayList<Method[]> mMethodLists = new ArrayList<>();
    private ArrayList<Class<?>> mInterfaceList = new ArrayList<>();


    private boolean mScrollEnabled = true;


    public SensorPlotter(@NonNull Context context) {
        this(context, null, -1);
    }

    public SensorPlotter(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SensorPlotter(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.widget_sensor_plotter, this);
        mContext = context;

        TypedArray attributes = mContext.obtainStyledAttributes(attrs, R.styleable.SensorPlotter);
        if (attributes != null) {
            try {
                mScrollEnabled = attributes.getBoolean(R.styleable.SensorPlotter_scrollable, true);
                mWindowSize = attributes.getInteger(R.styleable.SensorPlotter_windowSize, 10);
                mRefreshRate = attributes.getInteger(R.styleable.SensorPlotter_refreshRate, 25);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                attributes.recycle();
            }
        }

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        SensorPlotterLayoutManager manager = new SensorPlotterLayoutManager(mContext);
        manager.setScrollEnabled(mScrollEnabled);
        mRecyclerView.setLayoutManager(manager);

        mAdapter = new SensorPlotterRecyclerAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }


    public void onNewOnsetPoint(int qrsDelay, int peak, int turn, int through, double onset, double onsetValue) {
        System.out.println("draw onsetPoint: " + qrsDelay);
    }


    public void addSensors(ArrayList<AbstractSensor> sensorList) {
        for (HardwareSensor hwSensor : HardwareSensor.values()) {
            // TODO: change to combine some HardwareSensors to one single plot
            SensorBundle sensorBundle = new SensorBundle(hwSensor);
            for (AbstractSensor sensor : sensorList) {
                if (sensor.getSelectedSensors().contains(hwSensor)) {
                    sensorBundle.addSensor(sensor);
                    sensorBundle.setSamplingRate(sensor.getSamplingRate());
                }
            }
            if (sensorBundle.getSensorList().isEmpty()) {
                continue;
            }
            mAdapter.add(sensorBundle);
            //mAdapter.notifyDataSetChanged();
        }
    }

    public void resetCharts() {
        mSensorBundles = new ArrayList<>(10);
        mLineData = new ArrayList<>(10);
        mMethodLists = new ArrayList<>(10);
        mAdapter.notifyDataSetChanged();
        mRecyclerView.invalidate();
    }


    public void onNewData(SensorDataFrame dataFrame) {
        String sensorId = dataFrame.getOriginatingSensor().getName() + "@" + dataFrame.getOriginatingSensor().getDeviceAddress();

        for (int i = 0; i < mSensorBundles.size(); i++) {
            //long timestamp = (long) ((time - (offset + pauseOffset)) / mSensorBundles.get(i).getSampleDistance());
            ArrayList<String> sensorList = mSensorBundles.get(i).getSensorIds();
            if (sensorList == null) {
                return;
            }
            int sensorIdx = sensorList.indexOf(sensorId);

            if (sensorIdx != -1 && mInterfaceList.get(i).isAssignableFrom(dataFrame.getClass())) {
                Method[] methods = mMethodLists.get(i);
                double[] data = new double[methods.length];
                float x = (float) ((dataFrame.getTimestamp()) * (1000 / dataFrame.getOriginatingSensor().getSamplingRate()));
                for (int j = 0; j < data.length; j++) {
                    try {
                        data[j] = (double) methods[j].invoke(dataFrame);
                        mLineData.get(i).addEntry(new Entry(x, (float) data[j]), sensorIdx * methods.length + j);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }
                //mLineData.get(i).notifyDataChanged();
            }
        }
        if ((System.currentTimeMillis() - lastTimestamp) > 1000 / mRefreshRate) {
            lastTimestamp = System.currentTimeMillis();
            refreshChart();
        }
    }

    private void configureChart(LineChart chart, final SensorBundle bundle) {
        chart.setTouchEnabled(false);
        chart.setDragEnabled(true);
        chart.setHighlightPerTapEnabled(false);
        chart.setScaleYEnabled(false);
        chart.setScaleXEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setGridBackgroundColor(Color.TRANSPARENT);
        chart.setDrawGridBackground(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getXAxis().setDrawAxisLine(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        // 1 second
        chart.getXAxis().setGranularity(1000);
        chart.getXAxis().setLabelCount((int) (mWindowSize * bundle.getSampleDistance()));
        chart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes((int) value),
                        TimeUnit.MILLISECONDS.toSeconds((int) value) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((int) value))
                );
            }
        });
    }

    private synchronized void configureDataSets(LineData data, LineDataSet[] datasets, ArrayList<String> sensorNames, int[] colors, String[] labels) {
        for (int i = 0; i < datasets.length; i++) {
            int color = ContextCompat.getColor(mContext, colors[i % colors.length]);
            if (i / colors.length > 0) {
                color = getColor(color, i / colors.length);
            }
            datasets[i] = new LineDataSet(null, sensorNames.get(i / labels.length) + "_" + labels[i % labels.length]);
            datasets[i].setColor(color);
            datasets[i].setDrawCircles(false);
            datasets[i].setLineWidth(2f);
            data.addDataSet(datasets[i]);
        }
        data.setDrawValues(false);
    }

    private void refreshChart() {
        for (int i = 0; i < mSensorBundles.size(); i++) {
            mAdapter.update(i);
        }
    }

    private void clearCharts() {
        for (int i = 0; i < mSensorBundles.size(); i++) {
            mAdapter.clear(i);
        }
    }

    private int getColor(int color, int i) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        hsl[2] += (hsl[2] * 0.25 * i * Math.pow(-1, i));
        return ColorUtils.HSLToColor(hsl);
    }

    @Override
    public void onSensorStateChange(AbstractSensor sensor, SensorState state) {
        switch (state) {
            case STREAMING:
                /*if (sensor == null) {
                    if (offset == 0) {
                        offset = System.currentTimeMillis();
                    }
                    if (pauseOffset != 0) {
                        if (pauseOffset < 0) {
                            Log.e(TAG, "NEGATIVE 1!!!");
                        }
                        pauseOffset = (System.currentTimeMillis() - pauseOffset) + oldPauseOffset;
                    }
                    for (int i = 0; i < mSensorBundles.size(); i++) {
                        mAdapter.setTouchEnabled(i, true);
                    }
                }*/
                break;
            case CONNECTED:
                if (previousState == SensorState.STREAMING) {
                    oldPauseOffset = pauseOffset;
                    pauseOffset = System.currentTimeMillis();
                    clearCharts();
                } else if (previousState == SensorState.CONNECTING) {
                    resetCharts();
                }
                break;
            case DISCONNECTED:
                offset = 0;
                pauseOffset = 0;
                break;
        }
        previousState = state;
    }

    @Override
    public void onSensorMessage(AbstractSensor sensor, SensorMessage message) {

    }


    private static class SensorPlotterViewHolder extends ViewHolder {

        private LineChart mLineChart;

        private SensorPlotterViewHolder(View itemView) {
            super(itemView);
            mLineChart = itemView.findViewById(R.id.line_chart);
        }
    }


    private class SensorPlotterRecyclerAdapter extends RecyclerView.Adapter<SensorPlotterViewHolder> {

        private SensorPlotterRecyclerAdapter() {
            mSensorBundles = new ArrayList<>(0);
        }

        @NonNull
        @Override
        public SensorPlotterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor_plotter, parent, false);
            if (!mScrollEnabled) {
                mRecyclerView.setHasFixedSize(true);
                int itemHeight = (int) (0.9 * (parent.getHeight() / mSensorBundles.size()));
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                layoutParams.height = itemHeight;
                itemView.setLayoutParams(layoutParams);
            }

            return new SensorPlotterViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorPlotterViewHolder holder, int position) {

            final SensorBundle bundle = mSensorBundles.get(position);
            ArrayList<String> sensorIds = bundle.getSensorIds();
            try {
                final String[] columns = (String[]) bundle.getHwSensor().getDataFrameClass().getDeclaredField("COLUMNS").get("null");

                // chart
                final LineChart lineChart = holder.mLineChart;
                lineChart.getDescription().setText(bundle.getHwSensor().getShortDescription());
                configureChart(lineChart, bundle);

                // data
                LineData lineData = new LineData();
                if (position < mLineData.size()) {
                    mLineData.set(position, lineData);
                } else {
                    mLineData.add(position, lineData);
                }
                lineChart.setData(lineData);

                // data set
                final LineDataSet[] dataSets = new LineDataSet[sensorIds.size() * columns.length];
                final int pos = holder.getLayoutPosition();

                lineChart.post(new Runnable() {
                    @Override
                    public void run() {
                        configureDataSets(mLineData.get(pos), dataSets, bundle.getSensorNames(), PlotColorMap.getColors(bundle.getHwSensor()), columns);
                    }
                });
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return mSensorBundles.size();
        }

        /**
         * Adds element at the specified position and
         * notifies the {@link SensorPlotter.SensorPlotterRecyclerAdapter} that the underlying list has changed.
         *
         * @param position Insert position
         * @param element  Sensor element as {@link Bundle}
         */
        private void addAt(int position, SensorBundle element) {
            if (!mSensorBundles.contains(element)) {
                mSensorBundles.add(position, element);
                mMethodLists.add(element.getHwSensor().getDataFrameClass().getDeclaredMethods());
                mInterfaceList.add(element.getHwSensor().getDataFrameClass());
                mAdapter.bindViewHolder(mAdapter.createViewHolder(mRecyclerView, 10), position);
                notifyItemInserted(position);
                notifyItemRangeChanged(position, mSensorBundles.size() - position - 1);
                notifyDataSetChanged();
            }
        }

        /**
         * Adds element to the end of the list and
         * notifies the {@link SensorPlotter.SensorPlotterRecyclerAdapter} that the underlying list has changed.
         *
         * @param element Sensor element as {@link Bundle}
         */
        private void add(SensorBundle element) {
            addAt(mAdapter.getItemCount(), element);
        }


        /**
         * Removes element at the specified position and
         * notifies the {@link SensorPlotter.SensorPlotterRecyclerAdapter} that the underlying list has changed.
         *
         * @param position Position to remove
         */
        private void removeAt(int position) {
            mSensorBundles.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mSensorBundles.size() - position);
        }

        /**
         * Removes element of the end of the list and
         * notifies the {@link SensorPlotter.SensorPlotterRecyclerAdapter} that the underlying list has changed.
         */
        private void remove() {
            removeAt(mSensorBundles.size() - 1);
        }

        private void update(int position) {
            SensorPlotterViewHolder viewHolder = (SensorPlotterViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null && viewHolder.mLineChart.getData().getEntryCount() != 0) {
                //viewHolder.mLineChart.getData().notifyDataChanged();
                viewHolder.mLineChart.notifyDataSetChanged();
                viewHolder.mLineChart.setAutoScaleMinMaxEnabled(true);
                viewHolder.mLineChart.setVisibleXRange((float) (mSensorBundles.get(position).getSamplingRate() * mSensorBundles.get(position).getSampleDistance() * mWindowSize), (float) (mSensorBundles.get(position).getSamplingRate() * mSensorBundles.get(position).getSampleDistance() * mWindowSize));
                viewHolder.mLineChart.moveViewToX(viewHolder.mLineChart.getData().getXMax());
            }
        }

        private void clear(int position) {
            SensorPlotterViewHolder viewHolder = (SensorPlotterViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null && viewHolder.mLineChart.getData().getEntryCount() != 0) {
                //viewHolder.mLineChart.getData().notifyDataChanged();
                for (ILineDataSet set : viewHolder.mLineChart.getLineData().getDataSets()) {
                    set.clear();
                }
            }
        }


        private void setTouchEnabled(int position, boolean enabled) {
            SensorPlotterViewHolder viewHolder = (SensorPlotterViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                viewHolder.mLineChart.setTouchEnabled(enabled);
            }
        }
    }

    private class SensorPlotterLayoutManager extends LinearLayoutManager {

        private boolean scrollEnabled = true;

        private SensorPlotterLayoutManager(Context context) {
            super(context);
        }

        @Override
        public boolean isAutoMeasureEnabled() {
            return true;
        }

        private void setScrollEnabled(boolean scrollEnabled) {
            this.scrollEnabled = scrollEnabled;
        }

        @Override
        public boolean canScrollVertically() {
            return scrollEnabled && super.canScrollVertically();
        }

    }

    private class SensorBundle {

        private HardwareSensor mHwSensor;
        private ArrayList<SensorInfo> mSensorList = new ArrayList<>();

        /**
         * Sampling rate of sensor in Hz.
         */
        private double mSamplingRate = -1;
        /**
         * Time between two samples in milliseconds
         */
        private double mSampleDistance = 1000 / mSamplingRate;

        private SensorBundle(HardwareSensor hwSensor) {
            mHwSensor = hwSensor;
        }

        private void addSensor(SensorInfo sensor) {
            mSensorList.add(sensor);
        }

        private void setSamplingRate(double samplingRate) {
            mSamplingRate = samplingRate;
            mSampleDistance = 1000 / mSamplingRate;
        }

        private double getSamplingRate() {
            return mSamplingRate;
        }

        private double getSampleDistance() {
            return mSampleDistance;
        }

        public HardwareSensor getHwSensor() {
            return mHwSensor;
        }

        private ArrayList<SensorInfo> getSensorList() {
            return mSensorList;
        }

        public ArrayList<String> getSensorIds() {
            ArrayList<String> sensorIds = new ArrayList<>();
            for (SensorInfo sensor : mSensorList) {
                sensorIds.add(sensor.getName() + "@" + sensor.getDeviceAddress());
            }

            return sensorIds;
        }

        public ArrayList<String> getSensorNames() {
            ArrayList<String> sensorIds = new ArrayList<>();
            for (SensorInfo sensor : mSensorList) {
                sensorIds.add(sensor.getName());
            }
            return sensorIds;
        }
    }
}