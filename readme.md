# lncpu

LNCPU is a design for a hobby 8-bit processor. 

![Alt text](v1/logisim/sample.png)

## Project structure

Overview of the repository:
* [`v1`](v1) is the first implementation of the lncpu. Here you will find the Logisim simulation and the KiCad project (currently under construction), as well as the Python script for generating the control unit EEPROMS.

* [`lnc`](lnc) is an assembler and C compiler written in Java for the lncpu. It is kept up to date with the current implementation.

* [`eeprom-serial-loader`](eeprom-serial-loader) is a utility program for editing binary data and loading it to/from EEPROMS.

* [`programs`](programs) contains some sample programs you can assemble and run on the lncpu as well as Notmon, a utility program strongly inspired in its functionality (and name) by Wozmon, made by Steve Wozniak for his Apple I.

## Prequisites

- [Maven](https://maven.apache.org/) is required for building both `lnc` and `eeprom-serial-loader`.
- [Python 3.x](https://www.python.org/downloads/) is required to generate the EEPROMs for the control unit.
- [Logisim-evolution](https://github.com/logisim-evolution/logisim-evolution) and [Digital](https://github.com/hneemann/Digital) are used to simulate the design.

## Building

If you're on Linux, `make.sh` will build everything for you, including lnc, eeprom-serial-loader and the control unit EEPROMs. Usage

    ./make.sh [--no-eeproms] [--no-eeprom-serial-loader|--no-esl] [--no-lnc]



**Note**: I suggest using [my fork of Logisim-evolution](https://github.com/lorenzonotaro/logisim-evolution) to open the Logisim project. It fixes some performance issues and adds Probe breakpoints, useful for debugging the lncpu.