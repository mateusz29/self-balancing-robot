#include <PID_v1.h>
#include <LMotorController.h>
#include <I2Cdev.h>
#include <MPU6050_6Axis_MotionApps20.h>
#include <SoftwareSerial.h>

#define MIN_ABSOLUTE_SPEED 10

MPU6050 mpu;
SoftwareSerial Bluetooth(A2, A3); // TX, RX

bool dmpReady = false;
uint8_t fifoBuffer[64];

Quaternion q; // [w, x, y, z]
VectorFloat gravity; // [x, y, z]
float ypr[3]; // [yaw, pitch, roll]

// PID
double angle = -7;
double input, output;
double powerLimit1 = 0.7;
double powerLimit2 = 0.6;

double Kp = 20;   
double Kd = 0.8;
double Ki = 180;
PID pid(&input, &output, &angle, Kp, Ki, Kd, DIRECT);

const int motor1Pin1 = 6; // IN1
const int motor1Pin2 = 7; // IN2
const int motor2Pin1 = 8; // IN3
const int motor2Pin2 = 9; // IN4
const int enable1Pin = 5; // ENA
const int enable2Pin = 10; // ENB 

volatile bool mpuInterrupt = false;
void dmpDataReady()
{
  mpuInterrupt = true;
}

void setup()
{
  Serial.begin(9600);
  Bluetooth.begin(9600);

  pinMode(motor1Pin1, OUTPUT);
  pinMode(motor1Pin2, OUTPUT);
  pinMode(motor2Pin1, OUTPUT);
  pinMode(motor2Pin2, OUTPUT);
  pinMode(enable1Pin, OUTPUT);
  pinMode(enable2Pin, OUTPUT);

  mpu.initialize();
  
  mpu.setXAccelOffset(540);
  mpu.setYAccelOffset(1301);
  mpu.setZAccelOffset(1326);
  mpu.setXGyroOffset(76);
  mpu.setYGyroOffset(-46);
  mpu.setZGyroOffset(31);

  if (mpu.dmpInitialize() == 0)
  {
    mpu.setDMPEnabled(true);
    attachInterrupt(0, dmpDataReady, RISING);
    dmpReady = true;

    pid.SetMode(AUTOMATIC);
    pid.SetSampleTime(10);
    pid.SetOutputLimits(-255, 255); 
  }
  else
  {
    Serial.print("Failed to initialize dmp!");
  }
}

void loop()
{
  if (!dmpReady) return;

  if (Bluetooth.available())
  {
    char command = Bluetooth.read();
    switch (command)
    {
      case 'F':
        Bluetooth.println("Moving the robot forward");
        moveForward();
        break;
      case 'B':
        Bluetooth.println("Moving the robot backward");
        moveBackward();
        break;
      case 'R':
        Bluetooth.println("Turning the robot right");
        turnRight();
        break;
      case 'L':
        Bluetooth.println("Turning the robot left");
        turnLeft();
        break;
      case 'S':
        Bluetooth.println("Balancing in place");
        balance(0);
      default:
        Bluetooth.println("Command not recognized");
        break;
    }
  }

  if (mpu.dmpGetCurrentFIFOPacket(fifoBuffer))
  {
    mpu.dmpGetQuaternion(&q, fifoBuffer);
    mpu.dmpGetGravity(&gravity, &q);
    mpu.dmpGetYawPitchRoll(ypr, &q, &gravity);

    input = ypr[1] * 180/M_PI;
    Serial.println(input);
    pid.Compute();

    balance(output);
  }
}

void balance(int output)
{
  // if (output > 0)
  // {
  //   digitalWrite(motor1Pin1, HIGH);
  //   digitalWrite(motor1Pin2, LOW);
  //   digitalWrite(motor2Pin1, HIGH);
  //   digitalWrite(motor2Pin2, LOW);
  // }
  // else
  // {
  //   digitalWrite(motor1Pin1, LOW);
  //   digitalWrite(motor1Pin2, HIGH);
  //   digitalWrite(motor2Pin1, LOW);
  //   digitalWrite(motor2Pin2, HIGH);
  // }

  // output = abs(output);
  
  // analogWrite(enable1Pin, output * powerLimit1);
  // analogWrite(enable2Pin, output * powerLimit2);

  if (output < 0)
  {
    output = min(output, -1*MIN_ABSOLUTE_SPEED);
  }
  else if (output > 0)
  {
    output = max(output, MIN_ABSOLUTE_SPEED);
  }

  int realOutput = map(abs(output), 0, 255, MIN_ABSOLUTE_SPEED, 255);

  digitalWrite(motor1Pin1, output > 0 ? HIGH : LOW);
  digitalWrite(motor1Pin2, output > 0 ? LOW : HIGH);
  digitalWrite(motor2Pin1, output > 0 ? HIGH : LOW);
  digitalWrite(motor2Pin2, output > 0 ? LOW : HIGH);
  analogWrite(enable1Pin, realOutput * powerLimit1);
  analogWrite(enable2Pin, realOutput * powerLimit2);
}
