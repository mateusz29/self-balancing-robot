const int motor1Pin1 = 6; // IN1
const int motor1Pin2 = 7; // IN2
const int motor2Pin1 = 8; // IN3
const int motor2Pin2 = 9; // IN4
const int enable1Pin = 5; // ENA
const int enable2Pin = 10; // ENB

void setup() {
  pinMode(motor1Pin1, OUTPUT);
  pinMode(motor1Pin2, OUTPUT);
  pinMode(motor2Pin1, OUTPUT);
  pinMode(motor2Pin2, OUTPUT);
  pinMode(enable1Pin, OUTPUT);
  pinMode(enable2Pin, OUTPUT);

  // Motor 1 forwards
  digitalWrite(motor1Pin1, HIGH);
  digitalWrite(motor1Pin2, LOW);
  analogWrite(enable1Pin, 255); // Full speed

  // Motor 2 forwards
  digitalWrite(motor2Pin1, HIGH);
  digitalWrite(motor2Pin2, LOW);
  analogWrite(enable2Pin, 255); // Full speed
}

void loop() {

}
