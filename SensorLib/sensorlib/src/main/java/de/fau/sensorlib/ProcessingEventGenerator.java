package de.fau.sensorlib;

public interface ProcessingEventGenerator {

    /**
     * This method adds a new ProcessEventListener to the list of listeners.
     *
     * @param listener This is the ProcessEventListener to be added.
     */
    public void addProcessingEventListener(ProcessingEventListener listener);

    /**
     * This method removes a ProcessEventListener from the list of listeners.
     *
     * @param listener This is the ProcessEventListener to be removed.
     */
    public void removeProcessingEventListener(ProcessingEventListener listener);

    /**
     * This method is called when state changes.
     */
    public void notifyListeners();


}
