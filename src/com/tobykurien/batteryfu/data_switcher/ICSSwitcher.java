package com.tobykurien.batteryfu.data_switcher;

import java.lang.reflect.Method;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import com.tobykurien.batteryfu.BatteryFu;
import com.tobykurien.batteryfu.R;

/**
 * An ICS-compatible data switcher
 * @author toby
 */
public class ICSSwitcher extends MobileDataSwitcher {
   private ConnectivityManager connMan;
   private Context context;
   private Method isEnabledMethod;
   private Method setEnabledMethod;

   @Override
   public void enableMobileData(Context context) {
      setMobileDataEnabled(context, true);
   }

   @Override
   public void disableMobileData(Context context) {
      setMobileDataEnabled(context, false);
   }

   @Override
   public int isToggleWorking(Context context) {
      init(context);
      
      if (setEnabledMethod != null) {
         return 0;
      }
      
      return R.string.apn_problem_text;
   }

   public void init(Context context) {
      this.context = context;
      this.connMan = ((ConnectivityManager) this.context.getSystemService("connectivity"));
      try {
         Class localClass = this.connMan.getClass();
         Class[] arrayOfClass = new Class[1];
         arrayOfClass[0] = Boolean.TYPE;
         this.setEnabledMethod = localClass.getMethod("setMobileDataEnabled", arrayOfClass);
         this.isEnabledMethod = this.connMan.getClass().getMethod("getMobileDataEnabled", new Class[0]);
      } catch (Exception localException) {
         Log.e(BatteryFu.LOG_TAG, localException.getMessage(), localException);
      }
   }

   public void setMobileDataEnabled(Context context, boolean enabled) {
      init(context);
      try {
         Method localMethod = this.setEnabledMethod;
         ConnectivityManager localConnectivityManager = this.connMan;
         Object[] arrayOfObject = new Object[1];
         arrayOfObject[0] = Boolean.valueOf(enabled);
         localMethod.invoke(localConnectivityManager, arrayOfObject);
      } catch (Exception localException) {
         Log.e(BatteryFu.LOG_TAG, localException.getMessage(), localException);
      }
   }

   public boolean isMobileDataEnabled() throws Exception {
      return ((Boolean) this.isEnabledMethod.invoke(this.connMan, new Object[0])).booleanValue();
   }
}
