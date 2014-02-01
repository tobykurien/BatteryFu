package com.tobykurien.batteryfu;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class ModeSelect extends Activity {
   String[] modes;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.mode_select);
      
      setTitle(getResources().getString(R.string.mode_select));

      Settings settings = Settings.getSettings(this);
      if (!settings.isEnabled()) {
         showSettings();
         finish();
         return;
      }
      
      ListView lv = (ListView) findViewById(R.id.mode_select_list);
      lv.setAdapter(getAdapter());
      lv.setOnItemClickListener(new OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
            HashMap<String,Object> d = (HashMap<String,Object>) arg0.getAdapter().getItem(pos);
            String mode = (String) d.get("title");
            if (mode == null) return;
            
            Toast.makeText(ModeSelect.this, 
                     "BatteryFu: " + mode, 
                     Toast.LENGTH_SHORT)
                     .show();
            
            Intent i = new Intent(ModeSelect.this, DataToggler.class);
            if (isTravel(mode)) {
               Settings settings = Settings.getSettings(ModeSelect.this);
               if (!settings.isEnabled()) {
                  BatteryFu.start(ModeSelect.this);
               }
               i.setData(Uri.parse("travelmode://on"));
            } else if (isStandard(mode)) {
               Settings settings = Settings.getSettings(ModeSelect.this);
               if (!settings.isEnabled()) {
                  BatteryFu.start(ModeSelect.this);
               }
               i.setData(Uri.parse("standardmode://on"));               
            } else if (isOnline(mode)) {
               i.setData(Uri.parse("onlinemode://on"));               
            } else if (isOffline(mode)) {
               i.setData(Uri.parse("offlinemode://on"));               
            } else if (isNightmodeOn(mode)) {
               // enable nightmode even if data while screen on
               i.setData(Uri.parse("nightmode://force"));               
            } else if (isNightmodeOff(mode)) {
               i.setData(Uri.parse("nightmode://off"));               
            } else if (isSync(mode)) {
               i.setData(Uri.parse("data://on"));               
            } else if (mode.equals(getString(R.string.settings))) {
               showSettings();
               return;
            } else if (isBatteryMinder(mode)) {
               Intent bm = new Intent(ModeSelect.this, BatteryMinder.class);
               bm.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
               startActivity(bm);
            } else {
               // mode not implemented
            }
            
            sendBroadcast(i);
            finish();
         }
      });
   }

   private void showSettings() {
      Intent i = new Intent(ModeSelect.this, BatteryFu.class);               
      startActivity(i);
      finish();
   }
   
   private ListAdapter getAdapter() {
      Settings settings = Settings.getSettings(this);
      
      ArrayList<HashMap<String,Object>> data = new ArrayList<HashMap<String,Object>>();
      
      for (String s: getResources().getStringArray(R.array.mode_select)) {
         HashMap<String,Object> d = new HashMap<String,Object>();
         d.put("title", s);
         
         if (isTravel(s)) {
            if (settings.isWifiEnabled()) {
                  d.put("icon", R.drawable.ic_action_map);
                  data.add(d);
            }
         } else if (isStandard(s)) {
            d.put("icon", R.drawable.ic_action_brightness_low);
            data.add(d);
         } else if (isSync(s)) {
            if (!settings.isDataOn()) {
               d.put("icon", R.drawable.ic_action_refresh);
               data.add(d);
            }
         } else if (isOnline(s)) {
            //if (!settings.isDataOn()) {
            d.put("icon", R.drawable.ic_action_accept);
               data.add(d);
            //}
         } else if (isOffline(s)) {
            //if (settings.isDataOn()) {
            d.put("icon", R.drawable.ic_action_cancel);
               data.add(d);
            //}
         } else if (isBatteryMinder(s)) {
            if (enableBatteryMinder()) {
               d.put("icon", R.drawable.ic_action_battery);               
               data.add(d);
            }
         } else if (isNightmodeOn(s)) {
            if (!settings.isNightmode()) {
               d.put("icon", R.drawable.ic_action_data_usage);
               data.add(d);
            }
         } else if (isNightmodeOff(s)) {
            if (settings.isNightmode()) {
               d.put("icon", R.drawable.ic_action_time);
               data.add(d);
            }
         } else {
            d.put("icon", R.drawable.ic_action_accept);
            data.add(d);
         }
      }         
      
      HashMap<String,Object> d = new HashMap<String,Object>();
      d.put("title", getString(R.string.settings));
      d.put("icon", R.drawable.ic_action_settings);
      data.add(d);

      SimpleAdapter aa = new SimpleAdapter(this, 
               data,
               R.layout.list_item_icon_text, 
               new String[]{ "title", "icon" },
               new int[]{ android.R.id.text1, android.R.id.icon1 });
      
      return aa;
   }
   
   public static boolean enableBatteryMinder() {
      // battery minder only available from android 2.2 up
      return Integer.parseInt(Build.VERSION.SDK) >= 8;
   }

   private boolean isOffline(String mode) {
      return mode.equals(getString(R.string.mode_always_offline));
   }
   
   private boolean isOnline(String mode) {
      return mode.equals(getString(R.string.mode_always_online));
   }
   
   private boolean isStandard(String mode) {
      return mode.equals(getString(R.string.mode_standard));
   }
   
   private boolean isSync(String mode) {
      return mode.equals(getString(R.string.mode_go_online_now));
   }
   
   private boolean isNightmodeOn(String mode) {
      return mode.equals(getString(R.string.mode_start_night_mode));
   }

   private boolean isNightmodeOff(String mode) {
      return mode.equals(getString(R.string.mode_stop_night_mode));
   }

   private boolean isTravel(String mode) {
      return mode.equals(getString(R.string.mode_travel));
   }
   
   private boolean isBatteryMinder(String mode) {
      return mode.equals(getString(R.string.mode_batteryminder));
   }
   
}
