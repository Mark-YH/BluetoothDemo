package com.mark.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;


/**
 * Created on 2017/4/16
 * Do all stuff about bluetooth.
 *
 * @author Mark Hsu
 */

class BluetoothService {
    private static final String TAG = "BluetoothService"; // for debug
    private Handler mHandler; // handler that gets info from Bluetooth service
    private BluetoothAdapter mBluetoothAdapter;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // UUID for Arduino serial port

    BluetoothService(Handler handler, String address) {
        mHandler = handler;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        Log.i(TAG, "startConnectThread() Received device:" + String.valueOf(device));
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    void disconnect() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        Message disconnectedMsg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Disconnected!");
        disconnectedMsg.setData(bundle);
        disconnectedMsg.sendToTarget();
    }

    void sendMessage(String msg) {
        if (connectedThread != null) {
            connectedThread.write(msg.getBytes());
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final static int PACKET_LENGTH = 20; // 自訂封包的長度

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    int numBytes = 0; // bytes returned from read()
                    byte[] mmBuffer = new byte[PACKET_LENGTH];

                    // 等到 input stream 內確定讀取到 PACKET_LENGTH bytes 後才跳出 while loop
                    while (numBytes < PACKET_LENGTH) {
                        numBytes = mmInStream.available();
                    }
                    // 已確定 input stream 內有完整封包長度的封包, 可忽略 InputStream.read(byte[]) 的回傳值
                    // 但是還不確定這個封包格式是否符合自訂的標準封包
                    mmInStream.read(mmBuffer);

                    // 自訂標準為 前 4 bytes = 'H', 'E', 'A', 'D' 作為 Header 來辨識
                    // 因此先將 buffer 內前 4 bytes 讀出來
                    byte[] header = new byte[4];
                    ByteBuffer.wrap(mmBuffer, 0, 4).get(header, 0, 4);

                    if (new String(header).equals("HEAD")) {
                        // Packet is correct, so send to MainActivity
                        Message readMsg = mHandler.obtainMessage(Constants.MESSAGE_READ, mmBuffer);
                        readMsg.sendToTarget();
                    } else {
                        // 封包格式錯誤, 使用 InputStream.read() 來丟棄(drop) 1 byte
                        mmInStream.read();
                        Log.e(TAG, "PACKET IS WRONG!!!!!");
                    }

                    // for debug
//                    if (numBytes > PACKET_LENGTH) {
//                        Log.e(TAG, " ******\n numBytes > PACKET_LENGTH\n numBytes= " + String.valueOf(numBytes) + "\n *****");
//                    }
//                    Log.d(TAG, "numBytes:" + String.valueOf(numBytes));
//                    for (byte item : mmBuffer) {
//                        Log.d(TAG, "item:" + item);
//                    }
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        Constants.MESSAGE_WRITE, bytes);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(Constants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Send a failure message back to the activity.
                mHandler.sendEmptyMessage(Constants.MESSAGE_CONNECT_ERR);

                Log.e(TAG, "Could not connect to bluetooth device", connectException);

                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            Log.i(TAG, "---Socket Connected!---");

            mHandler.sendEmptyMessage(Constants.MESSAGE_CONNECTED);
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.start();
        }

        // Closes the client socket and causes the thread to finish.
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
}
