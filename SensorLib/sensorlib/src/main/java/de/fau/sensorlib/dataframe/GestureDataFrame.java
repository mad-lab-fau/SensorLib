/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Gesture data frame.
 */
public interface GestureDataFrame {

    String[] COLUMNS = new String[]{"gesture"};

    /**
     * Enumeration listing possible gestures from the MYO armband.
     */
    enum Gesture {
        UNKNOWN,
        REST,
        DOUBLE_TAP,
        FIST,
        WAVE_IN,
        WAVE_OUT,
        FINGERS_SPREAD
    }

    /**
     * Returns the current gesture.
     *
     * @return current gesture
     */
    Gesture getGesture();
}
