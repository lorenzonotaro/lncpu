output = ""

def main():
    global output
    with open("../lnasm/src/main/resources/opcodes.tsv", "r") as opcodes_tsv, open("aiprompt.txt", "w") as out:
        opcodes = opcodes_tsv.readlines()

        for opcode in opcodes[1:]:
            opcode = opcode.strip().split("\t")
            output += f"{immediate_to_lnasm(opcode[1])} ; {opcode[4]}. (FLAGS modified: {opcode[5]}).\n"

        out.write(output)

def convert_immediate_param(param: str):
    if param == "cst":
        return "<byte>"
    elif param == "dcst":
        return "<dcst>"
    elif param == "page0":
        return "[<page0 address>]"
    elif param == "abs":
        return "[<full address>]"
    elif param == "ircrd":
        return "[RC:RD]"
    elif param == "rcrd":
        return "RC:RD"
    else: 
        return param.upper()

def immediate_to_lnasm(immediate: str):
    splitted = immediate.split("_")
    opcode = splitted[0]
    params = ""
    return opcode + " " + ", ".join([convert_immediate_param(e) for e in splitted[1:]])

if __name__ == "__main__":
    main()