# BluetoothDemo

Demo how to communicate with Arduino via Bluetooth.



#### Here is Arduino code:


```c++
#include "DHT.h"
#define DHTPIN 2
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

void setup() {
  Serial.begin(9600);
  dht.begin();
}

void loop()
{
  char c;
  delay(100);
  
  if (Serial.available())
  {
    c = Serial.read();
    if (c == 't')
      readSensor();
  }

}
void readSensor() {
  float h = dht.readHumidity();
  float t = dht.readTemperature();
  String humidity,temp,heat;
  
  if (isnan(h) || isnan(t)) {
    Serial.println("Failed to read from DHT sensor!");
    return;
  }

  float hic = dht.computeHeatIndex(t, h, false);
  Serial.print("Humidity: ");
  Serial.print(h);
  Serial.println(" %");
  Serial.print("Temperature: ");
  Serial.print(t);
  Serial.println(" ℃");
  Serial.print("Heat index: ");
  Serial.print(hic);
  Serial.println(" ℃\n");
}
```