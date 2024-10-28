#include <SoftwareSerial.h>

const int motor1Pin1 = 6; // IN1
const int motor1Pin2 = 7; // IN2
const int motor2Pin1 = 8; // IN3
const int motor2Pin2 = 9; // IN4
const int enable1Pin = 5; // ENA
const int enable2Pin = 10; // ENB

SoftwareSerial bluetooth(0, 1); // RX, TX

void setup() {
  pinMode(motor1Pin1, OUTPUT);
  pinMode(motor1Pin2, OUTPUT);
  pinMode(motor2Pin1, OUTPUT);
  pinMode(motor2Pin2, OUTPUT);
  pinMode(enable1Pin, OUTPUT);
  pinMode(enable2Pin, OUTPUT);

  Serial.begin(9600);
  bluetooth.begin(9600);
}

void loop() {
  if (bluetooth.available()) {
    char command = bluetooth.read();

    if (command == 'f' || command == 's') {
      Serial.print("Received Command: ");
      Serial.println(command);

      switch (command) {
        case 'f':
          moveMotorsForward();
          bluetooth.println("Motors moving forward");
          break;
        case 's':
          stopMotors();
          bluetooth.println("Motors stopped");
          break;
      }
    } else {
        Serial.print("Unknown command: ");
        Serial.println(command);
    }
  }
}

void moveMotorsForward() {
  digitalWrite(motor1Pin1, HIGH);
  digitalWrite(motor1Pin2, LOW);
  analogWrite(enable1Pin, 255);

  digitalWrite(motor2Pin1, HIGH);
  digitalWrite(motor2Pin2, LOW);
  analogWrite(enable2Pin, 255);
}

void stopMotors() {
  digitalWrite(motor1Pin1, LOW);
  digitalWrite(motor1Pin2, LOW);
  analogWrite(enable1Pin, 0);

  digitalWrite(motor2Pin1, LOW);
  digitalWrite(motor2Pin2, LOW);
  analogWrite(enable2Pin, 0);
}
