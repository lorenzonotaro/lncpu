#!/usr/local/bin/python3.7
import itertools
import shutil
import math
import os
from collections import OrderedDict
from math import ceil
import subprocess
import hashlib

# ordered list of control signals
import json

OPCODES_TSV = 'opcodes.tsv'
EEPROM_FILES = []
EEPROM_HASHES = []

# the size of each instruction in the eeproms, in bytes
INSTR_SIZE = 16

# whether to fill the rest of the EEPROMs with the default signals
# this is necessary for the actual circuit, but not for simulation
FILL_EEPROMS = True

# the size of the EEPROMs in bytes
EEPROM_SIZE = 8192

with open('signals_meta.json') as file:
    SIGNALS_META = json.load(file, object_pairs_hook=OrderedDict)
    SIGNALS_DICT = {item['name']: item for item in SIGNALS_META['signals']}
    SIGNALS = OrderedDict({item['name']: item['default_value'] for item in SIGNALS_META['signals']})

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



def compose_comb_group(group : str, count : int, edits : OrderedDict):
    return ''.join([str({**SIGNALS, **edits}[group + str(i)]) for i in range(count)])

# merges the default signal states with the given modifications made by each microinstruction
# then checks whether there is a bus conflict (e.g. multiple components writing to the bus)
def make_microinstruction(instr_name, microinst_index, edits=None):
    if edits is None:
        return {**SIGNALS}

    dbus_writes = {key: value for key, value in edits.items()
                   if not key.startswith('_UNUSED')
                   and value != SIGNALS_DICT[key]['default_value']
                   and type(SIGNALS_DICT[key]['behavior']) == OrderedDict and
                   SIGNALS_DICT[key]['behavior']['dbus_write']}
    combinatorial_dbus_writes = {group: 1 for group, behavior in SIGNALS_META['behavior_groups'].items() if behavior[compose_comb_group(group, int(math.log2(len(behavior))), edits)]['dbus_write']}
    dbus_writes = {**dbus_writes, **combinatorial_dbus_writes}
    if len(dbus_writes) > 1:
        tostr = ', '.join([key for key, value in dbus_writes.items()])
        print(f'Instruction {instr_name}[{microinst_index}] has multiple bus writes: {tostr}')
        raise AssertionError

    dbus_reads = {key: value for key, value in edits.items()
                   if not key.startswith('_UNUSED')
                   and value != SIGNALS_DICT[key]['default_value']
                   and type(SIGNALS_DICT[key]['behavior']) == OrderedDict and
                   SIGNALS_DICT[key]['behavior']['dbus_read']}
    combinatorial_dbus_reads = {group: 1 for group, behavior in SIGNALS_META['behavior_groups'].items() if behavior[compose_comb_group(group, int(math.log2(len(behavior))), edits)]['dbus_read']}
    dbus_reads = {**dbus_reads, **combinatorial_dbus_reads}
    if len(dbus_writes) > 1:
        tostr = ', '.join([key for key, value in dbus_reads.items()])
        print(f'Instruction {instr_name}[{microinst_index}] has multiple bus reads: {tostr}')
        raise AssertionError

    if len(dbus_writes) == 1 and len(dbus_reads) == 0:
        print(f'Instruction {instr_name}[{microinst_index}] has one bus write ({list(dbus_writes.keys())[0]}) but no bus reads.')
    elif len(dbus_reads) == 1 and len(dbus_writes) == 0:
        print(f'Instruction {instr_name}[{microinst_index}] has one bus read ({list(dbus_reads.keys())[0]}) but no bus writes.')
    elif len(dbus_reads) > 1:
        print(f'Instruction {instr_name}[{microinst_index}] has multiple bus reads: {", ".join(dbus_reads.keys())}')
    elif len(dbus_writes) > 1:
        print(f'Instruction {instr_name}[{microinst_index}] has multiple bus writes: {", ".join(dbus_writes.keys())}')

    microcode = {**SIGNALS, **edits}

    return microcode


# writes to each .eeprom file the correct signals
def write_to_eeproms(address, byteLabel, microinstr):
    list = [(name, value) for name, value in microinstr.items()]
    for i in range(0, EEPROM_COUNT):
        eeprom = EEPROM_FILES[i]
        value = sum(list[i * 8 + n][1] * pow(2, n) for n in range(0, 8))
        bitLabels = [list[i * 8 + 7 - n][0] for n in range(0, 8)]
        eeprom.append({"address": address, "value": value, "byteLabel": byteLabel, "bitLabels": bitLabels})


with open('instructions.json') as file, open(OPCODES_TSV, mode='w') as opcodes_tsv:
    data = json.load(file)
    opcodes_tsv.write('Opcode\tName\tData length\tClock cycles\tDescription\tFlags modified\n')
    instr_addr = 0
    opcode = 0
    for elem in data:
        name = elem['name']
        instr = elem['microcode']
        addr = 0

        # ensure that every signal used in the microinstruction replaces an existing one
        for step in instr:
            items = {**step}.items()
            for signal, value in items:
                if type(value) == int:
                    # signal
                    if signal not in SIGNALS:
                        raise NameError(f"Invalid signal '{signal}' in instruction '{name}'")
                    # Check if the signal is combinatorial
                    signal_meta = next((s for s in SIGNALS_META['signals'] if s['name'] == signal), None)
                    if signal_meta and signal_meta.get('behavior') == 'combinatorial':
                        raise NameError(f"Use of isolated value for combinatorial signal '{signal}' in instruction '{name}'")
                elif type(value) == str:
                    # behavior group
                    # check if it is among the keys behavior group
                    if signal not in SIGNALS_META['behavior_groups']:
                        raise NameError(f"Invalid signal/behavior '{signal}' in instruction '{name}'")

                    # get the case in the behavior group that has its "name" property set to the signal value
                    cases = {k: dict(v) for k, v in SIGNALS_META['behavior_groups'][signal].items()}
                    case = next((case for case, properties in cases.items() if
                                 properties['name'] == value), None)

                    if case is None:
                        raise NameError(f"Invalid behavior group case in '{signal}' in instruction '{name}'")

                    # Replace the behavior group in the step with the generated signals from the case name.
                    # the case name is a binary string. Each generated signal is composed of the behavior group name + its digit.
                    # Signal 0 is the rightmost digit.

                    for i, bit in enumerate(reversed(case)):
                        step[signal + str(i)] = int(bit)

                    # remove the behavior group from the step
                    del step[signal]


                else:
                    raise TypeError(f"Invalid signal value type '{type(value)}' in instruction '{name}'")

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
        
        try:
            string = hex(opcode) + '\t' + name + '\t' + str(codesize) + '\t' + str(len(all_steps)) + '\t' + elem["description"] + '\t' + elem["flags_modified"] + '\n'
        except KeyError as e:
            print(f"Error in instruction '{name}': missing key {e}")
            exit(1)
        opcodes_tsv.write(string)

        # pad the clock cycles with the default signals
        # this isn't needed, it's just in case the instruction fails to pass
        # and runs through the remaining clock cycles
        while len(all_steps) < INSTR_SIZE:
            all_steps = all_steps + [SIGNALS]

        # eeprom files
        for i, step in enumerate(all_steps):
            try:
                microinstruction = make_microinstruction(name, i, step)
            except AssertionError:
                exit(1)
            write_to_eeproms(instr_addr + addr, name + '.' + str(addr), microinstruction)
            addr = addr + 1

        instr_addr = instr_addr + INSTR_SIZE
        opcode = opcode + 1

    if FILL_EEPROMS:
        for i in range(instr_addr, EEPROM_SIZE, 16):
            for j in range(0, INSTR_SIZE):
                write_to_eeproms(i + j, 'FILLER_NOP', SIGNALS)

def sha256sum(filename):
    h  = hashlib.sha256()
    b  = bytearray(128*1024)
    mv = memoryview(b)
    with open(filename, 'rb', buffering=0) as f:
        for n in iter(lambda : f.readinto(mv), 0):
            h.update(mv[:n])
    return h.hexdigest()

for i in range(EEPROM_COUNT):
    filename = f"EEPROM{i}.eeprom"
    if(os.path.isfile(filename)):
        EEPROM_HASHES.append(sha256sum(filename))
    else:
        EEPROM_HASHES.append("0")


print("Generating EEPROM files")
for i, data in enumerate(EEPROM_FILES):
    with open('EEPROM' + str(i) + ".eeprom", mode='w') as file:
        json.dump(data, file)

for i in range(EEPROM_COUNT):
    filename = f"EEPROM{i}.eeprom"
    if EEPROM_HASHES[i] != sha256sum(filename):
        print(f"EEPROM{i} changed.")
