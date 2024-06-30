#include <MPU6050.h>

MPU6050 mpu;

void setup() {
    Serial.begin(9600);
    mpu.initialize();
}

void loop() {
    int16_t ax, ay, az;
    mpu.getAcceleration(&ax, &ay, &az);

    Serial.print("ax: "); Serial.print(ax);
    Serial.print(" ay: "); Serial.print(ay);
    Serial.print(" az: "); Serial.println(az);

    delay(1000);
}
