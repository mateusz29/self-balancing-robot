#include <MPU6050.h>

MPU6050 mpu;

void setup() {
  Serial.begin(9600);
  mpu.initialize();
}

void loop() {

  int16_t ax, ay, az, gx, gy, gz;
  mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);

  Serial.print("aX = ");
  Serial.print(ax);
  Serial.print(" | aY = ");
  Serial.print(ay);
  Serial.print(" | aZ = ");
  Serial.println(az);

  Serial.print("gX = ");
  Serial.print(gx);
  Serial.print(" | gY = ");
  Serial.print(gy);
  Serial.print(" | gZ = ");
  Serial.println(gz);

  delay(500);
}
