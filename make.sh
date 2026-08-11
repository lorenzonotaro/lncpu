cd "$(dirname "$0")"

# config variables
build_lnc=true
build_eeprom_serial_loader=true
make_eeproms=true
make_emu=true
make_lnfsutils=true

for arg in "$@"; do

    if [ $arg == "--all" ]; then
        build_lnc=true
        build_eeprom_serial_loader=true
        make_eeproms=true
        make_emu=true
        make_lnfsutils=true
    elif [ $arg == "--lnc-only" ]; then
        build_lnc=true
        build_eeprom_serial_loader=false
        make_eeproms=false
        make_emu=false
        make_lnfsutils=false
    elif [ $arg == "--eeprom-serial-loader-only" ] || [ $arg == "--esl-only" ]; then
        build_lnc=false
        build_eeprom_serial_loader=true
        make_eeproms=false
        make_emu=false
        make_lnfsutils=false
    elif [ $arg == "--eeproms-only" ]; then
        build_lnc=false
        build_eeprom_serial_loader=false
        make_eeproms=true
        make_emu=false
        make_lnfsutils=false
    elif [ $arg == "--lncpu-emu-only" ] || [ $arg == "--emu-only" ]; then
        build_lnc=false
        build_eeprom_serial_loader=false
        make_eeproms=false
        make_emu=true
        make_lnfsutils=false
    elif [ $arg == "--lnfsutils-only" ] || [ $arg == "--lnfs-only" ]; then
        build_lnc=false
        build_eeprom_serial_loader=false
        make_eeproms=false
        make_emu=false
        make_lnfsutils=true
    elif [ "$arg" == "--no-lnc" ]; then
        build_lnc=false
    elif [ "$arg" == "--no-eeprom-serial-loader" ] || [ "$arg" == "--no-esl" ]; then
        build_eeprom_serial_loader=false
    elif [ "$arg" == "--no-eeproms" ]; then
        make_eeproms=false
    elif [ "$arg" == "--no-lncpuemu" ] || [ "$arg" == "--no-emu" ]; then
        make_emu=false
    elif [ "$arg" == "--no-lnfsutils" ] || [ "$arg" == "--no-lnfs" ]; then
        make_lnfsutils=false
    else
        echo "Unknown argument: $arg"
        echo "Usage: make.sh [--no-lnc] [--no-eeprom-serial-loader|--no-esl] [--no-eeproms] [--no-lncpuemu] [--no-lnfsutils|--no-lnfs] [--lnc-only] [--eeprom-serial-loader-only|--esl-only] [--eeproms-only] [--lncpu-emu-only|--emu-only] [--lnfsutils-only|--lnfs-only] [--all]"
        exit 1
    fi

done

# make output directory
mkdir -p output/eeproms/

# === make eeprom-serial-loader ===

if [ $build_eeprom_serial_loader = true ] ; then
    
    cd eeprom-serial-loader

    echo "Building eeprom-serial-loader..."

    mvn clean package

    if [ $? -ne 0 ]; then
        echo "Error: eeprom-serial-loader build failed"
        exit 1
    fi

    cp target/eeprom-serial-loader.jar ../output/

    # generate run cmd/bash for eeprom-serial-loader
    echo "java -jar %~dp0\eeprom-serial-loader.jar %*" > "../output/eeprom-serial-loader.bat"
    echo -e "#!/bin/bash\njava -jar \"\$(dirname "\$0")/eeprom-serial-loader.jar\" \"\$@\"" > "../output/eeprom-serial-loader"

    chmod +x ../output/eeprom-serial-loader

    cd ..
fi

# === make EEPROMs ===

if [ $make_eeproms = true ] ; then

    cd v1/controlunit

    echo "Generating EEPROMs..."

    python3 gen_eeproms.py

    if [ $? -ne 0 ]; then
        echo "Error: EEPROM generation failed"
        exit 1
    fi

    # === copy opcodes.tsv to lnc ===
    echo "Copying opcodes.tsv to lnc..."
    cp opcodes.tsv ../../lnc/src/main/resources/

    # === generate EEPROM binary files ===

    echo "Generating EEPROM binary files..."

    # for each .EEPROM*.eeprom file in v1/controlunit/, run eeprom-serial-loader to generate EEPROM*.bin in the cwd
    #subprocess.run(f"java -jar \"../../eeprom-serial-loader/target/eeprom-serial-loader.jar\" EEPROM{str(i)}.eeprom --no-gui --export-bin EEPROM{str(i)}.bin", shell=True)

    for eeprom in *.eeprom; do
        java -jar "../../output/eeprom-serial-loader.jar" "$eeprom" --no-gui --export-bin ../../output/eeproms/"${eeprom/.eeprom/.bin}"
    done
    
    cd ../..

fi

# === make lnc ===

if [ $build_lnc = true ] ; then

    cd lnc

    echo "Building lnc..."

    mvn package

    if [ $? -ne 0 ]; then
        echo "Error: lnc build failed"
        exit 1
    fi

    cp target/lnc.jar ../output/

    # copy lnc/lib to output, overriding if necessary
    cp -r lib/ ../output/

    # generate run cmd/bash for lnc
    echo "java -jar %~dp0\lnc.jar %*" > "../output/lnc.bat"
    cp ../output/lnc.bat ../output/lnasm.bat
    echo -e "#!/bin/bash\njava -jar \"\$(dirname "\$0")/lnc.jar\" \"\$@\"" > "../output/lnc"
    chmod +x ../output/lnc
    cp ../output/lnc ../output/lnasm
    chmod +x ../output/lnasm
    # === generate lnasm documentation ===

    echo "Generating lnasm instruction set documentation..."

    python3 gen_language_docs.py

    cd ..
fi

if [ $make_lnfsutils = true ] ; then

    cd lnfsutils

    echo "Building lnfsutils..."

    mvn package

    if [ $? -ne 0 ]; then
        echo "Error: lnfsutils build failed"
        exit 1
    fi

    cp target/lnfsutils.jar ../output/

    # generate run cmd/bash for lnfsutils
    echo "java -jar %~dp0\lnfsutils.jar %*" > "../output/lnfsutils.bat"
    echo -e "#!/bin/bash\njava -jar \"\$(dirname "\$0")/lnfsutils.jar\" \"\$@\"" > "../output/lnfsutils"
    chmod +x ../output/lnfsutils

    cd ..
fi

# === make lncpu-emu ===
if [ $make_emu = true ] ; then

    echo "Building lncpu-emu..."

    cd lncpu-emu

    python ./gen_opcodes_h.py

    #if build dir does not exists, create it
    mkdir -p build
    cd build

    cmake ..
    if [ $? -ne 0 ]; then
        echo "Error: gen_opcodes_h.py failed"
        exit 1
    fi

    cmake --build . --config Release

    if [ $? -ne 0 ]; then
        echo "Error: lncpu-emu build failed"
        exit 1
    fi

    # copy the executable to output
    if [ -f Release/lncpu_emu.exe ]; then
        cp Release/lncpu_emu.exe ../../output/lncpu_emu.exe
    elif [ -f lncpu_emu.exe ]; then
        cp lncpu_emu.exe ../../output/lncpu_emu.exe
    elif [ -f Release/lncpu_emu ]; then
        cp Release/lncpu_emu ../../output/lncpu_emu
    elif [ -f lncpu_emu ]; then
        cp lncpu_emu ../../output/lncpu_emu
    else
        echo "Error: lncpu-emu executable not found"
        exit 1
    fi

fi

echo "Done."
exit 0
