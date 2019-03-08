package de.fau.sensorlib;

public interface ProcessingEventListener {

    /**
     * This method is called by notifyListeners in all Classes that implement ProcessingEventListener.
     *
     * @param generator This is the ProcessingEventGenerator by that the method is called.
     */
    public void update(ProcessingEventGenerator generator);

}
