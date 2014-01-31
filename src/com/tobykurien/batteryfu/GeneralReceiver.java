package com.tobykurien.batteryfu;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import com.tobykurien.android.Utils;

/**
 * Not currently used. For this to work, there must be a long-living service as
 * Android
 * does not want to create processes when screen turns off or on.
 * 
 * @author toby
 * 
 */
public class GeneralReceiver extends BroadcastReceiver {

   @Override
   public void onReceive(Context context, Intent intent) {
      Log.d("BatteryFu", "GeneralReceiver received broadcast");

      try {
         Settings settings = Settings.getSettings(context);

         if (settings.isEnabled()) {
            // check for screen wake/sleep
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
               Log.d("BatteryFu", "Receiver: Screen is off");
               ScreenService.setScreenOn(context, false);

               // if screen on data is enabled, switch data off when screen goes
               // to sleep
               if (settings.isDataWhileScreenOn() || (settings.isScreenOnKeepData() && settings.isDisconnectOnScreenOff())) {
                  settings.setDisconnectOnScreenOff(false);
                  String sleepTime = settings.getScreenOffDelayTime();
                  int iSleepTime = 30;
                  try {
                     iSleepTime = Integer.parseInt(sleepTime);
                  } catch (Exception e) {
                  }

                  Intent intentSleep = new Intent(Intent.ACTION_EDIT, Uri.parse("data://sleep_once"), context, DataToggler.class);
                  intentSleep.putExtra(MainFunctions.INTENT_DATA_STATE, false);
                  if ("0".equals(sleepTime)) {
                     Log.d("BatteryFu", "Switching data off with no delay");
                     // switch data off immediately
                     context.sendBroadcast(intentSleep);
                  } else {
                     Log.d("BatteryFu", "Delaying data switch off by " + iSleepTime + " seconds");
                     // start a timer to switch data off
                     PendingIntent senderSleep = PendingIntent.getBroadcast(context, 0, intentSleep, 0);

                     // Schedule the sleep alarm!
                     long triggerTime = SystemClock.elapsedRealtime() + (long) (iSleepTime * 1000);
                     AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                     am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, senderSleep);
                  }
               }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
               Log.d("BatteryFu", "Receiver: Screen is on");
               ScreenService.setScreenOn(context, true);

               // if screen on data is enabled, switch data on when screen is on
               if (settings.isDataWhileScreenOn() && !settings.isWaitForScreenUnlock()) {
                  DataToggler.enableData(context, false);
                  MainFunctions.showNotification(context, settings, context.getString(R.string.data_switched_on_while_screen_is_on));
               }
            } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
               Log.d("BatteryFu", "Receiver: Screen unlocked");
               ScreenService.setScreenOn(context, true);

               // if screen on data is enabled, switch data on when screen
               // unlocks
               if (settings.isDataWhileScreenOn() && settings.isWaitForScreenUnlock()) {
                  DataToggler.enableData(context, false);
                  MainFunctions.showNotification(context, settings, context.getString(R.string.data_switched_on_while_screen_is_unlocked));
               }
            } else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
               Log.d("BatteryFu", "Boot completed");
               // Boot complete, start up BatteryFu
               if (settings.isStartOnBoot()) {
                  Log.d("BatteryFu", "Starting up");
                  startup(context, context.getString(R.string.started_on_boot));
               }
            } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
               Log.d("BatteryFu", "Connectivity changed");
               // network state change
               if (Utils.isNetworkConnected(context) && settings.isSyncOnData()) {
                  Log.d("BatteryFu", "Network connected, starting sync");

                  // sync once only
                  settings.setSyncOnData(false);

                  MainFunctions.startSync(context);
                  MainFunctions.showNotification(context, settings, context.getString(R.string.data_connected_running_sync));
               }
            } else if (!Utils.isBelowApi4() && intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
               Log.d("BatteryFu", "Receiver: Charger plugged in");
               settings.setIsCharging(true);
               if (!settings.isNightmode() && settings.isDataWhileCharging() && !settings.isDataOn()) {
                  DataToggler.enableData(context, false);
                  MainFunctions.showNotification(context, settings, context.getString(R.string.data_switched_on_while_charging));
               }
            } else if (!Utils.isBelowApi4() && intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
               Log.d("BatteryFu", "Receiver: Charger removed");
               settings.setIsCharging(false);
               if (!settings.isNightmode() && settings.isDataWhileCharging() && settings.isDataOn()) {
                  if (DataToggler.disableData(context, false)) {
                     MainFunctions.showNotificationWaitingForSync(context, settings);
                  }
               }
            }
         }
      } catch (Exception e) {
         Log.e("BatteryFu", "Error in general receiver", e);
         Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
      }
   }

   /**
    * Start up after boot or upgrade
    * 
    * @param context
    * @param pref
    */
   private void startup(Context context, String notification) {
      Settings settings = Settings.getSettings(context);
      if (settings.isEnabled()) {
         MainFunctions.startScheduler(context, true);
         MainFunctions.showNotification(context, settings, notification);
      }
   }
}
