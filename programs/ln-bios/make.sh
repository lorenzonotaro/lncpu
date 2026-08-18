SRC_FILES="ln-bios.lnasm ln-bios.lnc"
echo Source files: $SRC_FILES

if [ ! -d "build" ]; then
    mkdir build
fi

lnc $SRC_FILES -lf linker.cfg -oD="ROM,RAM,D1" -oA build/bios.lnasm -oB build/bios.out -oS build/bios.sym -oI build/bios.immediate.txt -oM build/bios.intermediate.txt -oA="build/__lncout.lnasm" -I="include/"