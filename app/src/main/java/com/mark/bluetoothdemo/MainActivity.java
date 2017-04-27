package com.mark.bluetoothdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created on 2017/4/14
 * Connect to Arduino via Bluetooth demo
 * Send character 't' to Arduino and then receive DHT sensor data.
 *
 * @author Mark Hsu
 */


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity"; // for debug
    private TextView tvContent, tvWritten;
    private Button btnConnect, btnDisconnect, btnSend;
    private EditText etRequest;
    private BluetoothService btService;
    private ProgressBar pbLoading;

    // The REQUEST_ENABLE_BT constant passed to startActivityForResult() as request code
    // and it is a locally defined integer that must be greater than 0


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate MainActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // Hide virtual key board when startup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnSend = (Button) findViewById(R.id.btnSend);
        etRequest = (EditText) findViewById(R.id.etRequest);
        pbLoading = (ProgressBar) findViewById(R.id.progressBar);
        tvContent = (TextView) findViewById(R.id.tvContent);
        tvWritten = (TextView) findViewById(R.id.tvWritten);
        tvContent.setMovementMethod(new ScrollingMovementMethod());
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
                    btService = new BluetoothService(mHandler, bundle.getString("address"));
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
        tvContent.append("天色昏暗");
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
    }

    /**
     * Receive message that returned from BluetoothService object
     */
    private final Handler mHandler = new Handler() {
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
                    if (lightVal > 500) {
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