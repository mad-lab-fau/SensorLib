package de.sensorlib.dataprocessinglib.filters;

/**
 * This class creates an abstract Filter for different types of data
 */

public abstract class AbstractFilterer {

    private int mSamplingRate;

    public AbstractFilterer(){
        this(-1);
    }

    public AbstractFilterer(int samplingRate){
        this.mSamplingRate = samplingRate;
    }

    /**
     * this method filters the data samplewise
     * @param datum A datasample.
     * @return The filtered datasample.
     */
    public abstract int filter(int datum);

    /**
     * This method computes the first derivative.
     * @param datum A datasample.
     * @return The first derivative of the datasample.
     */
    public abstract int deriv1(int datum);

}
