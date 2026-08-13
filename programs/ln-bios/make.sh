SRC_FILES="ln-bios.lnasm ln-bios.lnc"
echo Source files: $SRC_FILES
lnc $SRC_FILES -lf linker.cfg -oD="ROM,RAM,D1" -oA bios.lnasm -oB bios.out -oS bios.sym -oI bios.immediate.txt -oM bios.intermediate.txt -oA="__lncout.lnasm" -I="include/"