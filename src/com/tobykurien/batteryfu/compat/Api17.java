package com.tobykurien.batteryfu.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class Api17 {
   public static boolean isAirplaneMode(Context context) {
      return android.provider.Settings.Global.getInt(context.getContentResolver(),
               android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
   }
}
