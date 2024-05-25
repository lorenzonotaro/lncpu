cd "$(dirname "$0")"

# config variables
build_lnasm=true
build_eeprom_serial_loader=true
make_eeproms=true

for arg in "$@"; do

    if [ "$arg" == "--no-lnasm" ]; then
        build_lnasm=false
    elif [ "$arg" == "--no-eeprom-serial-loader" ]; then
        build_eeprom_serial_loader=false
    elif [ "$arg" == "--no-eeproms" ]; then
        make_eeproms=false
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
    echo -e "#!/bin/bash\njava -jar \"$(dirname "$0")/eeprom-serial-loader.jar\" \"\$@\"" > "../output/eeprom-serial-loader"

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

    # === generate EEPROM binary files ===

    echo "Generating EEPROM binary files..."

    # for each .EEPROM*.eeprom file in v1/controlunit/, run eeprom-serial-loader to generate EEPROM*.bin in the cwd
    #subprocess.run(f"java -jar \"../../eeprom-serial-loader/target/eeprom-serial-loader.jar\" EEPROM{str(i)}.eeprom --no-gui --export-bin EEPROM{str(i)}.bin", shell=True)

    for eeprom in *.eeprom; do
        java -jar "../../output/eeprom-serial-loader.jar" "$eeprom" --no-gui --export-bin ../../output/eeproms/"${eeprom/.eeprom/.bin}"
    done

    # move EEPROM*.bin to ../../output/eeproms/
    cp EEPROM*.bin ../../output/eeproms/

    cd ../..

fi

# === make lnasm ===

if [ $build_lnasm = true ] ; then

    cd lnasm

    echo "Building lnasm..."

    mvn clean package

    if [ $? -ne 0 ]; then
        echo "Error: lnasm build failed"
        exit 1
    fi

    cp target/lnasm.jar ../output/

    # generate run cmd/bash for lnasm
    echo "java -jar %~dp0\lnasm.jar %*" > "../output/lnasm.bat"
    echo -e "#!/bin/bash\njava -jar \"$(dirname "$0")/lnasm.jar\" \"\$@\"" > "../output/lnasm"

    # === generate lnasm documentation ===

    echo "Generating lnasm instruction set documentation..."

    python3 gen_language_docs.py

fi

echo "Done."
exit 0