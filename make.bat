@echo off
cd /d "%~dp0"

REM config variables
set build_lnc=true
set build_eeprom_serial_loader=true
set make_eeproms=true
set make_emu=true

REM parse command line arguments
:parse_args
if "%~1"=="" goto args_done
if "%~1"=="--no-lnc" (
    set build_lnc=false
    shift
    goto parse_args
)
if "%~1"=="--no-eeprom-serial-loader" (
    set build_eeprom_serial_loader=false
    shift
    goto parse_args
)
if "%~1"=="--no-esl" (
    set build_eeprom_serial_loader=false
    shift
    goto parse_args
)
if "%~1"=="--no-eeproms" (
    set make_eeproms=false
    shift
    goto parse_args
)
if "%~1"=="--no-emu" (
    set make_emu=false
    shift
    goto parse_args
)

echo Unknown argument: %~1
echo Usage: make.bat [--no-lnc] [--no-eeprom-serial-loader^|--no-esl] [--no-eeproms] [--no-emu]
exit /b 1

:args_done

REM make output directory
if not exist output\eeproms mkdir output\eeproms

REM === make eeprom-serial-loader ===

if "%build_eeprom_serial_loader%"=="true" (
    
    cd eeprom-serial-loader

    echo Building eeprom-serial-loader...

    call mvn clean package

    if errorlevel 1 (
        echo Error: eeprom-serial-loader build failed
        exit /b 1
    )

    copy target\eeprom-serial-loader.jar ..\output\

    REM generate run cmd/bash for eeprom-serial-loader
    echo java -jar %%~dp0\eeprom-serial-loader.jar %%* > "..\output\eeprom-serial-loader.bat"
    echo #!/bin/bash > "..\output\eeprom-serial-loader"
    echo java -jar "$(dirname "$0")/eeprom-serial-loader.jar" "$@" >> "..\output\eeprom-serial-loader"

    cd ..
)

REM === make EEPROMs ===

if "%make_eeproms%"=="true" (

    cd v1\controlunit

    echo Generating EEPROMs...

    python gen_eeproms.py

    if errorlevel 1 (
        echo Error: EEPROM generation failed
        exit /b 1
    )

    REM === copy opcodes.tsv to lnc ===
    echo Copying opcodes.tsv to lnc...
    copy opcodes.tsv ..\..\lnc\src\main\resources\

    REM === generate /lncpu-emu/opcodes.h ===
    echo Generating lncpu-emu/opcodes.h...
    python ..\..\lncpu-emu\gen_opcodes_h.py opcodes.tsv

    REM === generate EEPROM binary files ===

    echo Generating EEPROM binary files...

    REM for each .EEPROM*.eeprom file in v1/controlunit/, run eeprom-serial-loader to generate EEPROM*.bin in the cwd
    for %%f in (*.eeprom) do (
        java -jar "..\..\output\eeprom-serial-loader.jar" "%%f" --no-gui --export-bin "..\..\output\eeproms\%%~nf.bin"
    )
    
    cd ..\..

)

REM === make lnc ===

if "%build_lnc%"=="true" (

    cd lnc

    echo Building lnc...

    call mvn package

    if errorlevel 1 (
        echo Error: lnc build failed
        exit /b 1
    )

    copy target\lnc.jar ..\output\

    REM copy lnc/lib to output, overriding if necessary
    xcopy lib\ ..\output\lib\ /E /Y

    REM generate run cmd/bash for lnc
    echo java -jar %%~dp0\lnc.jar %%* > "..\output\lnc.bat"
    copy "..\output\lnc.bat" "..\output\lnasm.bat"
    echo #!/bin/bash > "..\output\lnc"
    echo java -jar "$(dirname "$0")/lnc.jar" "$@" >> "..\output\lnc"
    copy "..\output\lnc" "..\output\lnasm"

    REM === generate lnasm documentation ===

    echo Generating lnasm instruction set documentation...

    python gen_language_docs.py

    cd ..
)

REM === make lncpu-emu ===

if "%make_emu%"=="true" (

    cd lncpu-emu

    echo Building lncpu-emu...

    REM If build exists, delete it and recreate it

    if exist build rmdir /s /q build
    mkdir build

    cd build

    REM try with MinGW
    cmake .. -G "MinGW Makefiles"

    if errorlevel 1 (
        echo CMake configuration with MinGW failed, trying with default generator...
        
        cd ..
        rmdir /s /q build
        mkdir build

        cd build

        cmake ..
    )

    if errorlevel 1 (
        echo Error: CMake configuration failed
        exit /b 1
    )

    cmake --build . --config Release

    if errorlevel 1 (
        echo Error: lncpu-emu build failed
        exit /b 1
    )

    if exist Release\lncpu_emu.exe (
        copy Release\lncpu_emu.exe ..\..\output\lncpu_emu.exe
    ) else (
        if exist Release\lncpu-emu (
            copy Release\lncpu-emu ..\..\output\lncpu_emu
        ) else (
            echo Error: lncpu-emu executable not found
            exit /b 1
        )
    )
)

echo Done.
exit /b 0