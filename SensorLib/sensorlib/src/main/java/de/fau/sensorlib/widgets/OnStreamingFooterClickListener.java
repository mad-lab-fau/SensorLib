/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 /*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.widgets;

/**
 * Implements callbacks when the Streaming Footer was clicked.
 */
public interface OnStreamingFooterClickListener {

    /**
     * Called then the Floating Action Button (FAB) was clicked.
     *
     * @param fabOpen true if the streaming footer was open when clicked, false otherwise
     */
    void onFabClicked(boolean fabOpen);

    /**
     * Called when the Pause Button was clicked.
     */
    void onStartStopButtonClicked();

    /**
     * Called when the Stop Button was clicked.
     */
    void onDisconnectButtonClicked();
}