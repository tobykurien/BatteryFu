package com.tobykurien.batteryfu;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Timer;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

public class BattServiceInfo {
   private Class clazz = null;
   private Object inst = null;
   private SparseArray<? extends Object> stats = null;

   // Include all of the data in the stats, including previously saved data.
   public static final int STATS_TOTAL = 0;
   // Include only the last run in the stats.
   public static final int STATS_LAST = 1;
   // Include only the current run in the stats.
   public static final int STATS_CURRENT = 2;
   // Include only the run since the last time the device was unplugged in the
   // stats.
   public static final int STATS_UNPLUGGED = 3;

   // screen on type
   public static final int SCREEN_BRIGHTNESS_DARK = 0;
   public static final int SCREEN_BRIGHTNESS_DIM = 1;
   public static final int SCREEN_BRIGHTNESS_MEDIUM = 2;
   public static final int SCREEN_BRIGHTNESS_LIGHT = 3;
   public static final int SCREEN_BRIGHTNESS_BRIGHT = 4;

   public BattServiceInfo(Context paramContext) {
      try {
         ClassLoader cl = paramContext.getClassLoader();
         this.clazz = cl.loadClass("com.android.internal.os.BatteryStatsImpl");
         Class localClass1 = cl.loadClass("android.os.ServiceManager");
         Class[] arrayOfClass1 = new Class[1];
         arrayOfClass1[0] = String.class;
         Method localMethod1 = localClass1.getMethod("getService", arrayOfClass1);
         Object[] arrayOfObject1 = new Object[1];
         arrayOfObject1[0] = "batteryinfo";
         IBinder localIBinder = (IBinder) localMethod1.invoke(localClass1, arrayOfObject1);
         Class localClass2 = cl.loadClass("com.android.internal.app.IBatteryStats$Stub");
         Class[] arrayOfClass2 = new Class[1];
         arrayOfClass2[0] = IBinder.class;
         Method localMethod2 = localClass2.getMethod("asInterface", arrayOfClass2);
         Object[] arrayOfObject2 = new Object[1];
         arrayOfObject2[0] = localIBinder;
         Object localObject = localMethod2.invoke(localClass2, arrayOfObject2);
         byte[] arrayOfByte = (byte[]) cl.loadClass("com.android.internal.app.IBatteryStats").getMethod("getStatistics", new Class[0])
                  .invoke(localObject, new Object[0]);
         Parcel localParcel = Parcel.obtain();
         localParcel.unmarshall(arrayOfByte, 0, arrayOfByte.length);
         localParcel.setDataPosition(0);
         Class localClass3 = cl.loadClass("com.android.internal.os.BatteryStatsImpl");
         this.inst = ((Parcelable.Creator) localClass3.getField("CREATOR").get(localClass3)).createFromParcel(localParcel);
         return;
      } catch (IllegalArgumentException localIllegalArgumentException) {
         while (true)
            this.inst = null;
      } catch (ClassNotFoundException localClassNotFoundException) {
         while (true)
            this.inst = null;
      } catch (Exception localException) {
         while (true)
            this.inst = null;
      }
   }

   private void collectUidStats() {
      try {
         this.stats = ((SparseArray) this.clazz.getMethod("getUidStats", new Class[0]).invoke(this.inst, new Object[0]));
         return;
      } catch (IllegalArgumentException localIllegalArgumentException) {
         throw localIllegalArgumentException;
      } catch (Exception localException) {
         while (true)
            this.stats = null;
      }
   }

   public Long computeBatteryUptime(long paramLong, int paramInt) {
      new Long(0L);
      try {
         Class[] arrayOfClass = new Class[2];
         arrayOfClass[0] = Long.TYPE;
         arrayOfClass[1] = Integer.TYPE;
         Method localMethod = this.clazz.getMethod("computeBatteryUptime", arrayOfClass);
         Object[] arrayOfObject = new Object[2];
         arrayOfObject[0] = new Long(paramLong);
         arrayOfObject[1] = new Integer(paramInt);
         Long localLong = (Long) localMethod.invoke(this.inst, arrayOfObject);
         return localLong;
      } catch (IllegalArgumentException localIllegalArgumentException) {
         throw localIllegalArgumentException;
      } catch (Exception localException) {
         return new Long(0L);
      }
   }

   public Long computeUptime(long paramLong, int paramInt) {
      new Long(0L);
      try {
         Class[] arrayOfClass = new Class[2];
         arrayOfClass[0] = Long.TYPE;
         arrayOfClass[1] = Integer.TYPE;
         Method localMethod = this.clazz.getMethod("computeUptime", arrayOfClass);
         Object[] arrayOfObject = new Object[2];
         arrayOfObject[0] = new Long(paramLong);
         arrayOfObject[1] = new Integer(paramInt);
         Long localLong = (Long) localMethod.invoke(this.inst, arrayOfObject);
         return localLong;
      } catch (IllegalArgumentException localIllegalArgumentException) {
         throw localIllegalArgumentException;
      } catch (Exception localException) {
         return new Long(0L);
      }
   }

   public Long getScreenOnTime(long batteryRealtime, int which) {
      new Long(0L);
      try {
         Class[] arrayOfClass = new Class[2];
         arrayOfClass[0] = Long.TYPE;
         arrayOfClass[1] = Integer.TYPE;
         Method localMethod = this.clazz.getMethod("getScreenOnTime", arrayOfClass);
         Object[] arrayOfObject = new Object[2];
         arrayOfObject[0] = new Long(batteryRealtime);
         arrayOfObject[1] = new Integer(which);
         Long localLong = (Long) localMethod.invoke(this.inst, arrayOfObject);
         return localLong;
      } catch (IllegalArgumentException localIllegalArgumentException) {
         throw localIllegalArgumentException;
      } catch (Exception localException) {
         return new Long(0L);
      }
   }

   public int getDischargeCurrentLevel() {
      try {
         int j = ((Integer) this.clazz.getMethod("getDischargeCurrentLevel", new Class[0]).invoke(this.inst, new Object[0])).intValue();
         return j;
      } catch (IllegalArgumentException localIllegalArgumentException) {
         throw localIllegalArgumentException;
      } catch (Exception localException) {
         return 0;
      }
   }

   public int getDischargeAmountScreenOffSinceCharge() {
      try {
         int j = ((Integer) this.clazz.getMethod("getDischargeAmountScreenOffSinceCharge", new Class[0]).invoke(this.inst, new Object[0]))
                  .intValue();
         return j;
      } catch (IllegalArgumentException localIllegalArgumentException) {
         throw localIllegalArgumentException;
      } catch (Exception localException) {
         return 0;
      }
   }

   public Map<String, ? extends Timer> getKernelWakelockStats() {
      try {
         return ((Map<String, ? extends Timer>) this.clazz.getMethod("getKernelWakelockStats", new Class[0]).invoke(this.inst,
                  new Object[0]));
      } catch (IllegalArgumentException localIllegalArgumentException) {
         throw localIllegalArgumentException;
      } catch (Exception localException) {
         return null;
      }
   }

   public long getTotalTimeLocked(Object timer, long batteryRealtime, int which) {
      long retVal = 0;

      try {
         Class[] arrayOfClass = new Class[2];
         arrayOfClass[0] = Long.TYPE;
         arrayOfClass[1] = Integer.TYPE;
         Method m = timer.getClass().getMethod("getTotalTimeLocked", arrayOfClass);
         retVal = (Long) m.invoke(timer, batteryRealtime, which);
      } catch (Exception e) {
         throw new IllegalStateException(e);
      }

      return retVal;
   }

   public boolean getIsOnBattery(Context paramContext) {
      try {
         boolean bool2 = ((Boolean) this.clazz.getMethod("getIsOnBattery", new Class[0]).invoke(this.inst, new Object[0])).booleanValue();
         return bool2;
      } catch (IllegalArgumentException localIllegalArgumentException) {
         throw localIllegalArgumentException;
      } catch (Exception localException) {
         return false;
      }
   }

}
