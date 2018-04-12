package de.fau.sensorlib.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import de.fau.sensorlib.R;
import de.fau.sensorlib.SensorFoundCallback;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.enums.KnownSensor;

/**
 * {@link android.support.v4.app.DialogFragment} that lists files for simulation.
 */
public class SimulationPickerFragment extends DialogFragment implements View.OnClickListener {

    private static final String TAG = SensorPickerFragment.class.getSimpleName();

    private Context mContext;

    private Button mCancelButton;
    private Button mOkButton;
    private Spinner mFileSpinner;
    private File mSelectedFile;

    private SensorFoundCallback mSensorFoundCallback;
    private String mDirectory;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_simulation_picker, container);

        mContext = rootView.getContext();

        getDialog().setTitle("SimulationPicker");

        setCancelable(false);
        mFileSpinner = rootView.findViewById(R.id.spinner_sim_dialog);
        mCancelButton = rootView.findViewById(R.id.button_cancel);
        mCancelButton.setOnClickListener(this);
        mOkButton = rootView.findViewById(R.id.button_ok);
        mOkButton.setOnClickListener(this);

        return rootView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listSimulationFiles();
    }


    @Override
    public void onStart() {
        super.onStart();
        Objects.requireNonNull(getDialog().getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Activity activity = getActivity();
        if (activity instanceof DialogInterface.OnDismissListener) {
            ((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
        }
    }

    public void setSensorFoundCallback(SensorFoundCallback callback) {
        mSensorFoundCallback = callback;
    }

    private void listSimulationFiles() {
        final List<File> fileList = readSimulationFiles(mDirectory);

        final List<String> fileNameList = new ArrayList<>();
        fileNameList.add("Select file:");
        for (File f : fileList) {
            if (f != null) {
                fileNameList.add(f.getName());
            }
        }

        // initialize ECG adapter to fill the Spinner
        final ArrayAdapter<String> fileAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_spinner_dropdown_item, fileNameList);
        mFileSpinner.setAdapter(fileAdapter);
        mFileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // first position = "Select file:", so no further action for clicking on it
                if (position == 0) {
                    return;
                }
                mSelectedFile = fileList.get(position);
                Log.i(TAG, "Selected Simulation File: " + mSelectedFile.getName());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * Reads the file names of the simulation files from the external storage
     */
    private List<File> readSimulationFiles(String directoryName) {
        List<File> fileList = new ArrayList<>();
        // get the current state of the external storage
        String directoryState = Environment.getExternalStorageState();

        // media readable
        if (directoryState.equals(Environment.MEDIA_MOUNTED_READ_ONLY) ||
                directoryState.equals(Environment.MEDIA_MOUNTED)) {
            File root = Environment.getExternalStorageDirectory();

            /*if (mMitBih) {
                // MIT-BIH database
                File path = new File(root, "MITDatabaseRecordings");
                // Read all MIT-BIH recordings and add them to the list
                if (path.exists()) {
                    // get all files of this folder
                    fileList = new ArrayList<>(Arrays.asList(path.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String s) {
                            return s.contains("mit") && s.contains("sig");
                        }
                    })));
                    // add this to the beginning of the list so that it's displayed
                    // as first option in the data spinner
                    fileList.add(0, null);
                } else if (path.mkdir()) {
                    // if path does not exist and was successfully created, read files again
                    readSimulationFiles();
                }
            } else {*/
            // DailyHeart recordings
            File path = new File(root, directoryName);
            // Read all DailyHeart recordings and add them to the list
            if (path.exists()) {
                // add only files that are DailyHeart recordings
                fileList = new ArrayList<>(Arrays.asList(path.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        //Log.d(TAG, s);
                        //return s.contains("dailyheart") && s.contains(".csv");
                        // TODO just for testing
                        return s.contains("Simblee");
                    }
                })));
                // add this to the beginning of the list so that it's displayed
                // as first option in the data spinner
                fileList.add(0, null);
            } else if (path.mkdir()) {
                // if path does not exist and was successfully created, read files again
                readSimulationFiles(directoryName);
            }
        }
        /*}*/
        return fileList;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.button_cancel) {
            dismiss();
        } else if (i == R.id.button_ok) {
            if (mSelectedFile != null) {
                SensorInfo sensorInfo = new SensorInfo("SimbleeSimulator", mSelectedFile.getAbsolutePath(), KnownSensor.GENERIC_SIMULATED);
                mSensorFoundCallback.onKnownSensorFound(sensorInfo);
            }
            dismiss();
        }
    }

    public void show(Activity activity) {
        if (activity instanceof AppCompatActivity) {
            super.show((((AppCompatActivity) activity).getSupportFragmentManager()), "simulation_picker");
        }
    }

    public void setDirectory(String directory) {
        mDirectory = directory;
    }
}
