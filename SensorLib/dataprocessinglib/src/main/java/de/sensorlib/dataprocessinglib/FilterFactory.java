package de.sensorlib.dataprocessinglib;

import de.fau.sensorlib.enums.HardwareSensor;
import de.sensorlib.dataprocessinglib.filters.AbstractFilterer;
import de.sensorlib.dataprocessinglib.filters.ECGFilterer;
import de.sensorlib.dataprocessinglib.filters.PPGFilterer;

public class FilterFactory {

    /**
     * This method creates a new Instance of an Abstract filter. The filterer type depends on the given HardwareSensor type.
     * @param samplingRate The samplingRate of te signal.
     * @param hw The Hardwaresensor type with which the signal is recorded.
     * @return A new instance of an Abstract filter.
     */
    public static AbstractFilterer getFilterInstance(int samplingRate, HardwareSensor hw){

        switch(hw){
            case ECG:
                return new ECGFilterer(samplingRate);
            case PPG:
                return new PPGFilterer(samplingRate);
            default:
                return null;
        }

    }

}
