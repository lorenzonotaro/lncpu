# Serial protocol

## 1. General information
A loader is a program running on a serial device that will communicate with the desktop application and will load the data onto the target chip (each loader is specific to the chip, since they may require different wiring).

The serial protocol was built around Arduino Nano, so the Arduino environment is the easiest choice. That said, the protocol can work with any microcontroller with a serial port.

The loader is the interface between the client and the EEPROM chip. It needs to be able to write and read from the EEPROM and send/receive data accordingly.

In its default state, the loader must be constantly waiting for requests from the serial port.
As explained below, since most microcontrollers have a very limited amount of memory, the exchange of EEPROM data from and to the loader will happen in smaller blocks, or payloads, of a size determined by the loader.

## 2. Serial parameters & info
* The baud rate must be `115200` (the highest baud rate supported by Arduino Nano).
* The other serial parameters are the default parameters of Arduino: `8 data bits`, `no parity`, `1 stop bit`.
* The loader must also have a 1-second read timeout when expecting data or confirmation from the client, but not during the default state of listening for requests.
* Whenever a fixed number of bytes is exchanged between the loader and the client (e.g. a 2-byte unsigned integer, see parameters), the data must be sent in Big Endian order.
## 3. Requests
The loader must be listening for requests sent by the client application. These requests are in the form of a single ASCII character.

#### 3.1 `'v' (0x76)`: Signature and version
This is the first request a newly connected client will make.

The loader must respond with the following data (in order):
1. the protocol signature string `EEPROMLD` (8 bytes), allowing the client to identify the device as a valid loader.
2. a null-terminated string containing the name and version of the loader. 

#### 3.2 `'p'` (0x70): Parameters
This request usually follows the signature request. Before any uploading is done, the client needs to know:
 * the maximum read/write length of the loader: the amount of memory that the loader is capable of addressing in the EEPROM (ideally the size of the EEPROM); the client will use this number to notify the user when they're writing past the limit of the loader.
 * the payload size: the size of each block containing EEPROM data. When writing and reading, the client will know how may blocks to expect by calculating `(maximum read-write length)/(payload size)`.

The loader must respond with the following data (in order):
1. The maximum read/write length in bytes, expressed as a 2-byte unsigned integer.
2. The payload size in bytes, expressed as a 2-byte unsigned integer.

#### 3.3 `'r'` (0x72): Read
The client is requesting the contents of the EEPROM.

The loader must enter the read state: any request character sent during this period must not be considered.

At this point the loader must perform a number of read cycles equal to `(maximum read-write length)/(payload size)`.

A read cycle consists of the following steps:
1. The loader must wait for the client to send the control character `'n'` (0x6E)', meaning that the client is ready to receive the next payload. If the byte read is different than `'n'` (0x6E)' or if the timeout (1 second) is reached the loader must send a `'!'` (0x21) character and return to its default state.
2. The loader must send a number of bytes equal to the payload size, corresponding to the current payload.

No additional data needs to be sent after the last read cycle.

#### 3. `'w'` (0x77): Write
The client is requesting to write to the EEPROM.

The loader must enter the write state: any request character sent during this period must not be considered.

At this point the loader must perform a number of write cycles equal to `(maximum read-write length)/(payload size)`.

A write cycle consists of the following steps:
1. The loader must send the control character `'n'` (0x6E)', meaning that it is ready to receive the next payload. If any other byte is sent or if the client reaches the timeout (1 second), the client will consider the write attempt failed and the loader must return to its default state.
2. The loader must read a number of bytes equal to the payload size, corresponding to the current payload.

After the last write cycle, the loader shall send the control character `'k'` (0x6B), signaling that the write procedure ended successfully, and then return to its default state.

**Note:** the client may perform a read request immediately after the termination of the write request in order to check the EEPROM contents.






