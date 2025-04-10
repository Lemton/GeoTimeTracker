package de.dhbw.geofencinglbs.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;

/**
 * Hilfsmethoden zum Abrufen von Geräteinformationen.
 */
public class DeviceInfoUtil {

    /**
     * Gibt den aktuellen Batteriestand zurück (0-100%).
     */
    public static float getBatteryLevel(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        if (batteryStatus == null) {
            return -1f;
        }

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level == -1 || scale == -1) {
            return -1f;
        }

        return ((float) level / (float) scale) * 100.0f;
    }

    /**
     * Prüft, ob das Gerät gerade geladen wird.
     */
    public static boolean isDeviceCharging(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        if (batteryStatus == null) {
            return false;
        }

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
    }

    /**
     * Gibt den aktuellen Netzwerkverbindungstyp zurück (WIFI, MOBILE, NONE).
     */
    public static String getNetworkConnectionType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return "UNKNOWN";
        }

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null) {
            return "NONE";
        }

        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            return "WIFI";
        } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
            return "MOBILE";
        } else {
            return activeNetwork.getTypeName();
        }
    }
}