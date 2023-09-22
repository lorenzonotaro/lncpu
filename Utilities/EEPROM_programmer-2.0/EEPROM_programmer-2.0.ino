#define PORT_BITMASK B00011110

#define CLOCK 3
#define NOT_RESET 4
#define ROM_NOT_CE 5
#define ROM_NOT_OE 6
#define ROM_NOT_WE 7

#define PULSE_DURATION 1

#define PAYLOAD 0x1000
int val = 0;
void setup() {
  busMode(INPUT);
  
  pinMode(CLOCK, OUTPUT);
  pinMode(NOT_RESET, OUTPUT);
  pinMode(ROM_NOT_CE, OUTPUT);
  pinMode(ROM_NOT_OE, OUTPUT);
  pinMode(ROM_NOT_WE, OUTPUT);
  
  Serial.begin(9600);

  digitalWrite(ROM_NOT_OE, LOW);
  digitalWrite(ROM_NOT_CE, HIGH);
  digitalWrite(ROM_NOT_WE, HIGH);
  
  reset_counter();
  delay(1000);
  write_to_eeprom();
  delay(1000);
  reset_counter();
  delay(1000);
  visit_eeprom();
}

void write_to_eeprom(){
  Serial.println("Writing...");
  busMode(OUTPUT);
  digitalWrite(ROM_NOT_OE, HIGH);
  digitalWrite(ROM_NOT_WE, HIGH);
  digitalWrite(ROM_NOT_CE, HIGH);
  for(int i = 0; i < PAYLOAD; ){
    digitalWrite(ROM_NOT_OE, HIGH);
    for(int j = 0; j <= 63; j++){
      digitalWrite(ROM_NOT_CE, LOW);
      digitalWrite(ROM_NOT_WE, LOW);
      writeBus(j);
      digitalWrite(ROM_NOT_WE, HIGH);
      digitalWrite(ROM_NOT_CE, HIGH);
      if(++i >= PAYLOAD)
        break;
      pulse(CLOCK);
    }
    digitalWrite(ROM_NOT_OE, LOW);
    delay(10);
  }

  digitalWrite(ROM_NOT_OE, HIGH);
  digitalWrite(ROM_NOT_CE, HIGH);
  digitalWrite(ROM_NOT_WE, HIGH);
}

void visit_eeprom(){
  Serial.println("Visiting...");
  busMode(INPUT);  

  digitalWrite(ROM_NOT_OE, HIGH);
  digitalWrite(ROM_NOT_CE, HIGH);
  digitalWrite(ROM_NOT_WE, HIGH);

  byte value;
  for(int i = 0; i < PAYLOAD; ++i){
    digitalWrite(ROM_NOT_CE, LOW);
    digitalWrite(ROM_NOT_OE, LOW);

    value = readBus();

    digitalWrite(ROM_NOT_OE, HIGH);
    digitalWrite(ROM_NOT_CE, HIGH);

    pulse(CLOCK);

    if(i % 16 == 0){
      Serial.print("\n 0x");
      Serial.print(i, BIN);
      Serial.print(":");
    }
    Serial.print(" ");
    Serial.print(value, HEX);
  }
}

void reset_counter(){
  Serial.println("Resetting counter...");
  digitalWrite(NOT_RESET, LOW);
  pulse(CLOCK);
  digitalWrite(NOT_RESET, HIGH);
}

void loop() {

}

byte readBus(){
    return ((PINB & PORT_BITMASK) >> 1) + ((PINC & PORT_BITMASK) << 3);
}

void writeBus(byte val){
  PORTB = (val << 1) & PORT_BITMASK;
  PORTC = (val >> 3) & PORT_BITMASK;
}

void busMode(int mode){
  DDRC = DDRB = mode == OUTPUT ? PORT_BITMASK : ~PORT_BITMASK;  
}

void pulse(int pin){
  digitalWrite(pin, HIGH);
  delayMicroseconds(PULSE_DURATION);
  digitalWrite(pin, LOW);
}

void lowPulse(int pin){
  digitalWrite(pin, LOW);
  delayMicroseconds(PULSE_DURATION);
  digitalWrite(pin, HIGH);
}
