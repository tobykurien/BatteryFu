package com.tobykurien.batteryfu.data_switcher;

import java.lang.Runtime;
import java.io.DataOutputStream;
import java.io.IOException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

import com.tobykurien.batteryfu.BatteryFu;
import com.tobykurien.batteryfu.R;

/**
 * A Lollipop-compatible data switcher
 * @author andy
 */
public class LollipopSwitcher extends MobileDataSwitcher {
    private ConnectivityManager connMan;
    private Context context;

    @Override
    public void enableMobileData(Context context) {

        try {
            // Preform su to get root priviledges
            Process p = Runtime.getRuntime().exec("su");

            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("svc data enable\n");
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {
                    Log.d("BatteryFu", "Enabled Mobile Data");
                }
                else {
                    Log.d("BatteryFu", "Error enabling mobile data");
                }
            } catch (InterruptedException e) {
                Log.d("BatteryFu", "Error enabling mobile data");
            }
        } catch (IOException e) {
            Log.d("BatteryFu", "Error enabling mobile data");
        }

    }

    @Override
    public void disableMobileData(Context context) {
        try {
            // Preform su to get root privledges
            Process p = Runtime.getRuntime().exec("su");

            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("svc data disable\n");
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {
                    Log.d("BatteryFu", "Disabled Mobile Data");
                }
                else {
                    Log.d("BatteryFu", "Error disabling mobile data");
                }
            } catch (InterruptedException e) {
                Log.d("BatteryFu", "Error disabling mobile data");
            }
        } catch (IOException e) {
            Log.d("BatteryFu", "Error disabling mobile data");
        }
    }

    @Override
    public int isToggleWorking(Context context) {
        Log.d("BatteryFu", "isToggleWorking");
        if(init(context))
            return 0;
        else
            return R.string.apn_problem_text;
    }

    public boolean init(Context context) {
        Log.d("BatteryFu", "init in LollipopSwitcher");
        try {
            // Perform su to get root priviledges
            Process p = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {
                    // TODO Code to run on success
                    Log.d("BatteryFu", "Successfully got root rights in init!");
                    return true;
                }
                else {
                    // TODO Code to run on unsuccessful
                    Log.d("BatteryFu", "Error gaining root access");
                }
            } catch (InterruptedException e) {
                // TODO Code to run in interrupted exception
                Log.d("BatteryFu", "Error gaining root access");
            }
        } catch (IOException e) {
            // TODO Code to run in input/output exception
            Log.d("BatteryFu", "Error gaining root access");
        }
        Log.d("BatteryFu", "return false in init");
        return false;
    }

}