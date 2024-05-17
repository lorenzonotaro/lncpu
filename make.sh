cd "$(dirname "$0")"


# clear previous output
rm -rf output/*

# make output directory
mkdir -p output/eeproms/

# === make EEPROMs ===
cd v1/controlunit

echo "Generating EEPROMs..."

python3 gen_eeproms.py > ../../log.txt

if [ $? -ne 0 ]; then
    echo "Error: EEPROM generation failed"
    exit 1
fi

# move EEPROM*.bin to ../../output/eeproms/
mv EEPROM*.bin ../../output/eeproms/


# === make lnasm ===

cd ../../lnasm

echo "Building lnasm..."

mvn clean package > log.txt

if [ $? -ne 0 ]; then
    echo "Error: lnasm build failed"
    exit 1
fi

mv target/lnasm.jar ../output/

# === make eeprom-serial-loader ===

cd ../eeprom-serial-loader

echo "Building eeprom-serial-loader..."

mvn clean package > log.txt

if [ $? -ne 0 ]; then
    echo "Error: eeprom-serial-loader build failed"
    exit 1
fi

mv target/eeprom-serial-loader.jar ../output/


echo "Done."
exit 0