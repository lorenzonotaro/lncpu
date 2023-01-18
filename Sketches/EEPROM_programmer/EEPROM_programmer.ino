#include <Computer8Bit.h>

#define WRITE_CLOCK_DURATION 3000 /* us */
#define COUNTER_CLOCK_DURATION 1 /* us */

#define PAYLOAD_SIZE 30
                                                                                                                                                                                                                                                                                     
#define COUNTER_NOT_RESET A1
#define EEPROM_NOT_WE 10
#define EEPROM_NOT_CE 11
#define EEPROM_NOT_OE A0

Computer8Bit computer(12, 9, 8, 7, 6, 5, 4, 3, 2);
bool bus[8];

void reset_counter();
void write_eeprom();
void read_eeprom();

void setup() {
  Serial.begin(9600);

  pinMode(COUNTER_NOT_RESET, OUTPUT);
  pinMode(EEPROM_NOT_WE, OUTPUT);
  pinMode(EEPROM_NOT_OE, OUTPUT);
  pinMode(EEPROM_NOT_CE, OUTPUT);

  computer.busMode(OUTPUT);

  digitalWrite(COUNTER_NOT_RESET, HIGH);
  digitalWrite(EEPROM_NOT_WE, HIGH);
  digitalWrite(EEPROM_NOT_OE, LOW);
  digitalWrite(EEPROM_NOT_CE, HIGH);

  reset_counter();  
  write_eeprom();
  reset_counter();
  read_eeprom();
}

void reset_counter(){
  Serial.println("Resetting counter...");

  digitalWrite(COUNTER_NOT_RESET, LOW);
  computer.clockPulse(COUNTER_CLOCK_DURATION);
  digitalWrite(COUNTER_NOT_RESET, HIGH);
}

void write_eeprom(){
  byte value = 0xff;
  unsigned long int address;

  computer.busMode(OUTPUT);

  bus[0]=false;bus[1]=false;bus[2]=false;bus[3]=false;bus[4]=false;bus[5]=false;bus[6]=false;bus[7]=false;
  computer.writeToBus(bus);

  Serial.println("Writing to EEPROM...");
  digitalWrite(EEPROM_NOT_CE, LOW);
  delay(2000);
  for(address = 0; address < PAYLOAD_SIZE; ++address){
    tobinary(value, bus);
    digitalWrite(EEPROM_NOT_OE, HIGH);
    digitalWrite(EEPROM_NOT_WE, LOW);
    computer.writeToBus(bus);
    digitalWrite(EEPROM_NOT_WE, HIGH);
    digitalWrite(EEPROM_NOT_OE, LOW);
    computer.writeToBus(NULL);
    computer.clockPulse(COUNTER_CLOCK_DURATION);
  }
  digitalWrite(EEPROM_NOT_CE, HIGH);
}

void read_eeprom(){
  computer.busMode(INPUT);
  unsigned long int address;
  byte value;

  Serial.println("Reading from EEPROM...");
  digitalWrite(EEPROM_NOT_WE, HIGH);
  digitalWrite(EEPROM_NOT_OE, HIGH);
  for(address = 0; address < PAYLOAD_SIZE; ++address){
   digitalWrite(EEPROM_NOT_CE, LOW);
   digitalWrite(EEPROM_NOT_OE, LOW);
   if(address % 0x10 == 0){
    Serial.print("\n0x");
    Serial.print(address, HEX);
    Serial.print(":");
   }
   computer.readFromBus(bus);
   value = tobyte(bus);
   Serial.print(" 0x");
   Serial.print(value, HEX);
   Serial.print(" ");
   digitalWrite(EEPROM_NOT_OE, HIGH);
   digitalWrite(EEPROM_NOT_CE, HIGH);

  computer.clockPulse(COUNTER_CLOCK_DURATION);
  }
}

void loop() {
  // put your main code here, to run repeatedly:

}
