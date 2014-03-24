package com.tobykurien.batteryfu.compat;

import android.content.Context;

public class Api3 {
   @SuppressWarnings("deprecation")
   public static boolean isAirplaneMode(Context context) {
      return android.provider.Settings.System.getInt(context.getContentResolver(),
               android.provider.Settings.System.AIRPLANE_MODE_ON, 0) == 1;
   }
}
