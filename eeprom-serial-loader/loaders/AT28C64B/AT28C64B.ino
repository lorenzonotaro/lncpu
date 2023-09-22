/*  Created by: Lorenzo Notaro
 * 
 *  This sketch is the loader for the AT28C64B EEPROM chip.
 *  It was written for Arduino Nano and it conforms to the protocol defined in
 *  the loaders/ directory of this repository.
 *  The circuit associated with the sketch is illsustrated in an adjacent file.
 *  Given the limited numbers of pins on the Arduino Nano, 
 *  3 4-bit counters are used to keep track of addresses A0-A11, while a digital pin 
 *  will determine which half of the EEPROM is active (A12).
 */

/* The protocol signature that will let the desktop app identify the serial port. */
#define PROTOCOL_SIGNATURE "EEPROMLD"

/* Name and revision of this loader. */
#define VERSION "AT28C64B, rev6\0"

/* The baud rate, as specified in the protocol.*/
#define BAUD_RATE 115200

/* For performance reasons this sketch doesn't use plain digitalWrite's for
   writing data to the EEPROM. Instead, it uses the port registers B and C
   (you can read more about port register at https://www.arduino.cc/en/Reference/PortManipulation).
   This port bitmask specifies which digits of each port we are using; the ports are conveniently set up
   so that the bitmask will be the same for both PORTB and PORTC. 
   Refer to the writeBus and readBus functions to see how this works.*/
#define PORT_BITMASK B00011110

/* The clock pin. */
#define CLOCK      3

/* The !reset pin of the 3 4-bin counters. */
#define NOT_RESET  4

/* The EEPROM !CE (chip enable) pin. */
#define ROM_NOT_CE 5

/* The EEPROM !OE (output enable) pin. */
#define ROM_NOT_OE 6

/* The EEPROM !WE (chip enable) pin. */
#define ROM_NOT_WE 7

/* The most significant digit of the address bus. */
#define A12        8

/* The duration of the clock pulse, in μs. */
#define PULSE_DURATION 1

/* The payload size. */
#define PAYLOAD (unsigned int) 64

/* The EEPROM size. */
#define ROM_SIZE  (unsigned int) 8192

/* This will store the payload every time we receive one from the client. */
byte payload[PAYLOAD];

void setup() {
  Serial.begin(BAUD_RATE);

  //initialize all the pins. 
  busMode(INPUT);
  pinMode(CLOCK, OUTPUT);
  pinMode(NOT_RESET, OUTPUT);
  pinMode(ROM_NOT_CE, OUTPUT);
  pinMode(ROM_NOT_OE, OUTPUT);
  pinMode(ROM_NOT_WE, OUTPUT);
  pinMode(A12, OUTPUT);

  //set each pin to their default (inactive) value
  digitalWrite(CLOCK, LOW);
  digitalWrite(NOT_RESET, HIGH);
  digitalWrite(ROM_NOT_OE, LOW);
  digitalWrite(ROM_NOT_CE, HIGH);
  digitalWrite(ROM_NOT_WE, HIGH);
  digitalWrite(A12, LOW);

}

/* In the loop, the Arduino will be waiting for a request from the client and will act accordingly. */
void loop() {
  byte b;
  if(Serial.available() > 0){
    char c = Serial.read();
    switch(c){
      case 'v': //the client requested the loader signature and version
        Serial.write(PROTOCOL_SIGNATURE VERSION);
        break;
      case 'p': //the client requested the parameters for this loader
        write16(ROM_SIZE);
        write16(PAYLOAD);
        break;
      case 'w': 
        //the client requested to initiate a write cycle
        //we'll split the write cycles in two for loops, one with A12 on LOW and one with A12 on HIGH
        reset_counter();
        digitalWrite(A12, LOW);
        for(int i = 0; i < ROM_SIZE / PAYLOAD / 2; ++i){
          Serial.write('n');
          if(!read_payload_from_serial()){
            Serial.write('!');
            return;
          }
          write_eeprom();
        }
        reset_counter();
        digitalWrite(A12, HIGH);
        for(int i = 0; i < ROM_SIZE / PAYLOAD / 2; ++i){ //write cycle
          Serial.write('n'); //send control character
          if(!read_payload_from_serial()){
            Serial.write('!'); //error during serial read
            return;
          }
          write_eeprom();
        }
        digitalWrite(A12, LOW);
        Serial.write('k'); //send confirmation character
        break;
      case 'r':
        reset_counter();
        digitalWrite(A12, LOW);
        for(int i = 0; i < ROM_SIZE / PAYLOAD / 2; ++i){
          if(Serial.readBytes(&b, 1) <= 0 || b != 'n'){
            Serial.write('!');
            return;
          }
          read_payload_from_eeprom();
          Serial.write(payload, PAYLOAD);
        }
        reset_counter();
        digitalWrite(A12, HIGH);
        for(int i = 0; i < ROM_SIZE / PAYLOAD / 2; ++i){
          if(Serial.readBytes(&b, 1) <= 0 || b != 'n'){
            Serial.write('!');
            return;
          }
          read_payload_from_eeprom();
          Serial.write(payload, PAYLOAD);
        }
        digitalWrite(A12, LOW);
        break;
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
  //We are using page write mode on the AT28C64B, as explained in the datasheet
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
    return ((PINB & PORT_BITMASK) >> 1) + ((PINC & PORT_BITMASK) << 3); //We do the inverse of the shifts we did in writeBus()
}

void writeBus(byte val){
  //The first digit of PORTB (pin number 8) is being used for A12, so we want to save whatever value is in that bit
  //We want the last 4 bits of val (B00001111) to be positioned B00011110 in PORTB: shift 1 to the left
  PORTB = (PORTB & 1) | ((val << 1) & PORT_BITMASK);
  //We want the first 4 bits of val (B11110000) to be positioned B00011110 in PORTC: shift 3 to the right
  PORTC = (val >> 3) & PORT_BITMASK;
}

void busMode(int mode){
  DDRC = DDRB = mode == OUTPUT ? 0xFF : 0x00;  
  DDRB |= 1; //The first digit of PORTB (pin number 8) is being used for A12: it's always output
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

void write16(unsigned int val){
  byte buf[2];
  buf[0] = (val >> 8) & 0xFF;
  buf[1] = val & 0xFF;
  Serial.write(buf, 2);
}
