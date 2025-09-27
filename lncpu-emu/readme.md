# LNCPU-EMU

A simple emulator for the LNCPU written in C.

**Warning**: This emulator works on Windows ONLY because of the use of `<conio.h>` for keyboard input. If you want to port it to Linux or MacOS, feel free to open a PR.

---
## Building

Building LNCPU-EMU requires a C compiler (MinGW gcc recommended) and CMake. To build the emulator, follow these steps:
1. Move into the directory:

    `cd lncpu-emu`

2. Create a build directory and move into it:

    `mkdir build && cd build`
    
3. Run CMake to configure the project:

    `cmake ..`

4. Build the project:

    `cmake --build .`