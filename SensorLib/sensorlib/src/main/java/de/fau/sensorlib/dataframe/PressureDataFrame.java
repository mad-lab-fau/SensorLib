package de.fau.sensorlib.dataframe;

public interface PressureDataFrame {

    String[] COLUMNS = new String[]{"pressure"};

    double getPressure();
}
