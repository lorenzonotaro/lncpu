#!/usr/local/bin/python3.7
import os
from math import ceil
import subprocess
import hashlib

# ordered list of control signals
import json

OPCODES_TSV = 'opcodes.tsv'
EEPROM_FILES = []
EEPROM_HASHES = []

SIGNALS = {
    "NOT_RA_IN": 1, "NOT_RA_OUT": 1, "NOT_RA_SL": 1, "NOT_RA_SR": 1, "NOT_RB_IN": 1, "NOT_RB_OUT": 1, "NOT_RC_IN": 1,
    "NOT_RC_OUT": 1,
    "NOT_RD_IN": 1, "NOT_RD_OUT": 1, "NOT_MDS_IN": 1, "NOT_MAR_IN": 1, "NOT_SS_IN": 1, "NOT_SP_IN": 1, "NOT_SP_CT": 1,
    "SP_DIR": 0,
    "NOT_SDS_IN": 1, "NOT_SAR_IN": 1, "NOT_RT_IN": 1, "NOT_RT_OUT": 1, "X_SEL0": 0, "X_SEL1": 0, "Y_SEL0": 0, "Y_SEL1": 0, "OP_SEL0": 0, "OP_SEL1": 0,
    "OP_SEL2": 0, "NOT_RAM_IN": 1, "NOT_RAM_OUT": 1, "RAM_AS": 0, "NOT_ROM_OUT": 1, "ROM_AS": 0, "NOT_CS_IN": 1, "NOT_PC_IN": 1,
    "NOT_PC_INC": 1,
    "NOT_CS_OUT": 1, "NOT_PC_OUT": 1, "NOT_IR_IN": 1, "LCD_EN": 0, "LCD_RS": 0, "LCD_R_NW": 0,
    "NOT_FI": 1, "NOT_FLAGS_RS": 1, "J": 0,
    "JC": 0, "JZ": 0, "JN": 0, "JA": 0, "NOT_PASS": 1, "NOT_HLT" : 1, "I0_POLL": 0, "I0_TR": 0, "I1_POLL": 0, "I1_TR": 0, "I2_POLL": 0, "I2_TR": 0,
}

EEPROM_COUNT = ceil(len(SIGNALS) / 8)
for i in range(0, EEPROM_COUNT):
    EEPROM_FILES.append([])

if len(SIGNALS) != EEPROM_COUNT * 8:
    raise RuntimeError("invalid signal count")

# micro-instructions to be executed before every instruction
I_START = [
    {"NOT_ROM_OUT": 0, "ROM_AS": 0, "NOT_IR_IN": 0},
    {"NOT_PC_INC": 0}
]
# micro-instructions to be executed after every instruction
I_END = [
    {"NOT_PASS": 0}
]

#merges the default signal states with the given modifications made by each microinstruction
def make_microinstruction(edits=None):
    if edits is None:
        edits = {}
    return {**SIGNALS, **edits}

# writes to each .eeprom file the correct signals
def write_to_eeproms(address, byteLabel, microinstr):
    list = [(name, value) for name, value in microinstr.items()]
    for i in range(0, EEPROM_COUNT):
        eeprom = EEPROM_FILES[i]
        value = sum(list[i * 8 + n][1] * pow(2, n) for n in range(0, 8))
        bitLabels = [list[i * 8 + 7 - n][0] for n in range(0, 8)]
        eeprom.append({"address": address, "value": value, "byteLabel": byteLabel, "bitLabels": bitLabels})
        pass


with open('instructions.json') as file:
    data = json.load(file)
    opcodes_tsv = open(OPCODES_TSV, mode='w')
    opcodes_tsv.write('Opcode\tName\tData length\tClock cycles\n')
    instr_addr = 0
    opcode = 0
    for name, instr in data.items():
        addr = 0

        # ensure that every signal used in the microinstruction replaces an existing one
        for step in instr:
            for value, signal in enumerate(step):
                if signal not in SIGNALS:
                    raise NameError(f"Invalid signal '{signal}' in instruction '{name}'")

        # opcodes.tsv
        codesize = 1 + sum(
            1 for step in instr if "NOT_PC_INC" in step)  # count how many times the instruction increments PC
        string = hex(opcode) + '\t' + name + '\t' + str(codesize) + '\t' + str(len(I_START) + len(instr) + len(I_END)) + '\n'
        opcodes_tsv.write(string)

        all_steps = I_START + instr + I_END

        while len(all_steps) < 16:
            all_steps = all_steps + [SIGNALS]

        # eeprom files
        for step in all_steps:
            write_to_eeproms(instr_addr + addr, name + '.' + str(addr), make_microinstruction(step))
            addr = addr + 1

        instr_addr = instr_addr + 16
        opcode = opcode + 1


def sha256sum(filename):
    h  = hashlib.sha256()
    b  = bytearray(128*1024)
    mv = memoryview(b)
    with open(filename, 'rb', buffering=0) as f:
        for n in iter(lambda : f.readinto(mv), 0):
            h.update(mv[:n])
    return h.hexdigest()

for i in range(EEPROM_COUNT):
    filename = f"EEPROM{i}.raw"
    if(os.path.isfile(filename)):
        EEPROM_HASHES.append(sha256sum(filename))
    else:
        EEPROM_HASHES.append("0")


for i, data in enumerate(EEPROM_FILES):
    with open('EEPROM' + str(i) + ".eeprom", mode='w') as file:
        json.dump(data, file)
    subprocess.run(["java", "-jar","../Utilities/eeprom-serial-loader.jar", "EEPROM" + str(i) + ".eeprom", "--no-gui", "--export-raw", "EEPROM" + str(i) + ".raw"])

for i in range(EEPROM_COUNT):
    filename = f"EEPROM{i}.raw"
    if(EEPROM_HASHES[i] != sha256sum(filename)):
        print(f"EEPROM{i} changed.")
