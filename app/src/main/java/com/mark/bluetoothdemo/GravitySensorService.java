package com.mark.bluetoothdemo;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

/**
 * Created on 16/05/2017
 *
 * @author Mark Hsu
 */

class GravitySensorService {
    private static final String TAG = "GravitySensorService";
    private Handler mHandler;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private GravitySensorListener mSensorListener;

    GravitySensorService(SensorManager sensorManager, Handler handler) {
        Log.d(TAG, "Start sensor service.");
        mHandler = handler;
        mSensorManager = sensorManager;

        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorListener = new GravitySensorListener();
        mSensorManager.registerListener(mSensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    void cancel() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorListener);
        }
        Log.d(TAG, "Unregistered sensor listener.");
    }

    private class GravitySensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float gravity[] = new float[3];
            double vx = 0, vy = 0, vz = 0;
            int vt;

            gravity[0] = event.values[0];
            gravity[1] = event.values[1];
            gravity[2] = event.values[2];
            Log.d(TAG, "x: " + gravity[0] + "\ty: " + gravity[1] + "\tz: " + gravity[2]);

            vx += gravity[0] * 10;
            vy += gravity[1] * 10;
            vz += gravity[2] * 10;

            vt = (int) Math.sqrt(vx * vx + vy * vy + vz * vz);
            Log.d(TAG, "Vt = " + vt);

            mHandler.obtainMessage(Constants.SENSOR_GRAVITY_RESULT, vt, -1, gravity).sendToTarget();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "AcceSensorListener#onAccuracyChanged");
        }
    }
}
