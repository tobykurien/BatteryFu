package com.tobykurien.batteryfu;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

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

    private static Settings mInstance;
    private Context mContext;

	private Settings(Context context) {
        mContext = context.getApplicationContext();
    }
    
    private synchronized SharedPreferences getPreferences()
    {
        SharedPreferences preferences;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            preferences = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
        }
        else
        {
            preferences = mContext.getSharedPreferences(PREFS_NAME, 0);
        }
        return preferences;  
    }

	public static synchronized Settings getSettings(Context context) {
        if(mInstance == null)
        {
            mInstance = new Settings(context);
        }
      return mInstance;
	}
	
	public boolean isDnsFix() {
		return getPreferences().getBoolean("dns_fix", false);		
	}

   public void setIsDnsFix(boolean dnsFix) {
      getPreferences().edit().putBoolean("dns_fix", dnsFix).commit();
   }
	
	public boolean isEnabled() {
        return getPreferences().getBoolean("enabled", false);
	}

   public boolean isForceSync() {
      return getPreferences().getBoolean("force_sync", true);
   }
	
   public boolean isNightmodeOnly() {
      if (!isNightmodeEnabled()) return false;
      return getPreferences().getBoolean("night_mode_only", false);
   }

   public String getNightmodeStart() {
		return getNightmodeStart(Settings.DEFAULT_NIGHT_MODE_START);
	}

	public String getNightmodeStart(String defaultVal) {
		return getPreferences().getString("night_mode_start", defaultVal);
	}	
	
	public String getNightmodeEnd() {
		return getPreferences().getString("night_mode_end", Settings.DEFAULT_NIGHT_MODE_END);
	}
	
	public boolean isDataWhileScreenOn() {
		return getPreferences().getBoolean("screen_on_data", false);
	}

	public boolean isScreenOnKeepData() {
      return getPreferences().getBoolean("screen_keep_data", true);
   }

	public boolean isMobileDataEnabled() {
		return getPreferences().getBoolean("mobile", false);
	}

	public boolean isDataWhileCharging() {
		return getPreferences().getBoolean("charger_on_data", false);
	}
	
	public boolean isNightmodeEnabled() {
		return getPreferences().getBoolean("night_mode_enabled", false);
	}

	public boolean isWifiEnabled() {
		return getPreferences().getBoolean("wifi", true);
	}

	public String getScreenOffDelayTime() {
		return getPreferences().getString("screen_off_sleep_time", "30");
	}

	public boolean isWaitForScreenUnlock() {
		return getPreferences().getBoolean("screen_on_unlock", true);
	}

	public boolean isStartOnBoot() {
		return getPreferences().getBoolean("start_on_boot", true);
	}

	public String getSleepTime() {
		return getPreferences().getString("sleep_time", Settings.DEFAULT_SLEEP);
	}

	public String getAwakeTime() {
		return getPreferences().getString("awake_time", Settings.DEFAULT_AWAKE);
	}

	public boolean isShowNotification() {
		return getPreferences().getBoolean("show_notification", true);
	}

	public boolean isUseApndroid() {
		return getPreferences().getBoolean("apndroid", false);
	}

   public boolean isFirstRun() {
      return getPreferences().getInt("first_run", 0) != FIRST_RUN_VERSION;
   }

   public void setFirstRun() {
      getPreferences().edit().putInt("first_run", FIRST_RUN_VERSION).commit();     
   }
   
	public void setNightmodeStart(String start) {
		getPreferences().edit().putString("night_mode_start", start).commit();		
	}

	public void setNightmodeEnd(String end) {
		getPreferences().edit().putString("night_mode_end", end).commit();		
	}

	public void setEnabled(boolean enabled) {
		getPreferences().edit().putBoolean("enabled", enabled).commit();
	}

	public boolean isDataOn() {
		return getPreferences().getBoolean("data_state_on", true);
	}

	public void setDataStateOn(boolean on) {
		getPreferences().edit().putBoolean("data_state_on", on).commit();
	}

	public void setSyncOnData(boolean sync) {
		getPreferences().edit().putBoolean("sync_on_data", sync).commit();
	}

	public boolean isSyncOnData() {
		return getPreferences().getBoolean("sync_on_data", false);
	}

	public void setIsCharging(boolean isCharging) {
		getPreferences().edit().putBoolean("state_charging", isCharging).commit();
	}
	
	public boolean isCharging() {
		return getPreferences().getBoolean("state_charging", false);
	}
	
	public void setIsNightmode(boolean isNightmode) {
		getPreferences().edit().putBoolean("state_nightmode", isNightmode).commit();
	}
	
	public boolean isNightmode() {
		return getPreferences().getBoolean("state_nightmode", false);
	}
	
	public void setLastWakeTime(long time) {
		getPreferences().edit().putLong("last_wake_time", time).commit();
	}
	
	public long getLastWakeTime() {
		return getPreferences().getLong("last_wake_time", 0);
	}
	
   public void setIsTravelMode(boolean isTravelMode) {
      getPreferences().edit().putBoolean("state_travelmode", isTravelMode).commit();
   }
	
   public boolean isTravelMode() {
      return getPreferences().getBoolean("state_travelmode", false);
   }

   public void setUseApnDroid(boolean use) {
      getPreferences().edit().putBoolean("apndroid", use).commit();
   }
   
   public boolean isDisconnectOnScreenOff() {
      return getPreferences().getBoolean("state_disconnect_on_screen_off", false);
   }
   
   public void setDisconnectOnScreenOff(boolean disable) {
      getPreferences().edit().putBoolean("state_disconnect_on_screen_off", disable).commit();
   }

   public void setScreenOnOff(boolean screenOnOff)
   {
       getPreferences().edit().putBoolean("screen_state_on", screenOnOff).commit();
   }

   public boolean getScreenOnOff() {
       return getPreferences().getBoolean("screen_state_on", true);
   }

    public long getLastRun() {
        return getPreferences().getLong("last_run", 0);
    }

    public void setLastRun(long run)
    {
        getPreferences().edit().putLong("last_run", run).commit();
    }
}
