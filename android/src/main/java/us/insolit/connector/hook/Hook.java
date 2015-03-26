package us.insolit.connector.hook;

import java.util.HashMap;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEventListener;
import android.os.Handler;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Hook implements IXposedHookLoadPackage {
    private HashMap<SensorEventListener, CompassEventListener> cache = new HashMap<SensorEventListener, CompassEventListener>();
    private CompassEventListener getDummy(SensorEventListener listener) {
        synchronized (this) {
            if (cache.containsKey(listener)) {
                return cache.get(listener);
            } else {
                CompassEventListener dummy = new CompassEventListener(listener);
                cache.put(listener, dummy);
                return dummy;
            }
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpParam) {
        if (!lpParam.packageName.equals("com.google.android.apps.maps")) {
          return;
        }

        XposedBridge.log("Loaded app: " + lpParam.packageName);

        findAndHookMethod(
            "android.hardware.SensorManager", lpParam.classLoader,
            "registerListener", SensorEventListener.class, Sensor.class, int.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    SensorManager sensorManager = (SensorManager)param.thisObject;
                    SensorEventListener listener = (SensorEventListener)param.args[0];
                    Sensor sensor = (Sensor)param.args[1];
                    Integer samplingPeriodUs = (Integer)param.args[2];

                    XposedBridge.log("registerListener(" + sensor + " , " + samplingPeriodUs + ")");

                    if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                        XposedBridge.log("Found a magnetic field sensor");
                        param.args[0] = getDummy(listener);
                    } else if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                        XposedBridge.log("Found a rotation vector sensor");
                        param.args[0] = getDummy(listener);
                    } else if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
                        XposedBridge.log("Found an orientation sensor");
                        param.args[0] = getDummy(listener);
                    } else {
                        XposedBridge.log("Found an unknown sensor, type = " + sensor.getType());
                    }
                }
          });

        findAndHookMethod(
            "android.hardware.SensorManager", lpParam.classLoader,
            "unregisterListener", SensorEventListener.class, Sensor.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    SensorManager sensorManager = (SensorManager)param.thisObject;
                    SensorEventListener listener = (SensorEventListener)param.args[0];

                    synchronized (this) {
                        if (cache.containsKey(listener)) {
                            param.args[0] = cache.get(listener);
                        }
                    }
                }
          });

        findAndHookMethod(
            "android.hardware.SensorManager", lpParam.classLoader,
            "unregisterListener", SensorEventListener.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    SensorEventListener listener = (SensorEventListener)param.args[0];

                    synchronized (this) {
                        if (cache.containsKey(listener)) {
                            param.args[0] = cache.get(listener);
                        }
                    }
                }
          });
    }
}
