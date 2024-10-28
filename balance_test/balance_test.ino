#include <MPU6050.h>
#include <SoftwareSerial.h>

MPU6050 mpu;
SoftwareSerial Bluetooth(0, 1); // TX, RX

const int motor1Pin1 = 6; // IN1
const int motor1Pin2 = 7; // IN2
const int motor2Pin1 = 8; // IN3
const int motor2Pin2 = 9; // IN4
const int enable1Pin = 5; // ENA
const int enable2Pin = 10; // ENB 

// PID constants
float Kp = 7;
float Kd = 1;
float Ki = 10;

//float deadband = 10.0;
float setpoint = 0;
float input, output;
float previousError = 0;
float integral = 0;

unsigned long lastTime;
float powerLimit = 1;

void setup() {  
  pinMode(motor1Pin1, OUTPUT);
  pinMode(motor1Pin2, OUTPUT);
  pinMode(motor2Pin1, OUTPUT);
  pinMode(motor2Pin2, OUTPUT);
  pinMode(enable1Pin, OUTPUT);
  pinMode(enable2Pin, OUTPUT);

  mpu.initialize();

  Serial.begin(9600);
  Bluetooth.begin(9600);

  mpu.setXAccelOffset(540);
  mpu.setYAccelOffset(1301);
  mpu.setZAccelOffset(1326);
  mpu.setXGyroOffset(76);
  mpu.setYGyroOffset(-46);
  mpu.setZGyroOffset(31);

  lastTime = millis();
}

void loop() {
  // if (Bluetooth.available()) {
  //   char command = Bluetooth.read();
  //   switch (command) {
  //     case 'F': // Move robot forward
  //       Bluetooth.println("Moving the robot forward");
  //       break;
  //     case 'S': // Stop robot
  //       Bluetooth.println("Stopping the robot in place");
  //       break;
  //     case 'R': // Turn robot right
  //       Bluetooth.println("Turning the robot right");
  //       //turnRight();
  //       break;
  //     case 'L': // Turn robot left
  //       Bluetooth.println("Turning the robot left");
  //       //turnLeft();
  //       break;
  //     default:
  //       Bluetooth.println("Command not recognized");
  //       break;
  //   }
  // }

  unsigned long currentTime = millis();
  float elapsedTime = (currentTime - lastTime) / 1000.0;

  int16_t ax, ay, az;
  int16_t gx, gy, gz;
  mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);

  // Calculate angle 
  input = atan2(ax, az) * 180 / PI;

  // PID control
  float error = setpoint - input;
  // if (abs(error) < deadband) {
  //   error = 0;
  // }

  integral += error * elapsedTime;
  float derivative = (error - previousError) / elapsedTime;
  output = Kp * error + Ki * integral + Kd * derivative;
  output = constrain(output, -255, 255);
  output *= powerLimit;

  if (output > 0) {
    // Move backwards
    analogWrite(enable1Pin, output);
    digitalWrite(motor1Pin1, HIGH);
    digitalWrite(motor1Pin2, LOW);

    analogWrite(enable2Pin, output);
    digitalWrite(motor2Pin1, HIGH);
    digitalWrite(motor2Pin2, LOW);
  } else {
    // Move forwards
    analogWrite(enable1Pin, -output);
    digitalWrite(motor1Pin1, LOW);
    digitalWrite(motor1Pin2, HIGH);

    analogWrite(enable2Pin, -output);
    digitalWrite(motor2Pin1, LOW);
    digitalWrite(motor2Pin2, HIGH);
  }

  previousError = error;
  lastTime = currentTime;

  delay(10);
}
