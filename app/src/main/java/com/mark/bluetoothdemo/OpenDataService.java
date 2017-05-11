package com.mark.bluetoothdemo;

import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created on 2017/5/5
 *
 * @author Mark Hsu
 */

class OpenDataService {
    private static final String TAG = "OpenDataService";
    private LocationService mLocationService;
    private ArrayList<String> obstacle;
    private Handler mHandler;

    OpenDataService(LocationService locationService, Handler handler) {
        mHandler = handler;
        mLocationService = locationService;
        mHandler.sendEmptyMessage(Constants.LOCATION_SERVICE_ERROR);
    }

    void start() {
        obstacle = new ArrayList<>();
        new RequestThread().start();
    }

    private static String executeGet(String targetURL) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(targetURL);

            conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(1500);
            conn.setConnectTimeout(1500);
            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36");
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            InputStream is = conn.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }

            rd.close();

            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Exception:", e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private class RequestThread extends Thread {

        @Override
        public void run() {

            String jsonData = executeGet("http://od.moi.gov.tw/data/api/pbs");
            if (jsonData != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonData);
                    JSONArray jsonAry = new JSONArray(jsonObj.getString("result"));
                    for (int i = 0; i < jsonAry.length(); i++) {
                        String happenDate = jsonAry.getJSONObject(i).getString("happendate");
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
                        String[] currentDateandTime = sdf.format(new Date()).split("_");
                        String today = currentDateandTime[0];
                        String time = currentDateandTime[1];
                        if (happenDate.equals(today)) {
                            //TODO time filter
                            Log.d(TAG, "i = " + i + "\tTime = " + time);
                            dataFilter(jsonAry.getJSONObject(i));
                        }
                    }
                    mHandler.obtainMessage(Constants.STT_RESULT_OBSTACLE, obstacle).sendToTarget();
                } catch (JSONException e) {
                    Log.e(TAG, "JSON Exception:", e);
                }
            } else {
                this.start();
            }
        }

        void dataFilter(JSONObject jsonObject) {
            double x1, y1, x2, y2;
            try {
                x1 = Double.valueOf(jsonObject.getString("x1"));
                y1 = Double.valueOf(jsonObject.getString("y1"));
                try {
                    x2 = mLocationService.getLocation().getLongitude();
                    y2 = mLocationService.getLocation().getLatitude();
                } catch (NullPointerException e) {
                    mHandler.sendEmptyMessage(Constants.LOCATION_SERVICE_ERROR);
                    Log.e(TAG, "mLocationService.getLocation() is null", e);
                    return;
                }
                final int LIMIT_DISTANCE = 300_000; // 1 km = 1000
                double distance = getDistance(x1, y1, x2, y2);
                String areaNm = jsonObject.getString("areaNm");
                if (distance < LIMIT_DISTANCE &&
                        !areaNm.contains("高速") &&
                        !areaNm.contains("快速") &&
                        !areaNm.contains("國道")) {
                    String s = ("方向:" + jsonObject.getString("direction") + "\n" +
                            "道路名稱:" + jsonObject.getString("road") + "\n" +
                            "路況類別:" + jsonObject.getString("roadtype") + "\n" +
                            "發生日期:" + jsonObject.getString("happendate") + "\n" +
                            "發生時間:" + jsonObject.getString("happentime") + "\n" +
                            "修改時間:" + jsonObject.getString("modDttm") + "\n" +
                            "直線距離: " + distance / 1000 + "km\n" +
                            "狀況: " + jsonObject.getString("comment") + "\n" +
                            "------\n");
                    Log.d(TAG, s);
                    obstacle.add(s);
                }
                Log.d(TAG, "Distance: " + distance / 1000 + "km");
            } catch (
                    JSONException e)

            {
                e.printStackTrace();
            }
        }

    }

    private static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        double a = 6378137, b = 6356752.314245, f = 1 / 298.257223563;
        double L = Math.toRadians(lon2 - lon1);
        double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat1)));
        double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat2)));
        double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);
        double cosSqAlpha;
        double sinSigma;
        double cos2SigmaM;
        double cosSigma;
        double sigma;

        double lambda = L, lambdaP, iterLimit = 100;
        do {
            double sinLambda = Math.sin(lambda), cosLambda = Math.cos(lambda);
            sinSigma = Math.sqrt((cosU2 * sinLambda)
                    * (cosU2 * sinLambda)
                    + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
                    * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
            );
            if (sinSigma == 0) {
                return 0;
            }

            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1 - sinAlpha * sinAlpha;
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;

            double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
            lambdaP = lambda;
            lambda = L + (1 - C) * f * sinAlpha
                    * (sigma + C * sinSigma
                    * (cos2SigmaM + C * cosSigma
                    * (-1 + 2 * cos2SigmaM * cos2SigmaM)
            )
            );

        } while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

        if (iterLimit == 0) {
            return 0;
        }

        double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384
                * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double deltaSigma =
                B * sinSigma
                        * (cos2SigmaM + B / 4
                        * (cosSigma
                        * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM
                        * (-3 + 4 * sinSigma * sinSigma)
                        * (-3 + 4 * cos2SigmaM * cos2SigmaM)));

        return b * A * (sigma - deltaSigma);
    }
}
