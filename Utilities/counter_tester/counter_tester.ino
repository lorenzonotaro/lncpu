#define CLOCK A2
#define NOT_RESET A1

#define PULSE_DURATION 1

void setup() {
  Serial.begin(9600);
  
  pinMode(CLOCK, OUTPUT);
  pinMode(NOT_RESET, OUTPUT);
  
  for(int i = 2; i <= 13; ++i){
    pinMode(i, INPUT);  
  }

  digitalWrite(CLOCK, LOW);
  digitalWrite(NOT_RESET, HIGH);

  reset_counter();
}

void pulse(int pin){
  digitalWrite(pin, HIGH);
  delayMicroseconds(PULSE_DURATION);
  digitalWrite(pin, LOW);
}

void reset_counter(){
  Serial.println("Resetting counter...");
  digitalWrite(NOT_RESET, LOW);
  pulse(CLOCK);
  digitalWrite(NOT_RESET, HIGH);
}

void loop() {
   long val = 0;
   for(int i = 0; i < 12; ++i){
     val += (digitalRead(i + 2) == HIGH ? 1 : 0) << i;
   }
   Serial.println(val);
   pulse(CLOCK);
}
