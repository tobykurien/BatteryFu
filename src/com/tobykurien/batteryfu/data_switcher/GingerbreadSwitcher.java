package com.tobykurien.batteryfu.data_switcher;

import java.lang.reflect.Method;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

public class GingerbreadSwitcher extends MobileDataSwitcher {
   private static boolean DEBUG = true;

   @Override
   public void enableMobileData(Context context) {
      try {
         ConnectivityManager connService = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
         Method setMobileDataEnabledMethod = getDataMethod(context);
         if (setMobileDataEnabledMethod != null) setMobileDataEnabledMethod.invoke(connService, true);
      } catch (Exception e) {
         Log.e("BatteryFu", "Error enabling data", e);
      }
   }

   @Override
   public void disableMobileData(Context context) {
      try {
         ConnectivityManager connService = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
         Method setMobileDataEnabledMethod = getDataMethod(context);
         if (setMobileDataEnabledMethod != null) setMobileDataEnabledMethod.invoke(connService, false);
      } catch (Exception e) {
         Log.e("BatteryFu", "Error disabling data", e);
      }
   }

   @Override
   public int isToggleWorking(Context context) {
      if (getDataMethod(context) != null) {
         return 0;
      } else {
         // return id of error message
         return com.tobykurien.batteryfu.R.string.apn_problem_text; 
      }
   }

   private Method getDataMethod(Context context) {
      ConnectivityManager connService = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      if (null != connService) {
         Method[] methods = connService.getClass().getMethods();
         for (Method m : methods) {
            if ("setMobileDataEnabled".equals(m.getName())) { return m; }
         }
      }

      return null;
   }
}
