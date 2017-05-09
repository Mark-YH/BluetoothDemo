package com.mark.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/**
 * Created on 2017/4/18
 * Select device which has been paired or found
 *
 * @author Mark Hsu
 */

public class DeviceListFragment extends Activity {
    private final static String TAG = "DeviceListFragment";
    private ArrayAdapter<String> pairedDevicesArrayAdapter, newDevicesArrayAdapter;
    private Button btnScan;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);
        Log.d(TAG, "----onCreate fragment activity----");
        setResult(RESULT_CANCELED); // In case user close this activity

        btnScan = (Button) findViewById(R.id.btnScan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.tvTitleNewDevice).setVisibility(View.VISIBLE);
                setButtonEnable(true);
                mBluetoothAdapter.startDiscovery();
            }
        });

        pairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        newDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        ListView lvPaired = (ListView) findViewById(R.id.lvPairedDevices);
        ListView lvNew = (ListView) findViewById(R.id.lvNewDevices);

        lvPaired.setAdapter(pairedDevicesArrayAdapter);
        lvNew.setAdapter(newDevicesArrayAdapter);

        lvPaired.setOnItemClickListener(mDeviceClickListener);
        lvNew.setOnItemClickListener(mDeviceClickListener);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        findPairedDevices();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            // 接收 "發出 enable bluetooth 的 intent" 後的 result
            switch (resultCode) {
                case RESULT_OK:
                    findPairedDevices();
                    break;
                case RESULT_CANCELED:
                    Toast.makeText(this, "Enable Bluetooth failed", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
            }
        }
    }

    private void findPairedDevices() {
        // Ensure that bluetooth is enabled
        if (mBluetoothAdapter == null) {
            // Device does not support bluetooth
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        } else {
            // Querying paired device
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else {
                pairedDevicesArrayAdapter.add(getResources().getString(R.string.noPairedDevice));
            }
        }
    }

    private void setButtonEnable(boolean isScanning) {
        ProgressBar loading = (ProgressBar) findViewById(R.id.progressBar_Scanning);
        if (isScanning) {
            loading.setVisibility(View.VISIBLE);
            btnScan.setVisibility(View.GONE);
        } else {
            loading.setVisibility(View.GONE);
            btnScan.setVisibility(View.VISIBLE);
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "---- onReceive ----");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.i(TAG, "---- BLUETOOTH ACTION FOUND ----");
                // Discovery has found a device. Get the BluetoothDevice object from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                newDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Discovery finished.
                setTitle(R.string.device_list_name);
                setButtonEnable(false);

                if (newDevicesArrayAdapter.getCount() == 0) {
                    newDevicesArrayAdapter.add(getResources().getString(R.string.noFoundDevice));
                }
            }
        }
    };

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            String item = ((TextView) view).getText().toString();
            String noFound = getResources().getString(R.string.noFoundDevice);
            String noPaired = getResources().getString(R.string.noPairedDevice);
            if (!item.equals(noFound) & !item.equals(noPaired)) {
                String address = item.substring(item.length() - 17);
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("address", address);
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    };
}
