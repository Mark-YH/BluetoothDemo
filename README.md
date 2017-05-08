# BluetoothDemo

Intelligent helmet project


Arduino
---
 自訂一個封包共 20 bytes
* 4 bytes = Header
* 4 bytes = 光感測值 (int)
* 4 bytes = 濕度感測值 (float)
* 4 bytes = 溫度感測值 (float)
* 4 bytes = 熱指數值 (float)

#### Here is Arduino code:

```c++
#include "DHT.h"
#define DHTPIN 2
#define DHTTYPE DHT11
#define LIGHT_SENSOR_ANALOG A0

/**
   char 佔 1 byte
   char c = {'a', 'b'} 佔 3 bytes, 第 3 個 byte 為 '\0'
   float and double 在 arduino中都佔 4 bytes
   int 佔 2 bytes
*/

DHT dht(DHTPIN, DHTTYPE);
int flag = 0;

typedef union {
  float floatingPoint;
  byte binary[4];
} binaryFloat;

typedef union {
  int intPoint;
  byte binary[2]; // Arduino 之 int 佔 2 bytes
} binaryInteger;

void sendInteger(int i) {
  /**
     在 Arduino 中 int 為 2 bytes, Android Java 是 4 bytes
     若直接使用 Java 的 ByteBuffer.wrap() 來取得 int value 會拋出 exception
     因此透過此函式發送 int 後再發送一個 padding 來補齊 4 bytes
     而 padding 必須為全 0 或全 1, 端看 int value 為正數或負數
     if (int value >= 0) padding = 0000 0000
     0000 0000 ???? ????
     if (int value < 0) padding = 1111 1111
     1111 1111 ???? ????

     ps. 右方 ???? ???? 為 int value
  */
  binaryInteger iBin;
  binaryInteger padding;
  iBin.intPoint = i;
  Serial.write(iBin.binary, 2);

  if (iBin.intPoint >= 0) {
    padding.intPoint = 0; // 0000 0000 ???? ????
    Serial.write(padding.binary, 2);
  } else {
    padding.intPoint = -1; // 1111 1111 ???? ????
    Serial.write(padding.binary, 2);
  }
}

void sendFloat(float f){
  binaryFloat fBin;
  fBin.floatingPoint = f;
  Serial.write(fBin.binary, 4);
}

void setup() {
  Serial.begin(9600);
  dht.begin();
}

void loop()
{
  char c;
  if (flag) {
    char cc[] = {'H','E','A','D'}; // Header Start Check
    Serial.write(cc,4);
    readSensor();
    delay(10);
  }
  if (Serial.available())
  {
    c = Serial.read();
    if (c == 't') {
      flag = 1;
    } else if (c == 's') {
      flag = 0;
    }
  }
}

void readSensor() {
  float h = dht.readHumidity();
  float t = dht.readTemperature();
  int lightVal = analogRead(LIGHT_SENSOR_ANALOG);

  if (isnan(h) || isnan(t)) {
    h = -1;
    t = -1;
  }
  float hic = dht.computeHeatIndex(t, h, false);

  sendInteger(lightVal); // Light
  sendFloat(h); // Humidity
  sendFloat(t); // Temperature
  sendFloat(hic); // Heat index
}
```


Android
---

**Google Location Services API**

Android 官方建議使用 Google Location Services API 來取得位置資訊，下列擷取自官方文件：
> The Google Location Services API, part of Google Play services, is the preferred way to add location-awareness to your app. It offers a simpler API, higher accuracy, low-power geofencing, and more. If you are currently using the android.location API, you are strongly encouraged to switch to the Google Location Services API as soon as possible. 

 要使用 Google Location Services API 必須先完成下列前置動作 (setup)
 步驟擷取自：[https://developers.google.com/android/guides/setup](https://developers.google.com/android/guides/setup)
 
##### Step1. Set Up Google Play Services
> To develop an app using the Google Play services APIs, you need to set up your project with the Google Play services SDK.
>
> If you haven't installed the Google Play services SDK yet, get it now:
>
> 1. Start Android Studio.
> 2. On the Tools menu, click Android > SDK Manager.
> 3. Update the Android Studio SDK Manager: click SDK Tools, expand Support Repository, select Google Repository, and then click OK.

##### Step2. Add Google Play Services to Your Project
      
使用 Google Play Services 需要編輯 build.gradle(Module: app) 如下： 
 
 ```
 apply plugin: 'com.android.application'
      ...
  
      dependencies {
          compile 'com.google.android.gms:play-services:10.2.1'
      }
 ```

此專案目前只需要 Location 的 API，因此指定 location 即可，如下：

`compile 'com.google.android.gms:play-services-location:10.2.1'`

此外，官方特別註明：

> Be sure you update this version number each time Google Play services is updated.

