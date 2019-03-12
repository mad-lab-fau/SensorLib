package de.sensorlib.dataprocessinglib;

import de.sensorlib.dataprocessinglib.enums.ProcessingState;

public interface ProcessingEventListener {

    /**
     * This method is called by notifyListeners in all Classes that implement ProcessingEventListener.
     *
     * @param state This is the ProcessingState from which the method is called.
     */

    void onNewProcessingEvent(ProcessingState state);
    //public void onNewProcessingEvent (ProcessingEventGenerator generator);

}
