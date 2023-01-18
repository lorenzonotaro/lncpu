#define PROTOCOL_SIGNATURE "EEPROMLD"
#define VERSION "AT28C64B, rev5"
#define BAUD_RATE 115200

#define PORT_BITMASK B00011110

#define CLOCK 3
#define NOT_RESET 4
#define ROM_NOT_CE 5
#define ROM_NOT_OE 6
#define ROM_NOT_WE 7

#define PULSE_DURATION 1

#define PAYLOAD 64
#define ROM_SIZE 4096

byte payload[PAYLOAD];

void setup() {
  Serial.begin(BAUD_RATE);
  Serial.setTimeout(BAUD_RATE);
  busMode(INPUT);
  
  pinMode(CLOCK, OUTPUT);
  pinMode(NOT_RESET, OUTPUT);
  pinMode(ROM_NOT_CE, OUTPUT);
  pinMode(ROM_NOT_OE, OUTPUT);
  pinMode(ROM_NOT_WE, OUTPUT);

  digitalWrite(CLOCK, LOW);
  digitalWrite(NOT_RESET, HIGH);
  digitalWrite(ROM_NOT_OE, LOW);
  digitalWrite(ROM_NOT_CE, HIGH);
  digitalWrite(ROM_NOT_WE, HIGH);

}

void loop() {
  byte b;
  if(Serial.available() > 0){
    char c = Serial.read();
    switch(c){
      case 'v':
        Serial.print(PROTOCOL_SIGNATURE VERSION);
        break;
      case 'w':
        reset_counter();
        for(int i = 0; i < ROM_SIZE / PAYLOAD; ++i){
          Serial.write('n');
          if(!read_payload_from_serial()){
            Serial.write('!');
            return;
          }
          write_eeprom();
        }
        Serial.write('k');
        break;
      case 'r':
        reset_counter();
        for(int i = 0; i < ROM_SIZE / PAYLOAD; ++i){
          if(Serial.readBytes(&b, 1) <= 0 || b != 'n'){
            Serial.write('!');
            return;
          }
          read_payload_from_eeprom();
          Serial.write(payload, PAYLOAD);
        }
      default:
        Serial.write('?');
    }
  }
}

void read_payload_from_eeprom(){
  busMode(INPUT);  

  digitalWrite(ROM_NOT_OE, HIGH);
  digitalWrite(ROM_NOT_CE, HIGH);
  digitalWrite(ROM_NOT_WE, HIGH);

  for(int i = 0; i < PAYLOAD; ++i){
    digitalWrite(ROM_NOT_CE, LOW);
    digitalWrite(ROM_NOT_OE, LOW);

    payload[i] = readBus();
    
    digitalWrite(ROM_NOT_OE, HIGH);
    digitalWrite(ROM_NOT_CE, HIGH);

    pulse(CLOCK);
  }
}

void write_eeprom(){
  busMode(OUTPUT);
  digitalWrite(ROM_NOT_OE, HIGH);
  digitalWrite(ROM_NOT_WE, HIGH);
  digitalWrite(ROM_NOT_CE, HIGH);

  for(int i = 0; i < PAYLOAD / 64; ++i){
    for(int j = 0; j < 64; j++){
        digitalWrite(ROM_NOT_CE, LOW);
        digitalWrite(ROM_NOT_WE, LOW);
        writeBus(payload[j]);
        digitalWrite(ROM_NOT_WE, HIGH);
        digitalWrite(ROM_NOT_CE, HIGH);
        pulse(CLOCK);
    }
    digitalWrite(ROM_NOT_OE, LOW);
  }

  digitalWrite(ROM_NOT_OE, HIGH);
  digitalWrite(ROM_NOT_CE, HIGH);
  digitalWrite(ROM_NOT_WE, HIGH);
}

void reset_counter(){
  digitalWrite(NOT_RESET, LOW);
  pulse(CLOCK);
  digitalWrite(NOT_RESET, HIGH);
}

bool read_payload_from_serial(){
  int count = Serial.readBytes(payload, PAYLOAD);
  return count == PAYLOAD;
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
