#include <MPU6050.h>

MPU6050 mpu;

const int motor1Pin1 = 6; // IN1
const int motor1Pin2 = 7; // IN2
const int motor2Pin1 = 8; // IN3
const int motor2Pin2 = 9; // IN4
const int enable1Pin = 5; // ENA
const int enable2Pin = 10; // ENB 

// PID constants
float Kp = 4.0;
float Kd = 0.0;
float Ki = 1.0;

float setpoint = 0;
float input, output;
float previousError = 0;
float integral = 0;

unsigned long lastTime;

void setup() {
  pinMode(motor1Pin1, OUTPUT);
  pinMode(motor1Pin2, OUTPUT);
  pinMode(motor2Pin1, OUTPUT);
  pinMode(motor2Pin2, OUTPUT);
  pinMode(enable1Pin, OUTPUT);
  pinMode(enable2Pin, OUTPUT);

  mpu.initialize();

  Serial.begin(9600);

  mpu.setXAccelOffset(246);
  mpu.setYAccelOffset(-1931);
  mpu.setZAccelOffset(1128);
  mpu.setXGyroOffset(74);
  mpu.setYGyroOffset(-38);
  mpu.setZGyroOffset(5);

  lastTime = millis();
}

void loop() {
  unsigned long currentTime = millis();
  float elapsedTime = (currentTime - lastTime) / 1000.0;

  int16_t ax, ay, az;
  int16_t gx, gy, gz;
  mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);

  // Calculate angle 
  input = atan2(ax, az) * 180 / PI;

  // PID control
  float error = setpoint - input;
  integral += error * elapsedTime;
  float derivative = (error - previousError) / elapsedTime;
  output = Kp * error + Ki * integral + Kd * derivative;
  output = constrain(output, -255, 255);

  if (output > 0) {
    // Move backwards
    analogWrite(enable1Pin, output);
    digitalWrite(motor1Pin1, HIGH);
    digitalWrite(motor1Pin2, LOW);

    analogWrite(enable2Pin, output);
    digitalWrite(motor2Pin1, LOW);
    digitalWrite(motor2Pin2, HIGH);
  } else {
    // Move forwards
    analogWrite(enable1Pin, -output);
    digitalWrite(motor1Pin1, LOW);
    digitalWrite(motor1Pin2, HIGH);

    analogWrite(enable2Pin, -output);
    digitalWrite(motor2Pin1, HIGH);
    digitalWrite(motor2Pin2, LOW);
  }

  previousError = error;
  lastTime = currentTime;

  delay(10);
}
