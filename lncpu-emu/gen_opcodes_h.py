# Takes in a .csv file, and for each entry generates a #DEFINE with the corresponding opcode

import csv
import sys


def sanitize(param):
    return param.replace("$", "_").upper()


if __name__ == "__main__":
    """    csv_filename = sys.argv[1] if len(sys.argv) > 1 else "../v1/controlunit/opcodes.tsv"
    with open(csv_filename, 'r') as csvfile, open('opcodes.h', 'w') as outfile:
        reader = list(csv.reader(csvfile, delimiter='\t'))[1:]
        outfile.write("#ifndef LNCPU_EMU_OPCODES_H\n#define LNCPU_EMU_OPCODES_H\n\n enum Opcode {")
        for row in reader:
            outfile.write(f"\tOP_{sanitize(row[1])} =\t{row[0]}, \n")
        outfile.write("\n};\n#endif //LNCPU_EMU_OPCODES_H\n")"""
    with open("descriptions.tsv", "r") as file:
        values = list(csv.reader(file, delimiter="\t"))[1:]
        values = [(sanitize(r[0]), r[1]) for r in values]

    with open("descriptions.tsv", "w") as file:
        writer = csv.writer(file, delimiter="\t", lineterminator="\n")
        writer.writerows(values)