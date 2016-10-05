package de.fau.sensorlib.sensorpicker;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * {@link RecyclerView} to dynamically add discovered sensors to the sensor list.
 */
public class DsRecyclerView extends RecyclerView {

    public DsRecyclerView(Context context) {
        this(context, null, -1);
    }

    public DsRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public DsRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutManager(new LinearLayoutManager(context));
    }

}
