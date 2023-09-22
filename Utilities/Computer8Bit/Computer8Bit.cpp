#include "Computer8Bit.h"

Computer8Bit::Computer8Bit(byte clock, byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) :
clockPin(clock), b0(b0), b1(b1), b2(b2), b3(b3), b4(b4), b5(b5), b6(b6), b7(b7){
  pinMode(clockPin, OUTPUT);
}



void Computer8Bit::clock(int status){
  digitalWrite(clockPin, status);
}

void Computer8Bit::clockPulse(unsigned int duration){
  digitalWrite(clockPin, HIGH);
  delayMicroseconds(duration);
  digitalWrite(clockPin, LOW);
}

void Computer8Bit::readFromBus(bool values[]){
  values[0] = digitalRead(b0);
  values[1] = digitalRead(b1);
  values[2] = digitalRead(b2);
  values[3] = digitalRead(b3);
  values[4] = digitalRead(b4);
  values[5] = digitalRead(b5);
  values[6] = digitalRead(b6);
  values[7] = digitalRead(b7);

}

void Computer8Bit::writeToBus(bool values[]){
  if(values == NULL){
    digitalWrite(b0, LOW);
    digitalWrite(b1, LOW);
    digitalWrite(b2, LOW);
    digitalWrite(b3, LOW);
    digitalWrite(b4, LOW);
    digitalWrite(b5, LOW);
    digitalWrite(b6, LOW);
    digitalWrite(b7, LOW);
    return;
  }
  digitalWrite(b0, values[0]);
  digitalWrite(b1, values[1]);
  digitalWrite(b2, values[2]);
  digitalWrite(b3, values[3]);
  digitalWrite(b4, values[4]);
  digitalWrite(b5, values[5]);
  digitalWrite(b6, values[6]);
  digitalWrite(b7, values[7]);

}

void Computer8Bit::busMode(int mode){
  pinMode(b0, mode);
  pinMode(b1, mode);
  pinMode(b2, mode);
  pinMode(b3, mode);
  pinMode(b4, mode);
  pinMode(b5, mode);
  pinMode(b6, mode);
  pinMode(b7, mode);
}

void Computer8Bit::setBusPins(byte _b0, byte _b1, byte _b2, byte _b3, byte _b4, byte _b5, byte _b6, byte _b7){
	b0 = _b0;
	b1 = _b1;
	b2 = _b2;
	b3 = _b3;
	b4 = _b4;
	b5 = _b5;
	b6 = _b6;
	b7 = _b7;
}

void Computer8Bit::setClockPin(byte clock){
	clockPin = clock;
  pinMode(clock, OUTPUT);
}


void tobinary(byte number, bool dest[]){
  byte j;
  for(j = 0; j < 8; ++j){
    dest[7-j] = (number >> j) & 1;
  }
}

byte tobyte(bool bits[]){
  byte j, val = 0;
  for(j = 0; j < 8; ++j){
    val += (bits[j] << 7 - j);
  }
  return val;
}

void serialFlush(){
  while(Serial.read() != -1);
}


void serialPrintf(char *format, ...){
  char buf[256];
  va_list args;
  va_start (args, format);
  vsprintf (buf,format, args);
  Serial.print(buf);
  va_end (args);
}