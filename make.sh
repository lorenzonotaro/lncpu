cd "$(dirname "$0")"

# config variables
build_lnc=true
build_lncpuemu=true
build_eeprom_serial_loader=true
make_eeproms=true

for arg in "$@"; do

    if [ "$arg" == "--no-lnc" ]; then
        build_lnc=false
    elif [ "$arg" == "--no-eeprom-serial-loader" ] || [ "$arg" == "--no-esl" ]; then
        build_eeprom_serial_loader=false
    elif [ "$arg" == "--no-eeproms" ]; then
        make_eeproms=false
    elif [ "$arg" == "--no-lncpuemu" ] || [ "$arg" == "--no-emu" ]; then
        build_lncpuemu=false
    else
        echo "Unknown argument: $arg"
        echo "Usage: make.sh [--no-lnc] [--no-eeprom-serial-loader|--no-esl] [--no-eeproms] [--no-lncpuemu]"
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

    # === generate /lncpu-emu/opcodes.h ===
    echo "Generating lncpu-emu/opcodes.h..."
    python3 ../../lncpu-emu/gen_opcodes_h.py opcodes.tsv

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
    cp ../output/lnc ../output/lnasm

    # === generate lnasm documentation ===

    echo "Generating lnasm instruction set documentation..."

    python3 gen_language_docs.py

fi

# === make lncpuemu ===
if [ $build_lncpuemu = true ] ; then

    cd lncpu-emu

    echo "Building lncpu-emu..."

    mvn package

    if [ $? -ne 0 ]; then
        echo "Error: lncpuemu build failed"
        exit 1
    fi

    cp target/lncpuemu.jar ../output/

    # generate run cmd/bash for lncpuemu
    echo "java -jar %~dp0\lncpuemu.jar %*" > "../output/lncpuemu.bat"
    echo -e "#!/bin/bash\njava -jar \"\$(dirname "\$0")/lncpuemu.jar\" \"\$@\"" > "../output/lncpuemu"

    cd ..

fi

echo "Done."
exit 0
