#ifndef COMPUTER8BIT_H
#define COMPUTER8BIT_H

#include <Arduino.h>

class Computer8Bit{
private:
	byte clockPin;
	byte b0, b1, b2, b3, b4, b5, b6, b7;
public:
	Computer8Bit(byte clock, byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7);

	/* Sets the clock status.*/
	void clock(int status);

	/* Pulses the clock for the given length.*/
	void clockPulse(unsigned int length);

	/* Writes the given values to the bus. 
 	* If values is equal to NULL, Every channel of the bus is set to LOW.
 	* Note: busMode(OUTPUT) mut be called before calling this function. */
	void writeToBus(bool values[]);

	/* Reads the bus content and stores it in values. Note: busMode(INPUT) mut be called before calling this function. */
	void readFromBus(bool values[]);

	/* Calls pinMode(..., mode) for every bus pin.*/
	void busMode(int mode);

	/* Sets the clock pin. */
	void setClockPin(byte clock);

	/* sets the bus pins*/
	void setBusPins(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7);
};

/* Converts the given number to its binary form and stores the bits in dest. */
void tobinary(byte number, bool dest[]);

	/* Returns the decimal value of the number whose binary form is contained in bits. */
byte tobyte(bool bits[]);

	/* Flushes the serial input stream. */
void serialFlush();

	/* Writes formatted output to the serial port. */
void serialPrintf(char *format, ...);

#endif