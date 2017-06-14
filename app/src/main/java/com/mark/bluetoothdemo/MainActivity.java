package com.mark.bluetoothdemo;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * Created on 2017/4/14
 * Connect to Arduino via Bluetooth demo
 * Send character 't' to Arduino and then receive DHT sensor data.
 * <p>
 * Note: Since this class is based on the Google Play services client library,
 * make sure you install the latest version before using the sample apps or code snippets.
 * To learn how to set up the client library with the latest version,
 * see Setup in the Google Play services guide.
 * link: https://developers.google.com/android/guides/setup
 *
 * @author Mark Hsu
 */

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity"; // for debug
    private static boolean isLightOff = true;
    private TextView tvLightSensor, tvInput, tvOutput, tvGSensor;
    private Button btnConnect, btnDisconnect;
    private ProgressBar pbLoading;
    private BluetoothService mBtService;
    private MyTextToSpeechService mTtsService;
    private LocationService mLocationService;
    private SpeechToTextService mSttService;
    private OpenDataService mOpenDataService;
    private GravitySensorService mGravitySensorService;
    private PhoneStateService mPhoneStateService;

    private final static String[] permissions = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        pbLoading = (ProgressBar) findViewById(R.id.progressBar);
        tvInput = (TextView) findViewById(R.id.tvInput);
        tvOutput = (TextView) findViewById(R.id.tvOutput);
        tvLightSensor = (TextView) findViewById(R.id.tvLightSensor);
        tvGSensor = (TextView) findViewById(R.id.tvGravitySensor);

        checkPermission(MainActivity.this);
        mTtsService = new MyTextToSpeechService(MainActivity.this);
        mLocationService = new LocationService(MainActivity.this);
        mSttService = new SpeechToTextService(MainActivity.this, mSttHandler);
        mOpenDataService = new OpenDataService(mLocationService, mSttHandler);
        mGravitySensorService = new GravitySensorService((SensorManager) getSystemService(SENSOR_SERVICE), mGsHandler);
        mPhoneStateService = new PhoneStateService(MainActivity.this, mPhoneHandler);

        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);//"android.intent.action.MEDIA_BUTTON"
        MediaButtonIntentReceiver r = new MediaButtonIntentReceiver();
        registerReceiver(r, filter);
    }

    public class MediaButtonIntentReceiver extends BroadcastReceiver {

        public MediaButtonIntentReceiver() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
                return;
            }
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }
            int action = event.getAction();
            if (action == KeyEvent.ACTION_DOWN) {
                startSpeechToText();
            }
            abortBroadcast();
        }

    }

    @Override
    protected void onDestroy() {
        if (mTtsService != null) {
            mTtsService.destroy();
        }
        if (mGravitySensorService != null) {
            mGravitySensorService.cancel();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_SELECT_DEVICE) {
            switch (resultCode) {
                case RESULT_OK:
                    Bundle bundle = data.getExtras();
                    mBtService = new BluetoothService(mBtHandler, bundle.getString("address"));
                    pbLoading.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void doConnect() {
        if (mBtService != null) { // 若藍芽耳機主動連線會導致連線中斷，因此須中斷重連
            mBtService.disconnect();
        }
        Intent scanIntent = new Intent(MainActivity.this, DeviceListFragment.class);
        startActivityForResult(scanIntent, Constants.REQUEST_SELECT_DEVICE);
    }

    private void startSpeechToText() {
        if (checkPermission(MainActivity.this)) {
            mSttService.start();
        }
    }

    private void askLocation() {
        if (checkPermission(MainActivity.this)) {
            String speaking = "目前位置是：" + mLocationService.getAddress() + "\n";
            mTtsService.speak(speaking);
            tvOutput.append(speaking);
        }
    }

    private void askObstacle() {
        if (checkPermission(MainActivity.this)) {
            mOpenDataService.start();
        }
    }

    private void setButtonEnable(boolean isConnected) {
        btnConnect.setEnabled(!isConnected);
        btnDisconnect.setEnabled(isConnected);
    }

    private void autoScrollDown() {
        ScrollView writtenScrollView = (ScrollView) findViewById(R.id.scrollView_input);
        ScrollView logScrollView = (ScrollView) findViewById(R.id.scrollView_output);

        logScrollView.fullScroll(ScrollView.FOCUS_DOWN);
        writtenScrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    public void onClickConnect(View v) {
        doConnect();
    }

    public void onClickDisconnect(View v) {
        setButtonEnable(false);
        mBtService.disconnect();
    }

    public void onClickClear(View v) {
        tvInput.setText("");
        tvLightSensor.setText("");
        tvOutput.setText("");
        tvGSensor.setText("");
    }

    public void onClickSTT(View v) {
        startSpeechToText();
    }

    public void onClickLightSwitch(View v) {
        isLightOff = !isLightOff;
    }

    /**
     * @param context for ActivityCompat.checkSelfPermission(Context context, String permission)
     * @return true if application has the permission above
     */
    private static boolean checkPermission(Context context) {
        ArrayList<String> lostPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                lostPermissions.add(permission);
            }
        }
        if (lostPermissions.size() > 0) {
            Log.d(TAG, "Lost permissions: " + lostPermissions + " , request it now.");
            ActivityCompat.requestPermissions((Activity) context,
                    lostPermissions.toArray(new String[0]), // why array size is 0? see here: https://shipilev.net/blog/2016/arrays-wisdom-ancients/
                    Constants.REQUEST_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constants.REQUEST_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Got a permission: " + permissions[i]);
                } else {
                    Log.e(TAG, "Permission '" + permissions[i] + "' was denied or request was cancelled");
                }
            }
        }
    }

    /**
     * Receive message that returned from BluetoothService object
     */
    private final Handler mBtHandler = new Handler() {
        private long currentTime;
        private long lightRemindTime; // 昏暗提醒時間
        private long moveTime; // 模擬油門前進時間
        private static final int REMIND_DELAY = 5000; // millisecond

        @Override
        public void handleMessage(Message msg) {
            Context context = getApplicationContext();
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    currentTime = System.currentTimeMillis();
                    byte[] mmBuffer = (byte[]) msg.obj;
                    int lightVal, switchState;
                    StringBuilder sb = new StringBuilder();
                    lightVal = ByteBuffer.wrap(mmBuffer, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    switchState = ByteBuffer.wrap(mmBuffer, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    sb.append("光感測：").append(lightVal).append("\n");
                    if (switchState > 0) {
                        sb.append("模擬油門：").append("移動中").append("\n");
                        if (moveTime == 0) {
                            moveTime = currentTime;
                        }
                    } else {
                        sb.append("模擬油門：").append("停止").append("\n");
                        if (currentTime - moveTime > REMIND_DELAY && moveTime > 0) {
                            askLocation();
                            askObstacle();
                            moveTime = 0;
                        }
                    }
                    if (mPhoneStateService.isListening()) {
                        sb.append("來電自動轉接中");
                    } else {
                        sb.append("未監聽來電");
                    }
                    tvLightSensor.setText(sb.toString());

                    if (lightVal > 500 && isLightOff) {
                        if ((currentTime - lightRemindTime) > REMIND_DELAY) {
                            Log.d(TAG, "(currentTime - lightRemindTime) = " + (currentTime - lightRemindTime));
                            mTtsService.speak("天色昏暗，請開啟大燈");
                            tvOutput.append("天色昏暗，請開啟大燈\n");
                            lightRemindTime = currentTime;
                        }
                    }
                    break;

                case Constants.MESSAGE_TOAST:
                    // toast from BluetoothService object.
                    Bundle bundle;
                    bundle = msg.getData();
                    Toast.makeText(context, bundle.getString("toast"), Toast.LENGTH_SHORT).show();
                    break;

                case Constants.MESSAGE_CONNECTED:
                    // sock.connect() 連線成功
                    setButtonEnable(true);
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(context, getResources().getString(R.string.bluetooth_connected)
                            , Toast.LENGTH_SHORT).show();
                    mPhoneStateService.start();
                    break;

                case Constants.MESSAGE_CONNECT_ERR:
                    // sock.connect() 連線失敗
                    setButtonEnable(false);
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(context, getResources().getString(R.string.bluetooth_connect_failed)
                            , Toast.LENGTH_SHORT).show();
                    break;

                case Constants.MESSAGE_DISCONNECTED:
                    Toast.makeText(context, getResources().getString(R.string.bluetooth_disconnect)
                            , Toast.LENGTH_SHORT).show();
                    mPhoneStateService.stop();
                    break;
            } // end switch
        }// end handleMessage(msg)
    };

    private Handler mSttHandler = new Handler() {
        private final String[] fakeObstacle = {"在中山路110號有事故\n", "在民族路155號有障礙\n"};

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.STT_ERROR:
                    tvOutput.append(msg.obj + "\n");
                    mTtsService.speak((String) msg.obj);
                    break;

                case Constants.STT_ASK_LOCATION:
                    askLocation();
                    break;

                case Constants.STT_ASK_OBSTACLE:
                    askObstacle();
                    break;

                case Constants.STT_RESULT_OBSTACLE:
                    if (msg.arg1 == 0) {
                        // demo時模擬狀況
                        for (int i = 0; i < fakeObstacle.length; i++) {
                            tvOutput.append("第 " + (i + 1) + " 筆: ");
                            tvOutput.append(fakeObstacle[i]);
                            mTtsService.speak(fakeObstacle[i]);
                        }

                        // 正常情況
//                        tvOutput.append("無事故發生\n");
//                        mTtsService.speak("無事故發生");
                        break;
                    }
                    @SuppressWarnings("unchecked")
                    ArrayList<Obstacle> obstacles = (ArrayList<Obstacle>) msg.obj;
                    for (int i = 0; i < obstacles.size(); i++) {
                        tvOutput.append("第 " + (i + 1) + " 筆: ");
                        tvOutput.append(obstacles.get(i).getSpeaking());
                        mTtsService.speak(obstacles.get(i).getSpeaking());
                        Log.d(TAG, obstacles.get(i).getDetail());
                    }
                    break;
                case Constants.LOCATION_SERVICE_ERROR:
                    mTtsService.speak("請開啟定位服務後重試"); // 需開啟定位服務： 設定 -> 位置
                    break;
                case Constants.STT_RESULT_RECOGNITION:
                    tvInput.append("語音辨識結果: " + msg.obj + "\n");
                    break;
            }
            autoScrollDown();
        }
    };

    private Handler mGsHandler = new Handler() {
        private long currentTime;
        private long remindTime;
        private static final int REMIND_DELAY = 500; // millisecond

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.SENSOR_GRAVITY_RESULT:
                    String speaking = "偵測到車子倒地\n";
                    int vt = msg.arg1;
                    String result = "重力感測值：" + vt;
                    tvGSensor.setText(result);

                    currentTime = System.currentTimeMillis();

                    if (vt > 200 && (currentTime - remindTime > REMIND_DELAY)) {
                        mTtsService.speak(speaking);
                        tvOutput.append(speaking);
                        remindTime = currentTime;
                    }
                    break;
            }
        }
    };

    private Handler mPhoneHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.PHONE_SERVICE_RESULT:
                    String incomingNumber = (String) msg.obj;
                    tvOutput.append("來電號碼: " + incomingNumber + " 已自動轉接並已發送簡訊\n");
                    break;
                case Constants.PHONE_SERVICE_ERROR:
                    Exception e = (Exception) msg.obj;
                    if (msg.arg1 > 0) {
                        tvOutput.append("SMS IllegalArgumentException" + e.getMessage() + "\n");
                    } else {
                        tvOutput.append("Handle phone call exception" + e.getMessage() + "\n");
                    }
                    break;
            }
        }
    };
}