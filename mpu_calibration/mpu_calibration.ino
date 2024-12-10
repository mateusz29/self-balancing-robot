#include <MPU6050.h>
MPU6050 mpu;

void setup() {
  Serial.begin(115200);
  mpu.initialize();
  
  Serial.println("Testing device connections...");
  Serial.println(mpu.testConnection() ? "MPU6050 connection successful" : "MPU6050 connection failed");
  delay(1000);

  Serial.println("Calculating offsets...");


  
  mpu.CalibrateAccel(6);
  mpu.CalibrateGyro(6);

  Serial.println("Calibration complete.");
  Serial.print("X Accel Offset: "); Serial.println(mpu.getXAccelOffset());
  Serial.print("Y Accel Offset: "); Serial.println(mpu.getYAccelOffset());
  Serial.print("Z Accel Offset: "); Serial.println(mpu.getZAccelOffset());
  Serial.print("X Gyro Offset: "); Serial.println(mpu.getXGyroOffset());
  Serial.print("Y Gyro Offset: "); Serial.println(mpu.getYGyroOffset());
  Serial.print("Z Gyro Offset: "); Serial.println(mpu.getZGyroOffset());
}

void loop() {

}
