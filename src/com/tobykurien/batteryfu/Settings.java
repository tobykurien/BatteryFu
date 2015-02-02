package com.tobykurien.batteryfu;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class Settings {
	public static boolean DEBUG_NIGHT_MODE = false;
	public static final String DEFAULT_NIGHT_MODE_END = "06:00";
	public static final String DEFAULT_NIGHT_MODE_START = "22:00";
	public final static int MIN_AWAKE_TIME = 2;
	public static final String DEFAULT_AWAKE = "3";
	public static final String DEFAULT_SLEEP = "60";
	public static int AWAKE_PERIOD = 0;
	public static int SLEEP_PERIOD = 0;
	public static int FIRST_RUN_VERSION = 2;
    public static final String PREFS_NAME = "com.tobykurien.BatteryFu.shared_prefs";

	private SharedPreferences mPref;
    private static Settings mInstance;

	private Settings(SharedPreferences preferences) {
		mPref = preferences;
	}

	public static synchronized Settings getSettings(Context context) {
        if(mInstance == null)
        {
            SharedPreferences preferences;
            Context ctx = context.getApplicationContext();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            {
                preferences = ctx.getSharedPreferences(PREFS_NAME, 0 | Context.MODE_MULTI_PROCESS);
            }
            else
            {
                preferences = ctx.getSharedPreferences(PREFS_NAME, 0);
            }
            mInstance = new Settings(preferences);
        }

      return mInstance;
	}
	
	public boolean isDnsFix() {
		return mPref.getBoolean("dns_fix", false);		
	}

   public void setIsDnsFix(boolean dnsFix) {
      mPref.edit().putBoolean("dns_fix", dnsFix).commit();
   }
	
	public boolean isEnabled() {
		return mPref.getBoolean("enabled", false);
	}

   public boolean isForceSync() {
      return mPref.getBoolean("force_sync", true);
   }
	
   public boolean isNightmodeOnly() {
      if (!isNightmodeEnabled()) return false;
      return mPref.getBoolean("night_mode_only", false);
   }

   public String getNightmodeStart() {
		return getNightmodeStart(Settings.DEFAULT_NIGHT_MODE_START);
	}

	public String getNightmodeStart(String defaultVal) {
		return mPref.getString("night_mode_start", defaultVal);
	}	
	
	public String getNightmodeEnd() {
		return mPref.getString("night_mode_end", Settings.DEFAULT_NIGHT_MODE_END);
	}
	
	public boolean isDataWhileScreenOn() {
		return mPref.getBoolean("screen_on_data", false);
	}

	public boolean isScreenOnKeepData() {
      return mPref.getBoolean("screen_keep_data", true);
   }

	public boolean isMobileDataEnabled() {
		return mPref.getBoolean("mobile", false);
	}

	public boolean isDataWhileCharging() {
		return mPref.getBoolean("charger_on_data", false);
	}
	
	public boolean isNightmodeEnabled() {
		return mPref.getBoolean("night_mode_enabled", false);
	}

	public boolean isWifiEnabled() {
		return mPref.getBoolean("wifi", true);
	}

	public String getScreenOffDelayTime() {
		return mPref.getString("screen_off_sleep_time", "30");
	}

	public boolean isWaitForScreenUnlock() {
		return mPref.getBoolean("screen_on_unlock", true);
	}

	public boolean isStartOnBoot() {
		return mPref.getBoolean("start_on_boot", true);
	}

	public String getSleepTime() {
		return mPref.getString("sleep_time", Settings.DEFAULT_SLEEP);
	}

	public String getAwakeTime() {
		return mPref.getString("awake_time", Settings.DEFAULT_AWAKE);
	}

	public boolean isShowNotification() {
		return mPref.getBoolean("show_notification", true);
	}

	public boolean isUseApndroid() {
		return mPref.getBoolean("apndroid", false);
	}

   public boolean isFirstRun() {
      return mPref.getInt("first_run", 0) != FIRST_RUN_VERSION;
   }

   public void setFirstRun() {
      mPref.edit().putInt("first_run", FIRST_RUN_VERSION).commit();     
   }
   
	public void setNightmodeStart(String start) {
		mPref.edit().putString("night_mode_start", start).commit();		
	}

	public void setNightmodeEnd(String end) {
		mPref.edit().putString("night_mode_end", end).commit();		
	}

	public void setEnabled(boolean enabled) {
		mPref.edit().putBoolean("enabled", enabled).commit();
	}

	public boolean isDataOn() {
		return mPref.getBoolean("data_state_on", true);
	}

	public void setDataStateOn(boolean on) {
		mPref.edit().putBoolean("data_state_on", on).commit();
	}

	public void setSyncOnData(boolean sync) {
		mPref.edit().putBoolean("sync_on_data", sync).commit();
	}

	public boolean isSyncOnData() {
		return mPref.getBoolean("sync_on_data", false);
	}

	public void setIsCharging(boolean isCharging) {
		mPref.edit().putBoolean("state_charging", isCharging).commit();
	}
	
	public boolean isCharging() {
		return mPref.getBoolean("state_charging", false);
	}
	
	public void setIsNightmode(boolean isNightmode) {
		mPref.edit().putBoolean("state_nightmode", isNightmode).commit();
	}
	
	public boolean isNightmode() {
		return mPref.getBoolean("state_nightmode", false);
	}
	
	public void setLastWakeTime(long time) {
		mPref.edit().putLong("last_wake_time", time).commit();
	}
	
	public long getLastWakeTime() {
		return mPref.getLong("last_wake_time", 0);
	}
	
   public void setIsTravelMode(boolean isTravelMode) {
      mPref.edit().putBoolean("state_travelmode", isTravelMode).commit();
   }
	
   public boolean isTravelMode() {
      return mPref.getBoolean("state_travelmode", false);
   }

   public void setUseApnDroid(boolean use) {
      mPref.edit().putBoolean("apndroid", use).commit();
   }
   
   public boolean isDisconnectOnScreenOff() {
      return mPref.getBoolean("state_disconnect_on_screen_off", false);
   }
   
   public void setDisconnectOnScreenOff(boolean disable) {
      mPref.edit().putBoolean("state_disconnect_on_screen_off", disable).commit();
   }

   public void setScreenOnOff(boolean screenOnOff)
   {
       mPref.edit().putBoolean("screen_state_on", screenOnOff).commit();
   }

   public boolean getScreenOnOff() {
       return mPref.getBoolean("screen_state_on", true);
   }

    public long getLastRun() {
        return mPref.getLong("last_run", 0);
    }

    public void setLastRun(long run)
    {
        mPref.edit().putLong("last_run", run).commit();
    }
}
