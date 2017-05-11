package com.mark.bluetoothdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
    private static final String TAG = "MainActivity"; // for debug
    private static boolean isLightOff = true;
    private TextView tvContent, tvWritten, tvLog;
    private Button btnConnect, btnDisconnect, btnSend;
    private EditText etRequest;
    private ProgressBar pbLoading;
    private BluetoothService btService;
    private MyTextToSpeechService mTtsService;
    private LocationService locationService;
    private SpeechToTextService speechToTextService;
    private OpenDataService openDataService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // Hide virtual keyboard when startup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnSend = (Button) findViewById(R.id.btnSend);
        etRequest = (EditText) findViewById(R.id.etRequest);
        pbLoading = (ProgressBar) findViewById(R.id.progressBar);
        tvContent = (TextView) findViewById(R.id.tvContent);
        tvWritten = (TextView) findViewById(R.id.tvWritten);
        tvLog = (TextView) findViewById(R.id.tvLog);

        mTtsService = new MyTextToSpeechService(MainActivity.this);
        locationService = new LocationService(MainActivity.this);
        speechToTextService = new SpeechToTextService(MainActivity.this, mSttHandler);
        openDataService = new OpenDataService(locationService, mSttHandler);
    }

    @Override
    protected void onDestroy() {
        if (mTtsService != null) {
            mTtsService.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // receive result of intent
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            // 接收 "發出 enable bluetooth 的 intent" 後的 result
            switch (resultCode) {
                case RESULT_OK:
                    doConnect();
                    break;
                case RESULT_CANCELED:
                    Toast.makeText(this, "Enable Bluetooth failed", Toast.LENGTH_SHORT).show();
                    break;
            }
        } else if (requestCode == Constants.REQUEST_SELECT_DEVICE) {
            switch (resultCode) {
                case RESULT_OK:
                    Bundle bundle = data.getExtras();
                    btService = new BluetoothService(mBtHandler, bundle.getString("address"));
                    pbLoading.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void doConnect() {
        Intent scanIntent = new Intent(MainActivity.this, DeviceListFragment.class);
        startActivityForResult(scanIntent, Constants.REQUEST_SELECT_DEVICE);
    }

    private void setButtonEnable(boolean isConnected) {
        btnConnect.setEnabled(!isConnected);
        btnSend.setEnabled(isConnected);
        btnDisconnect.setEnabled(isConnected);
    }

    private void turnOnLight() {
        mTtsService.speak("天色昏暗，請開啟大燈");
    }

    public void onClickConnect(View v) {
        doConnect();
    }

    public void onClickSend(View v) {
        btService.sendMessage(etRequest.getText().toString());
    }

    public void onClickDisconnect(View v) {
        setButtonEnable(false);
        btService.disconnect();
    }

    public void onClickClear(View v) {
        tvWritten.setText("");
        tvContent.setText("");
        tvLog.setText("");
    }

    public void onClickSTT(View v) { //TODO: earphone button control >>> http://stackoverflow.com/questions/6287116/android-registering-a-headset-button-click-with-broadcastreceiver
        if (checkPermission(Constants.STT_REQUEST_PERMISSION, Manifest.permission.RECORD_AUDIO)) {
            speechToTextService.start();
        }
    }

    public void onClickLightSwitch(View v) {
        isLightOff = !isLightOff;
    }

    /**
     * @param requestCode Constant to verify on result (onRequestPermissionsResult())
     * @param permission  Manifest.permission.WHAT_PERMISSION_YOU_NEED
     * @return true if application has the permission above
     */
    private boolean checkPermission(int requestCode, String permission) {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            // Check Permissions Now
            Log.d(TAG, "request this permission: " + permission);
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.REQUEST_LOCATION_PERMISSION:
                if (grantResults.length == 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We can now safely use the API we requested access to
                    locationService.buildGoogleApiClient();
                    Log.d(TAG, "(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)");
                } else {
                    mTtsService.speak("請同意應用程式存取位置資訊的權限");
                    Log.d(TAG, "Permission was denied or request was cancelled");
                    // Permission was denied or request was cancelled
                }
                break;
            case Constants.STT_REQUEST_PERMISSION:
                if (grantResults.length == 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We can now safely use the API we requested access to
                    speechToTextService.initialize();
                    Log.d(TAG, "(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)");
                } else {
                    mTtsService.speak("請同意應用程式存取麥克風的權限");
                    Log.d(TAG, "Permission was denied or request was cancelled");
                }
                break;
        }
    }

    /**
     * Receive message that returned from BluetoothService object
     */
    private final Handler mBtHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Context context = getApplicationContext();
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    byte[] mmBuffer = (byte[]) msg.obj;
                    int lightVal;
                    float humidity, temperature, heatIndex;
                    String contents;
                    lightVal = ByteBuffer.wrap(mmBuffer, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    humidity = ByteBuffer.wrap(mmBuffer, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    temperature = ByteBuffer.wrap(mmBuffer, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    heatIndex = ByteBuffer.wrap(mmBuffer, 16, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    contents = ("光感測：" + lightVal + "\n" +
                            "濕度：" + humidity + "\n" +
                            "溫度：" + temperature + "\n" +
                            "熱指數：" + heatIndex + "\n" +
                            "---- ---- ---- ----\n");
                    tvContent.append(contents);
                    if (lightVal > 500 && isLightOff) {
                        turnOnLight();
                    }
                    break;

                case Constants.MESSAGE_WRITE: // display the message you sent
                    byte[] bytes = (byte[]) msg.obj;
                    String sentMsg = new String(bytes);
                    tvWritten.append("Written message: " + sentMsg + "\n");
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
                    Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show();
                    break;

                case Constants.MESSAGE_CONNECT_ERR:
                    // sock.connect() 連線失敗
                    setButtonEnable(false);
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(context, "Could not connect to bluetooth device", Toast.LENGTH_SHORT).show();
                    break;
            } // end switch
        }// end handleMessage(msg)
    };

    private Handler mSttHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.STT_ERROR:
                    tvLog.append("STT ERR: " + msg.obj + "\n");
                    break;
                case Constants.STT_ASK_LOCATION:
                    if (checkPermission(Constants.REQUEST_LOCATION_PERMISSION, Manifest.permission.ACCESS_FINE_LOCATION)
                            && checkPermission(Constants.REQUEST_LOCATION_PERMISSION,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        mTtsService.speak("目前位置是：" + locationService.getAddress());
                    }
                    break;
                case Constants.STT_ASK_OBSTACLE:
                    if (checkPermission(Constants.REQUEST_LOCATION_PERMISSION, Manifest.permission.ACCESS_FINE_LOCATION)
                            && checkPermission(Constants.REQUEST_LOCATION_PERMISSION,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        openDataService.start();
                    }
                    break;
                case Constants.STT_RESULT_OBSTACLE:
                    @SuppressWarnings("unchecked")
                    ArrayList<String> obstacle = (ArrayList<String>) msg.obj;
                    for (String item : obstacle) {
                        tvLog.append(item);
                    }
                    break;
                case Constants.LOCATION_SERVICE_ERROR:
                    mTtsService.speak("請開啟定位服務後重試"); // 需開啟定位服務： 設定 -> 位置
                    break;
            }
        }
    };
}