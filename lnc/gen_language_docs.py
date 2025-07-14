
base_md = """

### lnasm instruction set


| Opcode | Syntax | Description | Clock cycles | Flags Affected |
|--------|--------|-------------|--------------|----------------|
"""

def main():
    global base_md
    with open("src/main/resources/opcodes.tsv", "r") as opcodes_tsv, open("instructionset.md", "w") as out:
        opcodes = opcodes_tsv.readlines()

        for opcode in opcodes[1:]:
            opcode = opcode.strip().split("\t")
            base_md += "| {} | {} | {} | {} | {} |\n".format(f"{int(opcode[0].replace('0x', ''),16):02x}", immediate_to_lnasm(opcode[1]), opcode[4], opcode[3], ('`' + opcode[5] + '`') if opcode[5] != '-' else '-')
        
        out.write(base_md)


def convert_immediate_param(param: str):
    if param == "cst":
        return "<byte>"
    elif param == "dcst":
        return "<dcst>"
    elif param == "datap":
        return "[<data page address>]"
    elif param == "abs":
        return "[<full address>]"
    elif param == "ird":
        return "[RD]"
    elif param == "ircrd":
        return "[RC:RD]"
    elif param == "rcrd":
        return "RC:RD"
    elif param == "bpoffset":
        return "BP +/- <offset>"
    elif param == "ibpoffset":
        return "[BP +/- <offset>]"
    else: 
        return param.upper()

def immediate_to_lnasm(immediate: str):
    splitted = immediate.split("_")
    opcode = splitted[0]
    
    params = ""

    

    return "`" + opcode + " " + ",\t".join([convert_immediate_param(e) for e in splitted[1:]]) + "`"

if __name__ == "__main__":
    main()