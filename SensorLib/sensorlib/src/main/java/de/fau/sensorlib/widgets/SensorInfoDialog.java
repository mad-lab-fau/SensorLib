package de.fau.sensorlib.widgets;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import de.fau.sensorlib.Constants;
import de.fau.sensorlib.R;
import de.fau.sensorlib.sensors.AbstractSensor;

public class SensorInfoDialog extends DialogFragment implements View.OnClickListener {

    private static final String TAG = SensorInfoDialog.class.getSimpleName();

    private TextView mSensorNameTextView;
    private TextView mSensorAddressTextView;
    private TextView mManufacturerTextView;
    private TextView mModelNumberTextView;
    private TextView mFirmwareRevisionTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_sensor_info_dialog, container);

        mSensorNameTextView = rootView.findViewById(R.id.tv_sensor_name);
        mSensorAddressTextView = rootView.findViewById(R.id.tv_sensor_address);
        mManufacturerTextView = rootView.findViewById(R.id.tv_sensor_manufacturer);
        mModelNumberTextView = rootView.findViewById(R.id.tv_sensor_model_number);
        mFirmwareRevisionTextView = rootView.findViewById(R.id.tv_sensor_firmware_revision);
        Button okButton = rootView.findViewById(de.fau.sensorlib.R.id.button_ok);
        okButton.setOnClickListener(this);

        String sensorName = "n/a";
        String sensorAddress = "n/a";
        String manufacturer = "n/a";
        String modelNumber = "n/a";
        String firmwareRevision = "n/a";
        if (getArguments() != null) {
            AbstractSensor sensor = (AbstractSensor) getArguments().getSerializable(Constants.KEY_SENSOR);
            if (sensor != null) {
                sensorName = sensor.getDeviceName();
                sensorAddress = sensor.getDeviceAddress();
                manufacturer = sensor.getManufacturer();
                modelNumber = sensor.getModelNumber();
                firmwareRevision = sensor.getFirmwareRevision();
            }
        }

        mSensorNameTextView.setText(Html.fromHtml(getResources().getString(R.string.string_sensor_name, sensorName)));
        mSensorAddressTextView.setText(Html.fromHtml(getResources().getString(R.string.string_sensor_address, sensorAddress)));
        mManufacturerTextView.setText(Html.fromHtml(getResources().getString(R.string.string_manufacturer, manufacturer)));
        mModelNumberTextView.setText(Html.fromHtml(getResources().getString(R.string.string_model_number, modelNumber)));
        mFirmwareRevisionTextView.setText(Html.fromHtml(getResources().getString(R.string.string_firmware_revision, firmwareRevision)));

        return rootView;

    }

    @Override
    public void onStart() {
        super.onStart();
        Objects.requireNonNull(getDialog().getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();

        if (i == R.id.button_ok) {
            dismiss();
        }
    }
}
