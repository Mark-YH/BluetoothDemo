package com.mark.bluetoothdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;

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
 * <p>
 * <p>
 * <p>
 * <p>
 * If you are currently using the android.location API, you are strongly encouraged to switch to the Google Location Services API as soon as possible.
 *
 * @author Mark Hsu
 */


public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "MainActivity"; // for debug
    private static boolean isLightOff = true;
    private TextView tvContent, tvWritten;
    private Button btnConnect, btnDisconnect, btnSend;
    private EditText etRequest;
    private ProgressBar pbLoading;
    private BluetoothService btService;
    private MyTextToSpeechService mTtsService;
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;

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
        tvContent.setMovementMethod(new ScrollingMovementMethod());

        mTtsService = new MyTextToSpeechService(MainActivity.this);
        buildGoogleApiClient();
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
        getAddress();
    }

    public void onClickLightSwitch(View v){
        isLightOff = !isLightOff;
    }

    private void turnOnLight() {
        mTtsService.speak("天色昏暗，請開啟大燈");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constants.REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                mGoogleApiClient.disconnect();
                mGoogleApiClient.connect();
            } else {
                mTtsService.speak("請同意應用程式存取位置資訊的權限");
                // Permission was denied or request was cancelled
            }
        }
    }

    public void getAddress() {
        if (mLastLocation != null) {
            Geocoder gc = new Geocoder(this, Locale.TRADITIONAL_CHINESE);
            //自經緯度取得地址
            List<Address> lstAddress;
            try {
                lstAddress = gc.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                String address = lstAddress.get(0).getAddressLine(0);
                // 不需要郵遞區號及國家名稱
                address = address.replace(lstAddress.get(0).getPostalCode(), "");
                address = address.replace(lstAddress.get(0).getCountryName(), "");

                mTtsService.speak("您目前位置是：" + address);
                Log.d(TAG, address);
            } catch (IOException e) {
                Log.e(TAG, "gc.getFromLocation() exception", e);
            }
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle bundle) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            // Check Permissions Now
            Log.d(TAG, "Needs Permissions to access location");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.REQUEST_LOCATION);
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.d(TAG, "Google API Client connected.");
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
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
                        // TODO: Arduino 端須感測大燈是否開啟, 並傳值過來. 否則只要昏暗就會一直提醒
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
}