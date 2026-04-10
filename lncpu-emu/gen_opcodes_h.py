# Takes in a .csv file, and for each entry generates a #DEFINE with the corresponding opcode

import csv
import sys


def sanitize(param):
    return param.replace("$", "_").upper()


if __name__ == "__main__":
    csv_filename = sys.argv[1] if len(sys.argv) > 1 else "../v1/controlunit/opcodes.tsv"
    with open(csv_filename, 'r') as csvfile, open('opcodes.h', 'w') as hfile:
        reader = list(csv.reader(csvfile, delimiter='\t'))[1:]
        print(f"Read {len(reader)} opcodes from {csv_filename}")
        hfile.write("#ifndef LNCPU_EMU_OPCODES_H\n#define LNCPU_EMU_OPCODES_H\n\n#include <stdint.h>\n\n")
        
        hfile.write("struct opcode_info_t {\n\tconst uint8_t opcode; const char *mnemonic;\n\tconst uint8_t data_length;\n\tconst uint8_t clock_cycles;\n};\n\n")

        hfile.write("enum Opcode {") 
        for row in reader:
            hfile.write(f"\tOP_{sanitize(row[1])} =\t{row[0]}, \n")
        hfile.write("\n};")

        hfile.write("\n\nstatic const struct opcode_info_t opcode_info[] = {\n")
        for row in reader:
            hfile.write(f"\t{{OP_{sanitize(row[1])}, \"{row[1]}\", {row[2]}, {row[3]}}},\n")

        hfile.write("};\n\n#endif //LNCPU_EMU_OPCODES_H\n")