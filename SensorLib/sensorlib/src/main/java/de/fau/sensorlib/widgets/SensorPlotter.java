package de.fau.sensorlib.widgets;

import android.content.Context;
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
import android.util.Log;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
     * Sampling rate of sensor in Hz.
     */
    private static final int SAMPLING_RATE = 200;
    /**
     * Time between two samples in milliseconds
     */
    private final int SAMPLE_DISTANCE = 1000 / SAMPLING_RATE;
    /**
     * Range of displayed data in seconds.
     */
    private static final int WINDOW_SIZE = 10;
    /**
     * Plot refresh rate in Hz.
     */
    private static final int REFRESH_RATE = 25;
    private long lastTimestamp = System.currentTimeMillis();

    private long offset = 0;
    private long pauseOffset = 0;
    private long oldPauseOffset = 0;
    private SensorState previousState = SensorState.UNDEFINED;


    private Context mContext;
    private RecyclerView mRecyclerView;
    private SensorPlotterRecyclerAdapter mAdapter;

    private ArrayList<SensorInfo> mSelectedSensors = new ArrayList<>();
    private ArrayList<SensorBundle> mHwSensors = new ArrayList<>();

    private ArrayList<LineData> mLineData = new ArrayList<>();
    private ArrayList<Method[]> mMethodLists = new ArrayList<>();


    //TODO: add as XML property
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
        mAdapter = new SensorPlotterRecyclerAdapter();

        mRecyclerView = findViewById(R.id.recycler_view);
        SensorPlotterLayoutManager manager = new SensorPlotterLayoutManager(mContext);
        manager.setScrollEnabled(mScrollEnabled);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setAdapter(mAdapter);
    }


    public void addSensors(ArrayList<SensorInfo> sensorList) {
        mSelectedSensors = sensorList;

        for (HardwareSensor hwSensor : HardwareSensor.values()) {
            SensorBundle sensorBundle = new SensorBundle(hwSensor);
            for (SensorInfo sensor : mSelectedSensors) {
                if (sensor.getDeviceClass().getAvailableSensors().contains(hwSensor)) {
                    sensorBundle.addSensor(sensor);
                }
            }
            if (sensorBundle.getSensorList().isEmpty()) {
                continue;
            }
            mAdapter.add(sensorBundle);
        }
    }

    public void clear() {
        mSelectedSensors = new ArrayList<>();
        mHwSensors = new ArrayList<>();
        mLineData = new ArrayList<>();
        mMethodLists = new ArrayList<>();
        mAdapter.notifyDataSetChanged();
        mRecyclerView.invalidate();
    }


    public void onNewData(SensorDataFrame dataFrame) {
        String sensorId = dataFrame.getOriginatingSensor().getName() + "@" + dataFrame.getOriginatingSensor().getDeviceAddress();
        long timestamp = (System.currentTimeMillis() - (offset + pauseOffset)) / SAMPLE_DISTANCE;
        if (timestamp < 0) {
            Log.e(TAG, "NEGATIVE | offset: " + offset + ", pauseOffset: " + pauseOffset + ", timestamp: " + timestamp);
        }
        for (int i = 0; i < mHwSensors.size(); i++) {
            ArrayList<String> sensorList = mHwSensors.get(i).getSensorIds();
            if (sensorList == null) {
                return;
            }
            int sensorIdx = sensorList.indexOf(sensorId);
            if (sensorIdx != -1) {
                Method[] methods = mMethodLists.get(i);
                double[] data = new double[methods.length];
                for (int j = 0; j < data.length; j++) {
                    try {
                        data[j] = (double) methods[j].invoke(dataFrame);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mLineData.get(i).addEntry(new Entry(timestamp, (float) data[j]), sensorIdx * methods.length + j);
                }
            }
            mLineData.get(i).notifyDataChanged();
        }
        if ((System.currentTimeMillis() - lastTimestamp) > 1000 / REFRESH_RATE) {
            lastTimestamp = System.currentTimeMillis();
            refreshChart();
        }
    }

    private void configureChart(LineChart chart) {
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
        chart.getXAxis().setGranularity(SAMPLING_RATE);
        chart.getXAxis().setLabelCount(WINDOW_SIZE);
        chart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes((int) (value * SAMPLE_DISTANCE)),
                        TimeUnit.MILLISECONDS.toSeconds((int) (value * SAMPLE_DISTANCE)) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((int) value * 5))
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
    }

    private void refreshChart() {
        for (int i = 0; i < mHwSensors.size(); i++) {
            mAdapter.update(i);
        }
    }

    private int getColor(int color, int i) {
        // TODO Color Palette API from Android?
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        hsl[2] += (hsl[2] * 0.25 * i * Math.pow(-1, i));
        return ColorUtils.HSLToColor(hsl);
    }

    @Override
    public void onSensorStateChange(AbstractSensor sensor, SensorState state) {
        switch (state) {
            case STREAMING:
                if (sensor == null) {
                    if (offset == 0) {
                        offset = System.currentTimeMillis();
                    }
                    if (pauseOffset != 0) {
                        if (pauseOffset < 0) {
                            Log.e(TAG, "NEGATIVE 1!!!");
                        }
                        pauseOffset = (System.currentTimeMillis() - pauseOffset) + oldPauseOffset;
                    }
                    for (int i = 0; i < mHwSensors.size(); i++) {
                        mAdapter.setTouchEnabled(i, true);
                    }

                }
                break;
            case CONNECTED:
                if (previousState == SensorState.STREAMING) {
                    oldPauseOffset = pauseOffset;
                    pauseOffset = System.currentTimeMillis();
                } else {
                    clear();
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
            mHwSensors = new ArrayList<>(0);
        }

        @NonNull
        @Override
        public SensorPlotterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor_plotter, parent, false);
            if (!mScrollEnabled) {
                mRecyclerView.setHasFixedSize(true);
                int itemHeight = (int) (0.9 * (parent.getHeight() / mHwSensors.size()));
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                layoutParams.height = itemHeight;
                itemView.setLayoutParams(layoutParams);
            }

            return new SensorPlotterViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorPlotterViewHolder holder, int position) {
            final SensorBundle bundle = mHwSensors.get(position);
            ArrayList<String> sensorIds = bundle.getSensorIds();
            try {
                final String[] columns = (String[]) bundle.getHwSensor().getDataFrameClass().getDeclaredField("COLUMNS").get("null");

                // chart
                final LineChart lineChart = holder.mLineChart;
                lineChart.getDescription().setText(bundle.getHwSensor().getShortDescription());
                configureChart(lineChart);

                // data
                LineData lineData = new LineData();
                mLineData.add(position, lineData);
                lineChart.setData(lineData);

                // data set
                final LineDataSet[] dataSets = new LineDataSet[sensorIds.size() * columns.length];
                final int pos = holder.getAdapterPosition();
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
            return mHwSensors.size();
        }

        /**
         * Adds element at the specified position and
         * notifies the {@link SensorPickerFragment.SensorPickerRecyclerAdapter} that the underlying list has changed.
         *
         * @param position Insert position
         * @param element  Sensor element as {@link Bundle}
         */
        private void addAt(int position, SensorBundle element) {
            if (!mHwSensors.contains(element)) {
                mHwSensors.add(position, element);
                mMethodLists.add(element.getHwSensor().getDataFrameClass().getDeclaredMethods());
                notifyItemInserted(position);
                notifyItemRangeChanged(position, mHwSensors.size() - position - 1);
            }
        }

        /**
         * Adds element to the end of the list and
         * notifies the {@link SensorPickerFragment.SensorPickerRecyclerAdapter} that the underlying list has changed.
         *
         * @param element Sensor element as {@link Bundle}
         */
        private void add(SensorBundle element) {
            addAt(mHwSensors.size(), element);
        }


        /**
         * Removes element at the specified position and
         * notifies the {@link SensorPickerFragment.SensorPickerRecyclerAdapter} that the underlying list has changed.
         *
         * @param position Position to remove
         */
        private void removeAt(int position) {
            mHwSensors.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mHwSensors.size() - position);
        }

        /**
         * Removes element of the end of the list and
         * notifies the {@link SensorPickerFragment.SensorPickerRecyclerAdapter} that the underlying list has changed.
         */
        private void remove() {
            removeAt(mHwSensors.size() - 1);
        }

        private void update(int position) {
            SensorPlotterViewHolder viewHolder = (SensorPlotterViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                viewHolder.mLineChart.getData().notifyDataChanged();
                viewHolder.mLineChart.notifyDataSetChanged();
                viewHolder.mLineChart.setVisibleXRange(SAMPLING_RATE * WINDOW_SIZE, SAMPLING_RATE * WINDOW_SIZE);
                viewHolder.mLineChart.moveViewToX(viewHolder.mLineChart.getData().getEntryCount());
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

        private SensorBundle(HardwareSensor hwSensor) {
            mHwSensor = hwSensor;
        }

        private void addSensor(SensorInfo sensor) {
            mSensorList.add(sensor);
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
