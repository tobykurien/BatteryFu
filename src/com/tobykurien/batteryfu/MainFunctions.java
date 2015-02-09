package com.tobykurien.batteryfu;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import com.tobykurien.android.Utils;
import com.tobykurien.batteryfu.data_switcher.APNSwitcher;
import com.tobykurien.batteryfu.data_switcher.MobileDataSwitcher;

public class MainFunctions {
   public static final int NOTIFICATION_ID_RUNNING = 1;
   public static final String INTENT_DATA_STATE = "DATA_STATE";
   public static int PERIOD_24_HOURS = 1000 * 60 * 60 * 24;

   static GeneralReceiver networkReceiver = new GeneralReceiver();

   /**
    * Start the scheduling process
    */
   public static void startScheduler(Context context, boolean syncFirst) {
      Log.i("BatteryFu", "Starting scheduler");

      Settings settings = Settings.getSettings(context);
      MainFunctions.showNotification(context, settings, context.getString(R.string.starting));

      // cancel account syncs
      ContentResolver.cancelSync(null, null);
      
      // let our widget know we're up
      Intent active = new Intent(context, ToggleWidget.class);
      active.setAction(ToggleWidget.ACTION_WIDGET_RECEIVER);
      active.setData(Uri.parse("batteryfu://enabled"));
      context.sendBroadcast(active);

      // if user has disabled fiddling with mobile data, restore APN type
//      if (!settings.isMobileDataEnabled()) {
//         APNSwitcher.enableMobileData(context, settings);
//      }

      // set up default flag values
      settings.setDataStateOn(true);
      settings.setSyncOnData(false);
      settings.setIsNightmode(false);
      settings.setIsTravelMode(false);
      settings.setDisconnectOnScreenOff(false);

      Settings.getSettings(context).setLastWakeTime(System.currentTimeMillis());
      AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      setupNightMode(context, settings, am);

      if (settings.isNightmodeOnly()) {
         // for nightmode only, no need for normal data alarms
         MainFunctions.showNotification(context, settings, context.getString(R.string.data_enabled_until_next_night_mode_start));
         // make sure data is infact on
         DataToggler.enableData(context, false);
      } else {
         setupDataAlarms(context, am, syncFirst);

         // go to sleep now
         if (DataToggler.disableData(context, true)) {
            MainFunctions.showNotificationWaitingForSync(context, settings);
         } else {
            // data being left on for some reason (e.g. data while screen on), make sure it is infact on
            DataToggler.enableData(context, false);
         }
      }
   }

   /**
    * Stop the scheduling process
    */
   public static void stopScheduler(Context context) {
      Log.i("BatteryFu", "Stopping scheduler");

      AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      teardownDataAlarms(context, am);

      // re-awaken the data
      // context.sendBroadcast(intentWake);
      DataToggler.enableData(context, false);

      teardownNightMode(context, am);

      // let our widget know we're down
      Intent active = new Intent(context, ToggleWidget.class);
      active.setAction(ToggleWidget.ACTION_WIDGET_RECEIVER);
      active.setData(Uri.parse("batteryfu://disabled"));
      context.sendBroadcast(active);

      // set up default flag values
      Settings settings = Settings.getSettings(context);
      settings.setDataStateOn(true);
      settings.setSyncOnData(false);
      settings.setIsNightmode(false);
      settings.setIsTravelMode(false);
   }

   static void setupDataAlarms(Context context, AlarmManager am, boolean startNow) {
      if (am == null)
         am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      Settings settings = Settings.getSettings(context);
      try {
         Settings.SLEEP_PERIOD = 1000 * 60 * (Integer.parseInt(settings.getSleepTime()));
         if (Settings.SLEEP_PERIOD < 1000 * 60 * 15) {
            // update old, overly-aggressive settings
            throw new Exception("set default");
         }
      } catch (Exception ne) {
         Settings.SLEEP_PERIOD = 1000 * 60 * Integer.parseInt(Settings.DEFAULT_SLEEP); // default
      }
      Log.d("BatteryFu", "Sleep period: " + Settings.SLEEP_PERIOD);

      try {
         int awake_time = Integer.parseInt(settings.getAwakeTime());
         if (awake_time < Settings.MIN_AWAKE_TIME)
            awake_time = Settings.MIN_AWAKE_TIME; // removed the 1 minute awake,
                                                  // it's too short
         Settings.AWAKE_PERIOD = 1000 * 60 * (awake_time);
      } catch (Exception ne) {
         Settings.AWAKE_PERIOD = 1000 * 60 * Integer.parseInt(Settings.DEFAULT_AWAKE); // default
      }

      if (Settings.DEBUG_NIGHT_MODE) {
         Settings.SLEEP_PERIOD = 1000 * 60;
         Settings.AWAKE_PERIOD = 1000 * 50;
         startNow = true;
      }

      // When the alarm goes off, we want to broadcast an Intent to our
      // BroadcastReceiver. Here we make an Intent with an explicit class
      // name to have our own receiver (which has been published in
      // AndroidManifest.xml) instantiated and called, and then create an
      // IntentSender to have the intent executed as a broadcast.
      // Note that unlike above, this IntentSender is configured to
      // allow itself to be sent multiple times.
      Intent intentWake = new Intent(Intent.ACTION_EDIT, Uri.parse("data://wake"), context, DataToggler.class);
      intentWake.putExtra(INTENT_DATA_STATE, true);
      PendingIntent senderWake = PendingIntent.getBroadcast(context, 0, intentWake, 0);

      // Set the sleep period
      long firstTime = SystemClock.elapsedRealtime();
      if (!startNow)
         firstTime += Settings.SLEEP_PERIOD;

      // Schedule the wake alarm!
      am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, Settings.SLEEP_PERIOD, senderWake);

      // Set up sleep intent
      Intent intentSleep = new Intent(Intent.ACTION_EDIT, Uri.parse("data://sleep"), context, DataToggler.class);
      intentSleep.putExtra(INTENT_DATA_STATE, false);
      PendingIntent senderSleep = PendingIntent.getBroadcast(context, 0, intentSleep, 0);

      // Schedule the sleep alarm!
      am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime + Settings.AWAKE_PERIOD, Settings.SLEEP_PERIOD, senderSleep);
   }

   // set up the night mode alarms
   static void setupNightMode(Context context, Settings settings, AlarmManager am) {
      Log.d("BatteryFu", "Setting up night mode");

      if (settings.isNightmodeEnabled()) {
         // Set the sleep period
         String nmStart = settings.getNightmodeStart();
         String nmEnd = settings.getNightmodeEnd();

         Log.d("BatteryFu", "Night mode configured as " + nmStart + " till " + nmEnd);

         Date now = new Date();
         SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy/MM/dd");
         SimpleDateFormat dateTime = new SimpleDateFormat("yyyy/MM/dd HH:mm");
         Date dStart = null;
         Date dEnd = null;
         try {
            dStart = dateTime.parse(dateOnly.format(now) + " " + nmStart);
            dEnd = dateTime.parse(dateOnly.format(now) + " " + nmEnd);
         } catch (ParseException e) {
            Log.e("BatteryFu", "Error enabling night mode", e);
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
         }

         if (now.after(dStart)) {
            dStart = new Date(dStart.getTime() + PERIOD_24_HOURS);
         }
         if (now.after(dEnd)) {
            dEnd = new Date(dEnd.getTime() + PERIOD_24_HOURS);
         }

         Log.d("BatteryFu", "Night mode from " + dStart.toLocaleString() + " till " + dEnd.toLocaleString());

         // schedule night mode
         Intent intentNMOn = new Intent(Intent.ACTION_EDIT, Uri.parse("nightmode://on"), context, DataToggler.class);
         Intent intentNMOff = new Intent(Intent.ACTION_EDIT, Uri.parse("nightmode://off"), context, DataToggler.class);
         PendingIntent senderNMOn = PendingIntent.getBroadcast(context, 0, intentNMOn, 0);
         PendingIntent senderNMOff = PendingIntent.getBroadcast(context, 0, intentNMOff, 0);
         am.setRepeating(AlarmManager.RTC_WAKEUP, dStart.getTime(), PERIOD_24_HOURS, senderNMOn);
         am.setRepeating(AlarmManager.RTC_WAKEUP, dEnd.getTime(), PERIOD_24_HOURS, senderNMOff);
      }
   }

   static void teardownDataAlarms(Context context, AlarmManager am) {
      if (am == null)
         am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      // Create the same intent, and thus a matching IntentSender, for
      // the one that was scheduled.
      Intent intentWake = new Intent(Intent.ACTION_EDIT, Uri.parse("data://wake"), context, DataToggler.class);
      PendingIntent senderWake = PendingIntent.getBroadcast(context, 0, intentWake, 0);

      Intent intentSleep = new Intent(Intent.ACTION_EDIT, Uri.parse("data://sleep"), context, DataToggler.class);
      PendingIntent senderSleep = PendingIntent.getBroadcast(context, 0, intentSleep, 0);

      // And cancel the alarm.
      am.cancel(senderWake);
      am.cancel(senderSleep);
   }

   // Remove the nightmode alarms
   static void teardownNightMode(Context context, AlarmManager am) {
      try {
         // cancel nightmode
         Intent intentNMOn = new Intent(Intent.ACTION_EDIT, Uri.parse("nightmode://on"), context, DataToggler.class);
         Intent intentNMOff = new Intent(Intent.ACTION_EDIT, Uri.parse("nightmode://off"), context, DataToggler.class);
         PendingIntent senderNMOn = PendingIntent.getBroadcast(context, 0, intentNMOn, 0);
         PendingIntent senderNMOff = PendingIntent.getBroadcast(context, 0, intentNMOff, 0);
         am.cancel(senderNMOn);
         am.cancel(senderNMOff);
      } catch (Exception e) {
         Log.e("BatteryFu", "Error cancelling nightmode", e);
      }
   }

   public static void cancelNotification(Context context) {
      // cancel any notifications
      String ns = Context.NOTIFICATION_SERVICE;
      NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
      mNotificationManager.cancel(NOTIFICATION_ID_RUNNING);
   }

   // display a notification while BatteryFu is running
   public static void showNotification(Context context, Settings settings, String text) {
      String ns = Context.NOTIFICATION_SERVICE;
      NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

      if (!settings.isShowNotification()) {
         // clear existing notification
         mNotificationManager.cancel(NOTIFICATION_ID_RUNNING);
         return;
      }

      int icon = R.drawable.ic_stat_notif;
      long when = System.currentTimeMillis();
      Notification notification = new Notification(icon, null, when);

      notification.flags |= Notification.FLAG_ONGOING_EVENT;
      notification.flags |= Notification.FLAG_NO_CLEAR;

      // define extended notification area
      CharSequence contentTitle = "BatteryFu";
      CharSequence contentText = text;

      Intent notificationIntent = new Intent(context, ModeSelect.class);
      notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
      notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
      mNotificationManager.notify(NOTIFICATION_ID_RUNNING, notification);

   }

   public static void showNotificationWaitingForSync(Context context, Settings settings) {
      String message = context.getString(R.string.data_disabled_waiting_for_next_sync);

      long lastWakeTime = settings.getLastWakeTime();
      if (lastWakeTime > 0) {
         try {
            long nextSyncTime = lastWakeTime + (Long.parseLong(settings.getSleepTime()) * 60 * 1000);
            if (nextSyncTime > System.currentTimeMillis()) {
               SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
               message = context.getString(R.string.data_disabled_next_sync_at_) + " " + sdf.format(new Date(nextSyncTime));
               if (settings.isTravelMode()) {
                  message += " [" + context.getString(R.string.mode_travel_short) + "]";
               }
            }
         } catch (Exception e) {
            Utils.handleException("BatteryFu", context, e);
         }
      }

      showNotification(context, settings, message);
   }

   public static void startSync(Context context) {
      Settings settings = Settings.getSettings(context);

      // start the sync'ing process
      Bundle extras = new Bundle();
      if (settings.isForceSync()) {
         extras.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
      } else {
         extras.putBoolean(ContentResolver.SYNC_EXTRAS_ACCOUNT, true);
      }
      context.getContentResolver().startSync(null, extras);
   }
}
