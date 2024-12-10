#include <PID_v1.h>
#include <MPU6050_6Axis_MotionApps20.h>
#include <SoftwareSerial.h>
#include <math.h>

const uint8_t MOTOR1_PIN1 = 6; // IN1
const uint8_t MOTOR1_PIN2 = 7; // IN2
const uint8_t MOTOR2_PIN1 = 8; // IN3
const uint8_t MOTOR2_PIN2 = 9; // IN4
const uint8_t MOTOR1_ENABLE = 5; // ENA
const uint8_t MOTOR2_ENABLE = 10; // ENB
const uint8_t BLUETOOTH_TX = A3;
const uint8_t BLUETOOTH_RX = A2;

bool dmpReady = false;
uint8_t mpuIntStatus;
uint16_t packetSize;
uint16_t fifoCount;
uint8_t fifoBuffer[64];
Quaternion quaternion;
VectorFloat gravity;
float ypr[3];
volatile bool mpuInterrupt = false;

MPU6050 mpu;
SoftwareSerial Bluetooth(BLUETOOTH_RX, BLUETOOTH_TX);
String command = "";

const uint8_t MIN_MOTOR_SPEED = 30;
const uint8_t MAX_MOTOR_SPEED = 255;
const uint16_t TURN_DURATION = 50;
const uint8_t TURN_SPEED = 100;
double powerLimit1 = 0.6;
double powerLimit2 = 0.5;

double targetAngle = -5;
double originalAngle = targetAngle;
double angleOffset = 0.8;
double input, output;
double Kp = 20;
double Ki = 180;
double Kd = 1.2;
PID pid(&input, &output, &targetAngle, Kp, Ki, Kd, DIRECT);

bool isTurning = false;
int8_t turnDirection = 0; // -1: left, 0: none, 1: right
unsigned long turnStartTime = 0;

void dmpDataReady() {
  mpuInterrupt = true;
}

void setup() {
  Bluetooth.begin(9600);
  
  pinMode(MOTOR1_PIN1, OUTPUT);
  pinMode(MOTOR1_PIN2, OUTPUT);
  pinMode(MOTOR2_PIN1, OUTPUT);
  pinMode(MOTOR2_PIN2, OUTPUT);
  pinMode(MOTOR1_ENABLE, OUTPUT);
  pinMode(MOTOR2_ENABLE, OUTPUT);

  mpu.initialize();
  
  mpu.setXAccelOffset(-2352);
  mpu.setYAccelOffset(-1055);
  mpu.setZAccelOffset(1196);
  mpu.setXGyroOffset(180);
  mpu.setYGyroOffset(-9);
  mpu.setZGyroOffset(58);

  if (mpu.dmpInitialize() == 0) {
    mpu.setDMPEnabled(true);
    attachInterrupt(0, dmpDataReady, RISING);
    mpuIntStatus = mpu.getIntStatus();
    dmpReady = true;
    packetSize = mpu.dmpGetFIFOPacketSize();

    pid.SetMode(AUTOMATIC);
    pid.SetSampleTime(5);
    pid.SetOutputLimits(-MAX_MOTOR_SPEED, MAX_MOTOR_SPEED); 
  }
}

void initiateTurn(int8_t direction) {
  isTurning = true;
  turnDirection = direction;
  turnStartTime = millis();
}

void handleCommand(String command) {
  if (command.startsWith("F")) 
    targetAngle = targetAngle - angleOffset;
  else if (command.startsWith("B"))
    targetAngle = targetAngle + angleOffset;
  else if (command.startsWith("R")) {
    initiateTurn(1);
  } else if (command.startsWith("L")) {
    initiateTurn(-1);
  } else if (command.startsWith("S")) {
    targetAngle = originalAngle;
    turnDirection = 0;
    isTurning = false;
  } else if (command.startsWith("A")) {
    targetAngle = command.substring(1).toDouble();
    originalAngle = targetAngle;
  } else if (command.startsWith("P")) {
    Kp = command.substring(1).toDouble();
    pid.SetTunings(Kp, Ki, Kd);
  } else if (command.startsWith("I")) {
    Ki = command.substring(1).toDouble();
    pid.SetTunings(Kp, Ki, Kd);
  } else if (command.startsWith("D")) {
    Kd = command.substring(1).toDouble();
    pid.SetTunings(Kp, Ki, Kd);
  }
}

void processBluetoothCommand() {
  while (Bluetooth.available()) {
    char incomingChar = Bluetooth.read();

    if (incomingChar == '\n') {
      handleCommand(command);
      command = "";
    } 
    else command += incomingChar;
  }
}

void setMotorDirection(int pin1, int pin2, bool isForward) {
  digitalWrite(pin1, isForward ? HIGH : LOW);
  digitalWrite(pin2, isForward ? LOW : HIGH);
}

void turn(double balanceOutput, int turnDirection) {
  double leftMotorSpeed = TURN_SPEED * -turnDirection;
  double rightMotorSpeed = TURN_SPEED * turnDirection;

  setMotorDirection(MOTOR1_PIN1, MOTOR1_PIN2, leftMotorSpeed > 0);
  setMotorDirection(MOTOR2_PIN1, MOTOR2_PIN2, rightMotorSpeed > 0);

  int adjustedPower1 = map(abs(leftMotorSpeed), 0, MAX_MOTOR_SPEED, MIN_MOTOR_SPEED, MAX_MOTOR_SPEED);
  int adjustedPower2 = map(abs(rightMotorSpeed), 0, MAX_MOTOR_SPEED, MIN_MOTOR_SPEED, MAX_MOTOR_SPEED);

  analogWrite(MOTOR1_ENABLE, adjustedPower1 * powerLimit1 / 2);
  analogWrite(MOTOR2_ENABLE, adjustedPower2 * powerLimit2 / 2);
}

void controlMotors(double balanceOutput) {
  if (balanceOutput < 0) balanceOutput = min(balanceOutput, -1 * MIN_MOTOR_SPEED);
  else if (balanceOutput > 0) balanceOutput = max(balanceOutput, MIN_MOTOR_SPEED);

  int adjustedPower = map(abs(balanceOutput), 0, MAX_MOTOR_SPEED, MIN_MOTOR_SPEED, MAX_MOTOR_SPEED);

  setMotorDirection(MOTOR1_PIN1, MOTOR1_PIN2, balanceOutput > 0);
  setMotorDirection(MOTOR2_PIN1, MOTOR2_PIN2, balanceOutput > 0);

  analogWrite(MOTOR1_ENABLE, adjustedPower * powerLimit1);
  analogWrite(MOTOR2_ENABLE, adjustedPower * powerLimit2);
}

void loop() {
  if (!dmpReady) return;
  
  while (!mpuInterrupt && fifoCount < packetSize) {
    pid.Compute();    
    controlMotors(output);
    processBluetoothCommand();
    
    if (isTurning) {
      if (millis() - turnStartTime < TURN_DURATION) {
        turn(output, turnDirection);
      } else {
        isTurning = false;
        turnDirection = 0;
      }
    }
  }

  mpuInterrupt = false;
  mpuIntStatus = mpu.getIntStatus();

  fifoCount = mpu.getFIFOCount();

  if ((mpuIntStatus & 0x10) || fifoCount == 1024) {
    mpu.resetFIFO();
  }
  else if (mpuIntStatus & 0x02) {
    while (fifoCount < packetSize) fifoCount = mpu.getFIFOCount();

    mpu.getFIFOBytes(fifoBuffer, packetSize);
    
    fifoCount -= packetSize;

    mpu.dmpGetQuaternion(&quaternion, fifoBuffer);
    mpu.dmpGetGravity(&gravity, &quaternion);
    mpu.dmpGetYawPitchRoll(ypr, &quaternion, &gravity);
    input = ypr[1] * RAD_TO_DEG;
  }
}