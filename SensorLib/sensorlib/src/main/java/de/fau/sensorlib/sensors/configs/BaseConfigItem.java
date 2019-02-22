package de.fau.sensorlib.sensors.configs;

public class BaseConfigItem {

    // TODO find better name
    public enum UiType {
        TYPE_DROPDOWN,
        TYPE_MULTI_SELECT,
        TYPE
    }

    protected String mTitle = "";

    public BaseConfigItem() {
        this("");
    }

    public BaseConfigItem(String title) {
        mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

}
