package com.tobykurien.batteryfu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Timer;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.koushikdutta.widgets.ActivityBase;
import com.koushikdutta.widgets.ListItem;

public class BatteryMinder extends ActivityBase {
   BattServiceInfo bstat;
   private static final long MIN_WAKE_LOCK_SECS = 60*30; // only show wakelocks over 30 minutes
   
   @Override
   public void onCreate(Bundle savedInstanceState, View view) {
      super.onCreate(savedInstanceState, view);
      
      int which = bstat.STATS_TOTAL;
      bstat = new BattServiceInfo(this);
      long rawRealtime = System.currentTimeMillis() * 1000;

      long sleep = -1;
      long awake = bstat.getScreenOnTime(rawRealtime, which);
      try {
         String uptime = exec(new String[]{ "/system/bin/cat", "/proc/uptime" });
         sleep = (long) Float.parseFloat(uptime.split(" ")[1]);
      } catch (Exception e) {
         sleep = -1;
      }

      // get cpu stats
      String cslog = "\r\n";
      long busy = -1;
      try {
         String cs = exec(new String[]{ "/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state" });
         for (String csLine: cs.split("\n")) {
            String[] data = csLine.split(" ");
            if (data.length == 2) {
               long b = Long.parseLong(data[1]) * 10000l;
               busy += b;
               cslog += Long.parseLong(data[0])/1000f + " MHz = ";
               cslog += timeFormat(b) + "\r\n";
            }
         }
      } catch (Exception e) {
         cslog = getString(R.string.error_cpu_stats) + e.getMessage();
      }
      
      if (sleep >= 0) {
         cslog += "\r\n" + getString(R.string.cpu_sleep) + " = ";
         cslog += formatTimeRaw(sleep);
      }
      
      String gslog = "";
      // system uptime
//      gslog += "\r\nTime since boot:";
//      gslog += timeFormat(SystemClock.uptimeMillis() * 1000);
//      gslog += "\r\nTime on battery:";
//      gslog += timeFormat(bstat.computeBatteryUptime(SystemClock.elapsedRealtime() * 1000, which));      
      gslog += "\r\n" + getString(R.string.screen_on_time) + " = ";
      gslog += timeFormat(awake);
      gslog += "\r\n" + getString(R.string.battery_usage_while_screen_off) + " = " + bstat.getDischargeAmountScreenOffSinceCharge() + "%";
      gslog += "\r\n" + getString(R.string.battery_level) + ": " + bstat.getDischargeCurrentLevel() + "%";
      
      addItem(R.string.title_general_stats, new ListItem(getFragment(), 
               getString(R.string.background_cpu_usage), gslog, R.drawable.ic_action_battery_light));
      
      // add wakelock info
      String kwlog = "";
      Map<String, ? extends Timer> m = bstat.getKernelWakelockStats();
      if (m != null) for (String key : m.keySet()) {
         long time = bstat.getTotalTimeLocked(m.get(key), rawRealtime, which);
         // only show stuff that's wakelocked for more than 30 minutes
         if (toSecs(time) > MIN_WAKE_LOCK_SECS) kwlog += "\r\n" + key + " = " + timeFormat(time);
      }
      addItem(R.string.title_kernel_wakelocks, new ListItem(getFragment(), 
               getString(R.string.wakelocks_prevent_the_cpu_from_sleeping), kwlog, R.drawable.ic_action_secure));

      addItem(R.string.title_cpu_stats, new ListItem(getFragment(), 
               getString(R.string.time_spend_at_various_cpu_speeds), cslog, R.drawable.ic_action_favourite));
   }
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.menu_batteryminder, menu);
      return super.onCreateOptionsMenu(menu);
   }
   
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.battery_stats) {
         showBatteryUsage(null);
         return true;
      }
      
      return super.onOptionsItemSelected(item);
   }
   
   private String exec(String[] cmdAndArgs) {
      ProcessBuilder cmd;

      try{
       cmd = new ProcessBuilder(cmdAndArgs);

       Process process = cmd.start();
       InputStream in = process.getInputStream();
       ByteArrayOutputStream out = new ByteArrayOutputStream();
       byte[] re = new byte[1024];
       while(in.read(re) != -1){
          out.write(re);
       }
       in.close();
       out.close();
       return out.toString();
      } catch(IOException ex){
         Log.e("BatteryFu", "BatteryMinder error in exec", ex);
         return getString(R.string.error) + ex.getMessage();
      }
   }
   
   public String timeFormat(long microsecs) {
      return formatTimeRaw(toSecs(microsecs));
   }
   
   public long toSecs(long microsecs) {
      return (microsecs - 500)/(1000*1000);
   }
   
   public void showBatteryUsage(View v) {
      Intent powerUsageIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
      powerUsageIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
      ResolveInfo resolveInfo = getPackageManager().resolveActivity(powerUsageIntent, 0);
      // check that the Battery app exists on this device
      if(resolveInfo != null){
          startActivity(powerUsageIntent);
      }      
   }

   private String formatTimeRaw(long seconds) {
      StringBuilder out = new StringBuilder();
      long days = seconds / (60 * 60 * 24);
      if (days != 0) {
          out.append(days);
          out.append(getString(R.string.suffix_days));
      }
      long used = days * 60 * 60 * 24;

      long hours = (seconds - used) / (60 * 60);
      if (hours != 0 || used != 0) {
          out.append(hours);
          out.append(getString(R.string.suffix_hours));
      }
      used += hours * 60 * 60;

      long mins = (seconds-used) / 60;
      if (mins != 0 || used != 0) {
          out.append(mins);
          out.append(getString(R.string.suffix_minutes));
      }
      used += mins * 60;

      if (seconds != 0 || used != 0) {
          out.append(seconds-used);
          out.append(getString(R.string.suffix_seconds));
      }
      
      return out.toString();
  }
   
  public static void checkBattery(Context context) {
     Settings settings = Settings.getSettings(context);

     SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
     
     long lastRun = settings.getLastRun();
     
     if (System.currentTimeMillis() - lastRun < 1000*60*10) {
        // too soon to check
        return;
     }
     
     settings.setLastRun(System.currentTimeMillis());
  }
}
