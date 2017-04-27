# BluetoothDemo

Demo how to communicate with Arduino via Bluetooth.

> 自訂一個封包共 20 bytes
> * 4 bytes = Header
> * 4 bytes = 光感測值 (int)
> * 4 bytes = 濕度感測值 (float)
> * 4 bytes = 溫度感測值 (float)
> * 4 bytes = 熱指數值 (float)

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