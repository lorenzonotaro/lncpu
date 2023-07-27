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

with open('signals.json') as file:
    SIGNALS = json.load(file)

EEPROM_COUNT = ceil(len(SIGNALS) / 8)
for i in range(0, EEPROM_COUNT):
    EEPROM_FILES.append([])

if len(SIGNALS) != EEPROM_COUNT * 8:
    toadd = EEPROM_COUNT * 8 - len(SIGNALS)
    print(f"Warning: signals do not fit perfectly into EEPROMs. Adding {toadd} unused signal(s).")
    for i in range(toadd):
        SIGNALS[f'_UNUSED{i}_'] = 0

# micro-instructions to be executed after every instruction to pass to the next
I_END = {"NOT_PASS": 0, "NOT_PC_INC": 0, "NOT_FETCH": 0}

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

        # determine where the instruction needs an extra clock cycle to fetch the
        # next one or if we can optimize by fetching during the last microinstruction
        # We also check for NOT_PC_IN, low during jump instructions, because we cannot optimize
        # PC incrementation during jumps.
        all_steps = None
        keys = instr[-1].keys()
        if "NOT_PC_INC" in keys or "NOT_FETCH" in keys or "NOT_PC_IN" in keys:
            all_steps = instr + [I_END]
        else:
            all_steps = instr
            all_steps[-1] = {**(all_steps[-1]), **I_END}

        # store the instruction in opcodes.tsv
        codesize = sum(
            1 for step in all_steps if "NOT_PC_INC" in step and "CSPC_DIR" not in step)  # count how many times the instruction increments PC
        string = hex(opcode) + '\t' + name + '\t' + str(codesize) + '\t' + str(len(all_steps)) + '\n'
        opcodes_tsv.write(string)

        # pad the clock cycles with the default signals
        # this isn't needed, it's just in case the instruction fails to pass
        # and runs through the remaining clock cycles
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
