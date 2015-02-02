package com.tobykurien.batteryfu.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;


/**
 * Created by andy on 01/02/15.
 */
@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
public class Api20 {
    public static boolean isScreenOn(Context context)
    {
        PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        if(powerManager.isInteractive())
            return true;
        else
            return false;
    }
}
