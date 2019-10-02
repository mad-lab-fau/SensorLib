/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets;

import de.fau.sensorlib.R;

public class BatteryIconHelper {

    public static int getIconForBatteryLevel(int batteryLevel, boolean isCharging) {
        int level = batteryLevel / 10;
        if (isCharging) {
            switch (level) {
                case 0:
                case 1:
                    return R.drawable.ic_battery_charging_20_black_24dp;
                case 2:
                    return R.drawable.ic_battery_charging_20_black_24dp;
                case 3:
                case 4:
                    return R.drawable.ic_battery_charging_30_black_24dp;
                case 5:
                    return R.drawable.ic_battery_charging_50_black_24dp;
                case 6:
                case 7:
                    return R.drawable.ic_battery_charging_60_black_24dp;
                case 8:
                    return R.drawable.ic_battery_charging_80_black_24dp;
                case 9:
                    return R.drawable.ic_battery_charging_90_black_24dp;
                default:
                    return R.drawable.ic_battery_charging_full_black_24dp;
            }
        } else {
            switch (level) {
                case 0:
                case 1:
                    return R.drawable.ic_battery_alert_black_24dp;
                case 2:
                    return R.drawable.ic_battery_20_black_24dp;
                case 3:
                case 4:
                    return R.drawable.ic_battery_30_black_24dp;
                case 5:
                    return R.drawable.ic_battery_50_black_24dp;
                case 6:
                case 7:
                    return R.drawable.ic_battery_60_black_24dp;
                case 8:
                    return R.drawable.ic_battery_80_black_24dp;
                case 9:
                    return R.drawable.ic_battery_90_black_24dp;
                default:
                    return R.drawable.ic_battery_full_black_24dp;

            }
        }
    }
}
