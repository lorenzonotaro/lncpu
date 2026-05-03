@echo off
setlocal enabledelayedexpansion

echo Building ln-bios...
cd ..\ln-bios
call wsl -- "./make.sh"

if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

echo Building test-os
cd ..\test-os

REM Clean up
echo Cleaning up...
del /q test-os.out.imm.txt test-os.out.ir.txt test-os.out.lnasm test-os.out

set "SOURCES="
for /r "src" %%f in (*.lnc *.lnasm) do (
    set "SOURCES=!SOURCES! "%%f""
)


echo Source files: !SOURCES!

call lnc !SOURCES! -oD="D0" -oI="test-os.out.imm.txt" -oM="test-os.out.ir.txt" -oA="test-os.out.lnasm" -oB="test-os.out" -S="..\ln-bios\bios.sym" -I="..\ln-bios\include"

if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

if "%1"=="--run" (
    echo Running project...
    lncpu_emu --rom="..\ln-bios\bios.out" -t D1 --d0=test-os_D0.out 
)
endlocal
