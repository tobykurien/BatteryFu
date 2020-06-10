package com.tobykurien.batteryfu;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import android.app.PendingIntent;
import android.os.SystemClock;

import com.tobykurien.batteryfu.compat.Api17;
import com.tobykurien.batteryfu.compat.Api3;

public class DataToggler extends BroadcastReceiver {
   public static final int NOTIFICATION_CONNECTIVITY = 1;

   @SuppressWarnings("deprecation")
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
         if (handleWidgetBroadcasts(context, intent, settings)) { return; }

         if ("data://wake".equals(intent.getDataString()) || "data://on".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Data enabled by " + intent.getDataString());
            settings.setLastWakeTime(System.currentTimeMillis());

            // Check for airplane mode
            boolean isAirplaneMode = false;
            if (Integer.parseInt(Build.VERSION.SDK) < 17) {
               isAirplaneMode = Api3.isAirplaneMode(context);
            } else {
               isAirplaneMode = Api17.isAirplaneMode(context);
            }

            enableData(context, true);
            if (!isAirplaneMode) {
               // keep the notification running
               MainFunctions.showNotification(context, settings, context.getString(R.string.data_enabled_waiting_for_connection));
            } else {
               // keep the notification running
               MainFunctions.showNotification(context, settings, context.getString(R.string.airplane_mode_is_on));
            }

            // Schedule the sleep alarm!
            final AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Intent intentSleep = new Intent(Intent.ACTION_EDIT, Uri.parse("data://sleep"), context, DataToggler.class);
            intentSleep.putExtra(MainFunctions.INTENT_DATA_STATE, false);
            PendingIntent senderSleep = PendingIntent.getBroadcast(context, 0, intentSleep, 0);
            long sleepAlarm = SystemClock.elapsedRealtime() + Settings.AWAKE_PERIOD;
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, sleepAlarm, senderSleep);

         } else if ("data://sleep".equals(intent.getDataString()) || "data://off".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Data disabled by " + intent.getDataString());
            disableData(context, false, DataService.NOTIFICATION_TYPE_WAITING_FOR_SYNC);

            // Schedule the wake alarm!
            final AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Intent intentWake = new Intent(Intent.ACTION_EDIT, Uri.parse("data://wake"), context, DataToggler.class);
            intentWake.putExtra(MainFunctions.INTENT_DATA_STATE, true);
            PendingIntent senderWake = PendingIntent.getBroadcast(context, 0, intentWake, 0);
            long wakeAlarm = SystemClock.elapsedRealtime() + Settings.SLEEP_PERIOD;
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, wakeAlarm, senderWake);
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
            // make sure scheduler is started
            MainFunctions.startScheduler(context, false);
            if (settings.isWifiEnabled()) {
               // disable wifi for travel mode
               WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
               wm.disconnect();
               wm.setWifiEnabled(false);

               settings.setIsTravelMode(true);
//               MainFunctions.showNotification(context, settings, context.getString(R.string.wifi_disabled_travel_mode_activated));
//            } else {
//               MainFunctions.showNotification(context, settings,
//                        context.getString(R.string.wifi_toggling_not_enabled_standard_mode_activated));
               MainFunctions.showNotificationWaitingForSync(context, settings);
            }
         } else if ("offlinemode://on".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Always offline mode enable");
            MainFunctions.teardownDataAlarms(context, null);
            disableData(context, true, DataService.NOTIFICATION_TYPE_OFFLINE_MODE);
            MainFunctions.showNotification(context, settings, context.getString(R.string.data_disabled_offline_mode_activated));
         } else if ("onlinemode://on".equals(intent.getDataString())) {
            Log.d("BatteryFu", "Always online mode enable");
            MainFunctions.teardownDataAlarms(context, null);
            enableData(context, false, true); // enable mobile and wifi when
                                              // going into online mode
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
      Log.d("BatteryFu", "Setting night mode on");
      settings.setIsNightmode(true);
      //settings.setIsTravelMode(false);

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

   static void ensureScreenService(Context context) {
      // start the service for screen on
      Intent srvInt = new Intent(context, ScreenService.class);
      context.startService(srvInt);
   }

    static boolean disableData(final Context context, boolean force)
    {
        return disableData(context, force, DataService.NOTIFICATION_TYPE_NONE);
    }

   // Disable wifi and mobile data
   static boolean disableData(final Context context, boolean force, final int notificationType) {
      Log.i("BatteryFu", "DataToggler disabling data");

       Intent intent = new Intent(context, DataService.class);
       intent.putExtra("action", "disable");
       intent.putExtra("force", force);
       intent.putExtra("notificationType", notificationType);
       context.startService(intent);

      return true;
   }

   // Enable wifi and mobile data
   static void enableData(final Context context, boolean forceSync) {
      enableData(context, forceSync, false);
   }

   // Enable wifi and mobile data
   static void enableData(final Context context, final boolean forceSync, final boolean forceMobile) {
      Log.i("BatteryFu", "DataToggler enabling data");
       Intent intent = new Intent(context, DataService.class);
       intent.putExtra("action", "enable");
       intent.putExtra("force", forceSync);
       intent.putExtra("forceMobile", forceMobile);
       context.startService(intent);
   }


}
