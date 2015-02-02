package com.tobykurien.batteryfu;

import java.io.File;
import java.io.FileWriter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.tobykurien.android.Utils;
import com.tobykurien.batteryfu.data_switcher.APNDroidSwitcher;
import com.tobykurien.batteryfu.data_switcher.APNSwitcher;
import com.tobykurien.batteryfu.data_switcher.GingerbreadSwitcher;
import com.tobykurien.batteryfu.data_switcher.ICSSwitcher;
import com.tobykurien.batteryfu.data_switcher.MobileDataSwitcher;
import com.tobykurien.batteryfu.data_switcher.LollipopSwitcher;

public class BatteryFu extends PreferenceActivity {
   public static final String LOG_TAG = "BatteryFu";
   public static final int DIALOG_ABOUT_ID = 0;
   public static final int DIALOG_APN_PROBLEM = 1;
   private static final String PACKAGE_APNDROID = "com.google.code.apndroid";
   private static int mobileErrorMsg = 0;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.i("BatteryFu", "Loading on API level " + android.os.Build.VERSION.SDK);

       PreferenceManager pm = getPreferenceManager();
       if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
           pm.setSharedPreferencesMode(MODE_MULTI_PROCESS);
       pm.setSharedPreferencesName(Settings.PREFS_NAME);


      // Show the preferences screen
      try {
         Settings settings = Settings.getSettings(getApplicationContext());
         if (settings.getNightmodeStart(null) == null) {
            // set night-mode defaults (TimePicker defaults not working)
            settings.setNightmodeStart(Settings.DEFAULT_NIGHT_MODE_START);
            settings.setNightmodeEnd(Settings.DEFAULT_NIGHT_MODE_END);
         }

         addPreferencesFromResource(R.xml.settings);

         // test apn
         mobileErrorMsg = MobileDataSwitcher.getSwitcher(this, settings).isToggleWorking(getApplicationContext());
         if (mobileErrorMsg >= 1) {
            showDialog(DIALOG_APN_PROBLEM);
         }

         // Data while charging only supported in API level 4 and above
         if (Integer.parseInt(Build.VERSION.SDK) < 4) {
            Preference chargerOnData = findPreference("charger_on_data");
            if (chargerOnData != null) {
               chargerOnData.setDependency(null);
               chargerOnData.setEnabled(false);
               chargerOnData.setSummary(R.string.requires_android_1_6);
            }
         }

         // on gingerbread or above, no hacks needed
         if (!(MobileDataSwitcher.getSwitcher(this, settings) instanceof APNSwitcher)) {
            Preference dnsFix = findPreference("dns_fix");
            if (dnsFix != null) {
               dnsFix.setDependency(null);
               dnsFix.setEnabled(false);
               dnsFix.setSummary(R.string.not_needed_on_this_device);
            }
         }
      } catch (Exception e) {
         Log.e("BatteryFu", "Error creating activity", e);
         Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.about:
            showDialog(DIALOG_ABOUT_ID);
            return true;
         case R.id.exit:
            finish();
            return true;
      }
      return false;
   }

   @Override
   protected Dialog onCreateDialog(int id) {
      Dialog dialog;
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      switch (id) {
         case DIALOG_ABOUT_ID:
            builder.setMessage(R.string.about_text).setTitle(R.string.about_title)
                     .setNegativeButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                           dialog.cancel();
                        }
                     });
            dialog = builder.create();
            break;
         case DIALOG_APN_PROBLEM:
            builder.setMessage(mobileErrorMsg).setTitle(mobileErrorMsg - 1).setNegativeButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
               }
            });
            dialog = builder.create();
            break;
         default:
            dialog = null;
      }
      return dialog;
   }

   @Override
   protected void onPause() {
      Log.i("BatteryFu", "Preferences closed, setting up");
      
      // Cleanup APN state in case user switched from one switcher to another
      Settings settings = Settings.getSettings(getApplicationContext());
      if (!(MobileDataSwitcher.getSwitcher(this, settings) instanceof APNSwitcher)) {
         // not using APN switcher, so cleanup
         settings.setIsDnsFix(false);
         if (!Utils.isICS()) {
            new APNSwitcher().enableMobileData(this);         
         }
      }
           
      initBatteryFu(getApplicationContext(), false);

      super.onPause();
   }

   private static void initBatteryFu(Context context, boolean firstRun) {
      Intent srvInt = new Intent(context, ScreenService.class);
      try {
         try {
            MainFunctions.cancelNotification(context);
         } catch (Exception e) {
            Log.e("BatteryFu", "Error cancelling notification", e);
         }

         Settings settings = Settings.getSettings(context);
         if (settings.isEnabled()) {
            // User has enabled
            Log.i("BatteryFu", "Starting process");

            // check the charging state
            boolean isCharging = false;
            try {
               Intent bat = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
               int plugType = bat.getIntExtra("plugged", 0);
               isCharging = plugType > 0;
            } catch (Exception e) {
               isCharging = false;
            }
            settings.setIsCharging(isCharging);

            checkApnDroid(context, settings);

            MainFunctions.startScheduler(context, false);
            Toast.makeText(context, R.string.batteryfu_started, Toast.LENGTH_LONG).show();

            if (settings.isDataWhileScreenOn() || settings.isScreenOnKeepData()) {
               // start the service for screen on
               context.startService(srvInt);
            } else {
               try {
                  context.stopService(srvInt);
               } catch (Exception e) {
               }
            }

            if (firstRun) {
               firstRun(context, settings);
            }
         } else {
            Log.i("BatteryFu", "Stopping");

            try {
               context.stopService(srvInt);
            } catch (Exception e) {
            }

            MainFunctions.stopScheduler(context);
            Toast.makeText(context, R.string.batteryfu_stopped, Toast.LENGTH_LONG).show();
         }
      } catch (Exception e) {
         Log.e("BatteryFu", "Error creating activity", e);
         Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
      }
   }

   public static void checkApnDroid(Context context, Settings settings) {
      MobileDataSwitcher switcher = MobileDataSwitcher.getSwitcher(context, settings);
      if (switcher instanceof GingerbreadSwitcher ||
          switcher instanceof ICSSwitcher ||
          switcher instanceof LollipopSwitcher) {
         // on gingerbread/ICS, we won't worry about ApnDroid
         settings.setUseApnDroid(false);
         return;
      }
      if (APNDroidSwitcher.isApnDroidInstalled(context)) {
         if (!settings.isUseApndroid()) {
            // APNDroid installed, use it by default
            settings.setUseApnDroid(true);
         }
      } else {
         if (settings.isUseApndroid())
             settings.setUseApnDroid(false);
      }
   }

   /**
    * Things to do at the first run
    * 
    * @param context
    * @param settings
    */
   public static void firstRun(final Context context, Settings settings) {
      settings.setFirstRun();
      
      // stuff that needs to happen only on first run
      if (settings.isMobileDataEnabled() && settings.isDnsFix()) {
         try {
            // make sure we have root access
            String filename = context.getCacheDir().getPath() + "/dnsfix.sh";
            File f = new File(filename);
            if (f.exists()) f.delete();
            FileWriter fw = new FileWriter(f);
            fw.append("echo 'I need root permission, make it sticky'\n");
            fw.flush();
            fw.close();

            Utils.exec(new String[] { "chmod", "+x", filename });
            Utils.exec(new String[] { "su", "-c", filename });
         } catch (Exception e) {
            Toast.makeText(context, "Error tring to get root access " + e.getMessage(), Toast.LENGTH_LONG).show();
         }
      }

      // check for APNdroid and suggest installing it
//      if (!(MobileDataSwitcher.getSwitcher(context, settings) instanceof GingerbreadSwitcher) &&
//               !(MobileDataSwitcher.getSwitcher(context, settings) instanceof ICSSwitcher)) {
//         if (!APNDroidSwitcher.isApnDroidInstalled(context)) {
//            Utils.confirm(
//                     context,
//                     LOG_TAG,
//                     "APNdroid not found",
//                     "APNdroid is not installed. While BatteryFu may work without it, it is strongly recommended that you "
//                              + "install APNdroid for better mobile data switching. BatteryFu will automatically use APNdroid once it is installed."
//                              + "Would you like to install it now?", "Install", "Cancel", new Runnable() {
//                        @Override
//                        public void run() {
//                           Utils.openActionView(context, "market://search?q=pname:" + PACKAGE_APNDROID);
//                        }
//                     }, null);
//         }
//      }
   }

   /**
    * Start BatteryFu if it's stopped
    * 
    * @param context
    */
   public static void start(Context context) {
      Settings settings = Settings.getSettings(context);
      if (!settings.isEnabled()) {
         settings.setEnabled(true);
         BatteryFu.initBatteryFu(context, settings.isFirstRun());
      }
   }

   /**
    * Stop BatteryFu if it's running
    * 
    * @param context
    */
   public static void stop(Context context) {
      Settings settings = Settings.getSettings(context);
      if (settings.isEnabled()) {
         settings.setEnabled(false);
         BatteryFu.initBatteryFu(context, false);
      }
   }

   /**
    * Toggle the state of BatteryFu
    * 
    * @param context
    */
   public static void toggle(Context context) {
      Settings settings = Settings.getSettings(context);
      if (!settings.isEnabled()) {
         start(context);
      } else {
         stop(context);
      }
   }

}