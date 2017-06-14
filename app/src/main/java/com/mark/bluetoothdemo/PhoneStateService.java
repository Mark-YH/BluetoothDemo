package com.mark.bluetoothdemo;

import android.content.Context;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;

/**
 * Created on 14/06/2017
 *
 * @author Mark Hsu
 */

class PhoneStateService {
    private final static String TAG = "PhoneStateService";
    private final TelephonyManager telephonyManager;
    private final MyPhoneStateListener phoneStateListener;
    private static boolean isListening = false;
    private Handler mHandler;

    PhoneStateService(Context context, Handler handler) {
        mHandler = handler;
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new MyPhoneStateListener();
        Log.i(TAG, "Initialized PhoneStateService");
    }

    void start() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        isListening = true;
        Log.i(TAG, "Started listening phone state");
    }

    void stop() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        isListening = false;
        Log.i(TAG, "Stopped listening phone state");
    }

    /**
     * @return true if listening phone state
     */
    boolean isListening() {
        return isListening;
    }

    private class MyPhoneStateListener extends PhoneStateListener {

        MyPhoneStateListener() {
            super();
            Log.i(TAG, "Initialized phone state listener");
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.i(TAG, "onCallStateChanged()");
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i(TAG, "incoming number: " + incomingNumber);
                    try {
                        Method m1 = telephonyManager.getClass().getDeclaredMethod("getITelephony");
                        if (m1 != null) {
                            m1.setAccessible(true);
                            ITelephony iTelephony = (ITelephony) m1.invoke(telephonyManager);

                            if (iTelephony != null) {
                                // silenceRinger method got a exception, java.lang.reflect.InvocationTargetException
                                // Caused by: java.lang.SecurityException: Neither user 10166 nor current process has android.permission.MODIFY_PHONE_STATE.
                                // *** I think it might need rooted device to do this method. ***
//                                Method m2 = iTelephony.getClass().getDeclaredMethod("silenceRinger");
//
//                                if (m2 != null) {
//                                    m2.invoke(iTelephony);
//                                }

                                Method m3 = iTelephony.getClass().getDeclaredMethod("endCall");

                                if (m3 != null) {
                                    // this code might throw a exception,
                                    // but just disable *Phone permission* in Settings and enable it again,
                                    // then it could works.
                                    m3.invoke(iTelephony);
                                    SmsManager sms = SmsManager.getDefault();
                                    sms.sendTextMessage(incomingNumber, null, "騎車中，稍後回覆", null, null);
                                    mHandler.obtainMessage(Constants.PHONE_SERVICE_RESULT,
                                            -1, -1, incomingNumber).sendToTarget();
                                }
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "--- SMS IllegalArgumentException ---", e);
                        mHandler.obtainMessage(Constants.PHONE_SERVICE_ERROR, 1, -1, e).sendToTarget();
                    } catch (Exception e) {
                        Log.e(TAG, "--- Handle phone call exception ---", e);
                        mHandler.obtainMessage(Constants.PHONE_SERVICE_ERROR, -1, -1, e).sendToTarget();
                    }
                    break;
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    }
}
