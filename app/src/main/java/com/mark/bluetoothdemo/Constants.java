package com.mark.bluetoothdemo;

/**
 * Created on 2017/4/16.
 * Defines several constants used when
 * transmitting messages between the service and the UI.
 *
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

    // For Location
    int REQUEST_LOCATION_PERMISSION = 31;
    int LOCATION_SERVICE_ERROR = 32;

    // For STT Service
    int STT_ERROR = 40;
    int STT_ASK_LOCATION = 41;
    int STT_ASK_OBSTACLE = 42;
    int STT_RESULT_OBSTACLE = 43;
    int STT_REQUEST_PERMISSION = 44;
    int STT_RESULT_RECOGNITION =45;
}
