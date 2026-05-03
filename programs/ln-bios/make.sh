SRC_FILES=$(find src/lnasm/*/*.lnasm src/lnasm/*.lnasm src/lnc/*.lnc src/lnc/*/*.lnc -type f -nowarn | xargs echo)
echo Source files: $SRC_FILES
lnc $SRC_FILES -lf linker.cfg -oD="ROM,RAM,D1" -oA bios.lnasm -oB bios.out -oS bios.sym -oI bios.immediate.txt -oM bios.intermediate.txt -oA="__lncout.lnasm" -I="include/"