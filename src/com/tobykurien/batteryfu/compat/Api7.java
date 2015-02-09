package com.tobykurien.batteryfu.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

/**
 * Created by andy on 01/02/15.
 */
@TargetApi(Build.VERSION_CODES.ECLAIR_MR1)

public class Api7 {
    public static boolean isScreenOn(Context context)
    {
        PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        if(powerManager.isScreenOn())
            return true;
        else
            return false;
    }
}
