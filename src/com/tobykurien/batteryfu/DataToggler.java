package com.tobykurien.batteryfu;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;
import com.tobykurien.android.Utils;
import com.tobykurien.batteryfu.data_switcher.MobileDataSwitcher;

public class DataToggler extends BroadcastReceiver {
   public static final int NOTIFICATION_CONNECTIVITY = 1;

   @Override
   public void onReceive(Context context, Intent intent) {
      Log.d("BatteryFu", "DataToggler received broadcast");

      Settings settings = Settings.getSettings(context);

      // Check the screen service, in case it was killed
      try {
         if (settings.isDataWhileScreenOn() || settings.isScreenOnKeepData()) {
            // start the screen service if it was killed
            ensureScreenService(context);
         }
      } catch (Exception e) {
         Log.d("BatteryFu", "Unable to bind to screen service", e);
      }

      // Main BatteryFu functionality - turn data on when woken up by system
      try {
         if (handleWidgetBroadcasts(context, intent, settings)) {
            return;
         }

         if ("data://wake".equals(intent.getDataString()) ||
             "data://on".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Data enable");
            settings.setLastWakeTime(System.currentTimeMillis());

            // Check for airplane mode
            boolean isAirplaneMode = android.provider.Settings.System.getInt(context.getContentResolver(),
                     android.provider.Settings.System.AIRPLANE_MODE_ON,
                     0) == 1;

            enableData(context, true);
            if (!isAirplaneMode) {
               // keep the notification running
               MainFunctions.showNotification(context, settings, context.getString(R.string.data_enabled_waiting_for_connection));
            } else {
               // keep the notification running
               MainFunctions.showNotification(context, settings, context.getString(R.string.airplane_mode_is_on));
            }
         } else if ("data://sleep".equals(intent.getDataString()) || 
                    "data://sleep_once".equals(intent.getDataString()) ||
                    "data://off".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Data disable");
            if (disableData(context, false)) {
               MainFunctions.showNotificationWaitingForSync(context, settings);
            }
         } else if ("nightmode://on".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Night mode enable");
            if (!settings.isNightmode()) {
               if (settings.isDataWhileScreenOn() && ScreenService.isScreenOn(context)) {
                  MainFunctions.showNotification(context, settings, context.getString(R.string.data_while_screen_on_night_mode_cancelled));
               } else {
                  nightModeOn(context, settings);
               }
            }
         } else if ("nightmode://force".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Night mode force enable");
            nightModeOn(context, settings);
         } else if ("nightmode://off".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Night mode disable");
            if (settings.isNightmode()) {
               settings.setIsNightmode(false);
               MainFunctions.showNotification(context, settings, context.getString(R.string.night_mode_ended_starting_sync));
               AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
               MainFunctions.setupDataAlarms(context, am, true);
            }
         } else if ("standardmode://on".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Standard mode enable");
            MainFunctions.startScheduler(context, false);
         } else if ("travelmode://on".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Travel mode enable");
            if (!settings.isTravelMode()) {
               // make sure scheduler is started
               MainFunctions.startScheduler(context, false);
               if (settings.isWifiEnabled()) {
                  // disable wifi for travel mode
                  WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                  wm.disconnect();
                  wm.setWifiEnabled(false);

                  settings.setIsTravelMode(true);
                  MainFunctions.showNotification(context, settings, context.getString(R.string.wifi_disabled_travel_mode_activated));
               } else {
                  MainFunctions.showNotification(context, settings, context.getString(R.string.wifi_toggling_not_enabled_standard_mode_activated));
               }
            }
         } else if ("offlinemode://on".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Offline mode enable");
            MainFunctions.teardownDataAlarms(context, null);
            if (disableData(context, true)) {
               MainFunctions.showNotification(context, settings, context.getString(R.string.data_disabled_offline_mode_activated));
            }
         } else if ("onlinemode://on".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Online mode enable");
            MainFunctions.teardownDataAlarms(context, null);
            enableData(context, false);
            MainFunctions.showNotification(context, settings, context.getString(R.string.data_enabled_online_mode_activated));
         } else if ("sync://on".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Performing sync");
            enableData(context, true);
            MainFunctions.showNotification(context, settings, context.getString(R.string.running_account_sync));
         }
      } catch (Exception e) {
         Log.e("BatteryFu", "Error in DataToggler", e);
         Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
      }
   }

   private void nightModeOn(Context context, Settings settings) {
      settings.setIsNightmode(true);
      settings.setIsTravelMode(false);

      AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      MainFunctions.teardownDataAlarms(context, am);
      MainFunctions.setupNightMode(context, settings, am);

      if (disableData(context, true)) {
         MainFunctions.showNotification(context, settings, context.getString(R.string.data_disabled_night_mode_started));
      }
   }

   /**
    * Handle broadcasts from widget to enable/disable/toggle or query the status
    * of BatteryFu
    * 
    * @param context
    * @param intent
    * @param settings
    * @return
    */
   private boolean handleWidgetBroadcasts(Context context, Intent intent, Settings settings) {
      if ("batteryfu://enable".equals(intent.getDataString())) {
         Log.d("BatteryFu", "Enable BatteryFu");
         BatteryFu.start(context);
         return true;
      }

      if ("batteryfu://toggle".equals(intent.getDataString())) {
         Log.d("BatteryFu", "Toggle BatteryFu");
         BatteryFu.toggle(context);
         return true;
      }

      if ("batteryfu://status".equals(intent.getDataString())) {
         Log.d("BatteryFu", "Widget wants to know BatteryFu status");

         // let the widget know our status
         Intent active = new Intent(context, ToggleWidget.class);
         active.setAction(ToggleWidget.ACTION_WIDGET_RECEIVER);
         if (settings.isEnabled()) {
            active.setData(Uri.parse("batteryfu://enabled"));
         } else {
            active.setData(Uri.parse("batteryfu://disabled"));
         }
         context.sendBroadcast(active);

         return true;
      }

      if (!settings.isEnabled()) {
         // BatteryFu is disabled
         Log.d("BatteryFu", "Disabled, so ignoring broadcast");
         return true;
      }

      if ("batteryfu://disable".equals(intent.getDataString())) {
         Log.d("BatteryFu", "Disable BatteryFu");
         BatteryFu.stop(context);
         return true;
      }

      return false;
   }

   /**
    * Ensure that the screen service is running
    * 
    * @param context
    * @param pref
    */
   static void ensureScreenService(Context context) {
      // start the service for screen on
      Intent srvInt = new Intent(context, ScreenService.class);
      context.startService(srvInt);
   }

   // Disable wifi and mobile data
   static boolean disableData(Context context, boolean force) {
      Log.i("BatteryFu", "DataToggler disabling data");

      Settings settings = Settings.getSettings(context);
      BatteryFu.checkApnDroid(context, settings);
      if (!force) {
         // if (!settings.isDataOn()) {
         // MainFunctions.showNotification(context, settings,
         // "DEBUG: Data is already off");
         // return true;
         // }

         if (settings.isDataWhileCharging() && settings.isCharging()) {
            MainFunctions.showNotification(context, settings, context.getString(R.string.data_switched_on_while_charging));
            return false;
         }

         if (settings.isScreenOnKeepData() && ScreenService.isScreenOn(context)) {
            //MainFunctions.showNotification(context, settings, "Data kept on, waiting for screen to switch off");
            settings.setDisconnectOnScreenOff(true);
            return false;
         }
         
         if (settings.isDataWhileScreenOn() && ScreenService.isScreenOn(context)) {
            MainFunctions.showNotification(context, settings, context.getString(R.string.data_switched_on_while_screen_is_on));
            return false;
         }
      }

      context.getContentResolver().cancelSync(null);

      // save data state
      settings.setDataStateOn(false);
      settings.setSyncOnData(false);

      if (settings.isMobileDataEnabled()) {
         MobileDataSwitcher.disableMobileData(context, settings);
      } else {
         Log.d("BatteryFu", "Mobile data toggling disabled");
      }

      if (settings.isWifiEnabled()) {
         WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
         wm.disconnect();
         wm.setWifiEnabled(false);
      } else {
         Log.d("BatteryFu", "Wifi toggling disabled");
      }

      return true;
   }

   // Enable wifi and mobile data
   static void enableData(final Context context, boolean forceSync) {
      Log.i("BatteryFu", "DataToggler enabling data");
      NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

      final Settings settings = Settings.getSettings(context);
      BatteryFu.checkApnDroid(context, settings);
      if (!settings.isDataOn()) {
         // save data state
         settings.setDataStateOn(true);

         // clear any previous notifications
         nm.cancel(NOTIFICATION_CONNECTIVITY);

         // enable wifi
         if (settings.isWifiEnabled() && !settings.isTravelMode()) {
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            wm.setWifiEnabled(true);
            wm.startScan();
            wm.reconnect();
         } else {
            Log.d("BatteryFu", "Wifi toggling disabled");
         }

         // turn on mobile data
         if (settings.isMobileDataEnabled()) {
            MobileDataSwitcher.enableMobileData(context, settings);
         } else {
            Log.d("BatteryFu", "Mobile data toggling disabled");
         }
      }

      // set flag if sync should run
      if (forceSync) {
         if (Utils.isNetworkConnected(context)) {
            // do the sync now
            MainFunctions.startSync(context);
         } else {
            settings.setSyncOnData(true);

            // I don't trust Android to consistently notify of data connection,
            // so let's also make a manual check
            Utils.waitNonBlocking(15, new Runnable() {
               public void run() {
                  if (settings.isSyncOnData()) {
                     Log.d("BatteryFu", "Manually running sync after timeout");
                     MainFunctions.startSync(context);
                     settings.setSyncOnData(false);
                  }
               }
            });
         }
      } else {
         settings.setSyncOnData(false);
      }
   }

}
