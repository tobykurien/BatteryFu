package com.tobykurien.batteryfu;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.tobykurien.android.Utils;
import com.tobykurien.batteryfu.data_switcher.MobileDataSwitcher;

/**
 * Created by andy on 29/01/15.
 */
public class DataService extends IntentService {

    public DataService() {
        super("DataService working");
        Log.d("BatteryFu", "Starting DataService to handle command");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean force = intent.getBooleanExtra("force", false);
        final Context context = getApplicationContext();
        final Settings settings = Settings.getSettings(context);


        if(intent.getStringExtra("action").equals("disable"))
        {
            BatteryFu.checkApnDroid(context, settings);
            if (!force) {
                // if (!settings.isDataOn()) {
                // MainFunctions.showNotification(context, settings,
                // "DEBUG: Data is already off");
                // return true;
                // }

                if (settings.isDataWhileCharging() && settings.isCharging()) {
                    MainFunctions.showNotification(context, settings, context.getString(R.string.data_switched_on_while_charging));
                    return;
                }

                if (settings.isScreenOnKeepData() && ScreenService.isScreenOn(context)) {
                    // MainFunctions.showNotification(context, settings,
                    // "Data kept on, waiting for screen to switch off");
                    settings.setDisconnectOnScreenOff(true);
                    return;
                }

                if (settings.isDataWhileScreenOn() && ScreenService.isScreenOn(context)) {
                    MainFunctions.showNotification(context, settings, context.getString(R.string.data_switched_on_while_screen_is_on));
                    return;
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
        }
        else if(intent.getStringExtra("action").equals("enable"))
        {
            boolean forceMobile = intent.getBooleanExtra("forceMobile", false);
            final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // wait a bit for wifi to connect, and if not connected, connect mobile data
            BatteryFu.checkApnDroid(context, settings);

            if (force) {
                if (Utils.isNetworkConnected(context)) {
                    // do the sync now
                    MainFunctions.startSync(context);
                } else {
                    // sync once connected
                    settings.setSyncOnData(true);
                }
            }

            if (!settings.isDataOn()) {
                // save data state
                settings.setDataStateOn(true);

                // clear any previous notifications
                //nm.cancel(NOTIFICATION_CONNECTIVITY);

                // enable wifi
                if (settings.isWifiEnabled() && !settings.isTravelMode()) {
                    Log.i("BatteryFu", "DataToggler enabling wifi");
                    final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    wm.setWifiEnabled(true);
                    wm.startScan();
                    wm.reconnect();

                    if (!forceMobile) {
                        try {
                            Thread.sleep(10000); // wait 10 seconds
                        } catch (InterruptedException e) {
                        }

                        if (wm.getConnectionInfo() == null || wm.getConnectionInfo().getNetworkId() < 0) {
                            Log.i("BatteryFu", "Wifi not connected after timeout, enabling mobile data");
                            connectMobileData(context, settings);
                        }
                    } else {
                        // also connect mobile data
                        connectMobileData(context, settings);
                    }
                } else {
                    Log.d("BatteryFu", "Wifi toggling disabled");
                    connectMobileData(context, settings);
                }
            }
        }
    }

    private static void connectMobileData(final Context context, final Settings settings) {
        // turn on mobile data
        if (settings.isMobileDataEnabled()) {
            MobileDataSwitcher.enableMobileData(context, settings);
        } else {
            Log.d("BatteryFu", "Mobile data toggling disabled");
        }
    }
}
