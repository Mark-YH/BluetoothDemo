package com.mark.bluetoothdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created on 2017/5/5
 *
 * @author Mark Hsu
 */

class LocationService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "LocationService";
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Context mContext;

    LocationService(Context context) {
        this.mContext = context;
        buildGoogleApiClient();
    }

    private boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Needs Permissions to access location");
            ActivityCompat.requestPermissions((Activity) mContext,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.REQUEST_LOCATION_PERMISSION);
            return false;
        }
        return true;
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
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
        if (checkPermission()) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Log.d(TAG, "Google API Client connected.");
        }
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

    String getAddress() {
        if (mLastLocation != null) {
            Geocoder gc = new Geocoder(mContext, Locale.TRADITIONAL_CHINESE);
            //自經緯度取得地址
            List<Address> lstAddress;
            try {
                lstAddress = gc.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                String address = lstAddress.get(0).getAddressLine(0);
                // 不需要郵遞區號及國家名稱
                address = address.replace(lstAddress.get(0).getPostalCode(), "");
                address = address.replace(lstAddress.get(0).getCountryName(), "");
                Log.d(TAG, address);
                return address;
            } catch (IOException e) {
                Log.e(TAG, "gc.getFromLocation() exception", e);
            }
        }
        Log.d(TAG, "mLastLocation: " + String.valueOf(mLastLocation == null));
        return "發生錯誤，請確認是否已開啟權限存取位置資訊"; // 說明確認權限後重試
    }

    Location getLocation() {
        return mLastLocation;
    }
}