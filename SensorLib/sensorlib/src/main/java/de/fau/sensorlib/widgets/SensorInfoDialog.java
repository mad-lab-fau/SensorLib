package de.fau.sensorlib.widgets;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import de.fau.sensorlib.Constants;
import de.fau.sensorlib.R;
import de.fau.sensorlib.sensors.AbstractSensor;

public class SensorInfoDialog extends DialogFragment implements View.OnClickListener {

    private static final String TAG = SensorInfoDialog.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_sensor_info_dialog, container);

        TextView sensorNameTextView = rootView.findViewById(R.id.tv_sensor_name);
        TextView sensorAddressTextView = rootView.findViewById(R.id.tv_sensor_address);
        TextView manufacturerTextView = rootView.findViewById(R.id.tv_sensor_manufacturer);
        TextView modelNumberTextView = rootView.findViewById(R.id.tv_sensor_model_number);
        TextView firmwareRevisionTextView = rootView.findViewById(R.id.tv_sensor_firmware_revision);
        TextView hardwareRevisionTextView = rootView.findViewById(R.id.tv_sensor_hardware_revision);
        Button okButton = rootView.findViewById(de.fau.sensorlib.R.id.button_ok);
        okButton.setOnClickListener(this);

        String sensorName = "n/a";
        String sensorAddress = "n/a";
        String manufacturer = "n/a";
        String modelNumber = "n/a";
        String firmwareRevision = "n/a";
        String hardwareRevision = "n/a";

        if (getArguments() != null) {
            AbstractSensor sensor = (AbstractSensor) getArguments().getSerializable(Constants.KEY_SENSOR);
            if (sensor != null) {
                sensorName = sensor.getDeviceName();
                sensorAddress = sensor.getDeviceAddress();
                manufacturer = sensor.getManufacturerString();
                modelNumber = sensor.getModelNumberString();
                firmwareRevision = sensor.getFirmwareRevisionString();
                hardwareRevision = sensor.getHardwareRevisionString();
            }
        }

        sensorNameTextView.setText(Html.fromHtml(getResources().getString(R.string.string_sensor_name, sensorName)));
        sensorAddressTextView.setText(Html.fromHtml(getResources().getString(R.string.string_sensor_address, sensorAddress)));
        manufacturerTextView.setText(Html.fromHtml(getResources().getString(R.string.string_manufacturer, manufacturer)));
        modelNumberTextView.setText(Html.fromHtml(getResources().getString(R.string.string_model_number, modelNumber)));
        firmwareRevisionTextView.setText(Html.fromHtml(getResources().getString(R.string.string_firmware_revision, firmwareRevision)));
        hardwareRevisionTextView.setText(Html.fromHtml(getResources().getString(R.string.string_hardware_revision, hardwareRevision)));

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();

        if (i == R.id.button_ok) {
            dismiss();
        }
    }
}
