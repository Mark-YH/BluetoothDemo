package com.mark.bluetoothdemo;

/**
 * Created on 2017/4/16.
 * Defines several constants used when
 * transmitting messages between the service and the UI.
 * @author Mark Hsu
 */

interface Constants {
    // For transmitting messages between the service and the UI.
    int MESSAGE_READ = 10;
    int MESSAGE_WRITE = 11;
    int MESSAGE_TOAST = 12;
    int MESSAGE_CONNECTED = 13;
    int MESSAGE_CONNECT_ERR = 14;

    // For activity request code
    int REQUEST_ENABLE_BT = 20;
    int REQUEST_SELECT_DEVICE = 21;
}
