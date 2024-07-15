import base64
import vertexai
import os
import time
from vertexai.generative_models import GenerativeModel, Part, FinishReason
import vertexai.preview.generative_models as generative_models

CSV_FILE = "../lnasm/src/main/resources/opcodes.tsv"
OUTPUT_FOLDER = "./"

LINKER_CFG = "SECTIONS [ CODE: type=ROM, start=0x0 ]"

with open (CSV_FILE, "r") as file:
  opcodes = file.readlines()

def generate():
  vertexai.init(project="starry-embassy-429412-c4", location="europe-west3")
  model = GenerativeModel(
    "gemini-1.5-flash-001",
    system_instruction=[textsi_1]
  )

  for opcode in opcodes[4:]:
    data = opcode.strip().split("\t")
    immediate_name = data[1]

    if os.path.isdir(OUTPUT_FOLDER + immediate_name):
      print(f"Test for {immediate_name} already exists.")
      continue

    gen_test(model, immediate_name)
    time.sleep(60)


def gen_test(model: GenerativeModel, immediate_name: str):

    lnasm_prompt = immediate_to_lnasm(immediate_name)

    responses = model.generate_content(
        [lnasm_prompt],
        generation_config=generation_config,
        safety_settings=safety_settings,
        stream=True,
    )

    test_code = ".section CODE\n\t"
    for response in responses:
        test_code += response.text.replace("\n", "\n\t")

    test_pass_conditions = ""

    # last line comments are the test pass conditions
    test_code_splitted = test_code.split("\n")
    for i in range(-1, -len(test_code_splitted), -1):
        if test_code_splitted[i].strip().startswith(";"):
            test_pass_conditions += test_code_splitted[i].replace(";", "").strip() + "\n"
            break
    
    os.makedirs(OUTPUT_FOLDER + immediate_name, exist_ok=True)

    with open(OUTPUT_FOLDER + immediate_name + "/test.lnasm", "w") as file:
        file.write(test_code)

    with open(OUTPUT_FOLDER + immediate_name + "/pass.txt", "w") as file:
        file.write(test_pass_conditions)
    
    print(f"Generated test for {immediate_name}.")

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
    return "\"" + opcode + " " + ", ".join([convert_immediate_param(e) for e in splitted[1:]]) + "\""

textsi_1 = """This is documentation for a custom assembly language for a 8-bit data bus 16-bit address bus processor.

When prompted with an instruction template, you must generate a code snippet to test that instruction.
You may use more than one instruction to setup the test.
If you use push, pop, call, brk, ret or iret instructions, you must setup the stack by setting SS=0x21 and SP=0.
In this case the stack will begin at 0x2100 and grow upwards by 1.
The stack must be within RAM address space. The stack grows upwards, SP points to the first free location.
RAM address space is 0x2000-0x3FFF.
Page 0 is the first page of RAM, 0x2000-0x20FF.
Each test must end with a hlt instruction and be followed by a comment describing test pass conditions, for example:
; RA = 0x21
; SP = 0x21
; [0] = 0x22
; ...

Do not format using Markdown.
Use numbers different than 0x0 for the tests. Use hex (0x) notation for numbers (also in the comments).

Below you will find a list of all instructions and what they do.
In mov instructions, the first parameter is the source and the second is the destination.

\"nop \" : No operation. (FLAGS modified: -).
\"hlt \" : Halts the CPU. (FLAGS modified: -).
\"brk \" : Pushes CS:PC and FLAGS, then calls the interrupt vector. (FLAGS modified: I).
\"mov <byte>, RA\" : Moves a constant to RA. (FLAGS modified: -).
\"mov <byte>, RB\" : Moves a constant to RB. (FLAGS modified: -).
\"mov <byte>, RC\" : Moves a constant to RC. (FLAGS modified: -).
\"mov <byte>, RD\" : Moves a constant to RD. (FLAGS modified: -).
\"mov <byte>, SP\" : Moves a constant to SP. (FLAGS modified: -).
\"mov <byte>, SS\" : Moves a constant to SS. (FLAGS modified: -).
\"mov SS, RD\" : Moves SS to RD. (FLAGS modified: -).
\"mov SP, RD\" : Moves SP to RD. (FLAGS modified: -).
\"mov RA, RB\" : Moves RA to RB. (FLAGS modified: -).
\"mov RA, RC\" : Moves RA to RC. (FLAGS modified: -).
\"mov RA, RD\" : Moves RA to RD. (FLAGS modified: -).
\"mov RB, RA\" : Moves RB to RA. (FLAGS modified: -).
\"mov RB, RC\" : Moves RB to RC. (FLAGS modified: -).
\"mov RB, RD\" : Moves RB to RD. (FLAGS modified: -).
\"mov RC, RA\" : Moves RC to RA. (FLAGS modified: -).
\"mov RC, RB\" : Moves RC to RB. (FLAGS modified: -).
\"mov RC, RD\" : Moves RC to RD. (FLAGS modified: -).
\"mov RD, RA\" : Moves RD to RA. (FLAGS modified: -).
\"mov RD, RB\" : Moves RD to RB. (FLAGS modified: -).
\"mov RD, RC\" : Moves RD to RC. (FLAGS modified: -).
\"mov RD, SS\" : Moves RD to SS. (FLAGS modified: -).
\"mov RD, SP\" : Moves RD to SP. (FLAGS modified: -).
\"mov RA, [<page0 address>]\" : Copies RA to a location in the address space (zero page addressing mode). (FLAGS modified: -).
\"mov RB, [<page0 address>]\" : Copies RB to a location in the address space (zero page addressing mode). (FLAGS modified: -).
\"mov RC, [<page0 address>]\" : Copies RC to a location in the address space (zero page addressing mode). (FLAGS modified: -).
\"mov RD, [<page0 address>]\" : Copies RD to a location in the address space (zero page addressing mode). (FLAGS modified: -).
\"mov [<page0 address>], RA\" : Copies from location in the address space (zero page addressing mode) to RA. (FLAGS modified: -).
\"mov [<page0 address>], RB\" : Copies from location in the address space (zero page addressing mode) to RB. (FLAGS modified: -).
\"mov [<page0 address>], RC\" : Copies from location in the address space (zero page addressing mode) to RC. (FLAGS modified: -).
\"mov [<page0 address>], RD\" : Copies from location in the address space (zero page addressing mode) to RD. (FLAGS modified: -).
\"mov <byte>, [<page0 address>]\" : Copies a constant to a location in the address space (zero page addressing mode). (FLAGS modified: -).
\"mov RA, [RC:RD]\" : Copies RA to a location in the address space (full indirect addressing mode). (FLAGS modified: -).
\"mov RB, [RC:RD]\" : Copies RB to a location in the address space (full indirect addressing mode). (FLAGS modified: -).
\"mov RC, [RC:RD]\" : Copies RC to a location in the address space (full indirect addressing mode). (FLAGS modified: -).
\"mov RD, [RC:RD]\" : Copies RD to a location in the address space (full indirect addressing mode). (FLAGS modified: -).
\"mov [RC:RD], RA\" : Copies from location in the address space (full indirect addressing mode) to RA. (FLAGS modified: -).
\"mov [RC:RD], RB\" : Copies from location in the address space (full indirect addressing mode) to RB. (FLAGS modified: -).
\"mov [RC:RD], RC\" : Copies from location in the address space (full indirect addressing mode) to RC. (FLAGS modified: -).
\"mov [RC:RD], RD\" : Copies from location in the address space (full indirect addressing mode) to RD. (FLAGS modified: -).
\"mov <byte>, [RC:RD]\" : Copies a constant to a location in the address space (full indirect addressing mode). (FLAGS modified: -).
\"mov RA, [<full address>]\" : Copies RA to a location in the address space (absolute addressing mode). (FLAGS modified: -).
\"mov RB, [<full address>]\" : Copies RB to a location in the address space (absolute addressing mode). (FLAGS modified: -).
\"mov RC, [<full address>]\" : Copies RC to a location in the address space (absolute addressing mode). (FLAGS modified: -).
\"mov RD, [<full address>]\" : Copies RD to a location in the address space (absolute addressing mode). (FLAGS modified: -).
\"mov [<full address>], RA\" : Copies from location in the address space (absolute addressing mode) to RA. (FLAGS modified: -).
\"mov [<full address>], RB\" : Copies from location in the address space (absolute addressing mode) to RB. (FLAGS modified: -).
\"mov [<full address>], RC\" : Copies from location in the address space (absolute addressing mode) to RC. (FLAGS modified: -).
\"mov [<full address>], RD\" : Copies from location in the address space (absolute addressing mode) to RD. (FLAGS modified: -).
\"mov <byte>, [<full address>]\" : Copies a constant to a location in the address space (absolute addressing mode). (FLAGS modified: -).
\"mov [<page0 address>], [<page0 address>]\" : Copies a location in the address space (zero page addressing mode) to another location in the address space (zero page addressing mode). (FLAGS modified: -).
\"push RA\" : Pushes RA onto the stack. (FLAGS modified: -).
\"push RB\" : Pushes RB onto the stack. (FLAGS modified: -).
\"push RC\" : Pushes RC onto the stack. (FLAGS modified: -).
\"push RD\" : Pushes RD onto the stack. (FLAGS modified: -).
\"push [<page0 address>]\" : Pushes a location in the address space (zero page addressing mode) onto the stack. (FLAGS modified: -).
\"push [RC:RD]\" : Pushes a location in the address space (full indirect addressing mode) onto the stack. (FLAGS modified: -).
\"push [<full address>]\" : Pushes a location in the address space (absolute addressing mode) onto the stack. (FLAGS modified: -).
\"push <byte>\" : Pushes a constant onto the stack. (FLAGS modified: -).
\"pop RA\" : Pops from the stack into RA. (FLAGS modified: -).
\"pop RB\" : Pops from the stack into RB. (FLAGS modified: -).
\"pop RC\" : Pops from the stack into RC. (FLAGS modified: -).
\"pop RD\" : Pops from the stack into RD. (FLAGS modified: -).
\"pop [<page0 address>]\" : Pops from the stack into a location in the address space (zero page addressing mode). (FLAGS modified: -).
\"pop [RC:RD]\" : Pops from the stack into a location in the address space (full indirect addressing mode). (FLAGS modified: -).
\"pop [<full address>]\" : Pops from the stack into a location in the address space (absolute addressing mode). (FLAGS modified: -).
\"add RA, RA\" : Adds RA to RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RA, RB\" : Adds RB to RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RA, RC\" : Adds RC to RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RA, RD\" : Adds RD to RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RB, RA\" : Adds RA to RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RB, RB\" : Adds RB to RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RB, RC\" : Adds RC to RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RB, RD\" : Adds RD to RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RC, RA\" : Adds RA to RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RC, RB\" : Adds RB to RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RC, RC\" : Adds RC to RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RC, RD\" : Adds RD to RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RD, RA\" : Adds RA to RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RD, RB\" : Adds RB to RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RD, RC\" : Adds RC to RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RD, RD\" : Adds RD to RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RA, <byte>\" : Adds a constant to RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RB, <byte>\" : Adds a constant to RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RC, <byte>\" : Adds a constant to RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add RD, <byte>\" : Adds a constant to RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add [<page0 address>], RA\" : Adds RA to a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add [<page0 address>], RB\" : Adds RB to a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add [<page0 address>], RC\" : Adds RC to a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"add [<page0 address>], RD\" : Adds RD to a location in the address space (zero page addressing mode), stores the result back in the address space (zero page addressing mode), then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RA, RA\" : Subtracts RA from RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RA, RB\" : Subtracts RB from RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RA, RC\" : Subtracts RC from RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RA, RD\" : Subtracts RD from RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RB, RA\" : Subtracts RA from RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RB, RB\" : Subtracts RB from RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RB, RC\" : Subtracts RC from RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RB, RD\" : Subtracts RD from RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RC, RA\" : Subtracts RA from RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RC, RB\" : Subtracts RB from RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RC, RC\" : Subtracts RC from RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RC, RD\" : Subtracts RD from RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RD, RA\" : Subtracts RA from RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RD, RB\" : Subtracts RB from RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RD, RC\" : Subtracts RC from RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RD, RD\" : Subtracts RD from RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RA, <byte>\" : Subtracts a constant from RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RB, <byte>\" : Subtracts a constant from RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RC, <byte>\" : Subtracts a constant from RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub RD, <byte>\" : Subtracts a constant from RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub [<page0 address>], RA\" : Subtracts RA from a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub [<page0 address>], RB\" : Subtracts RD from a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub [<page0 address>], RC\" : Subtracts RC from a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"sub [<page0 address>], RD\" : Subtracts RD from a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RA, RA\" : Compares RA to RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RA, RB\" : Compares RA to RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RA, RC\" : Compares RA to RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RA, RD\" : Compares RA to RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RB, RA\" : Compares RB to RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RB, RB\" : Compares RB to RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RB, RC\" : Compares RB to RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RB, RD\" : Compares RB to RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RC, RA\" : Compares RC to RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RC, RB\" : Compares RC to RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RC, RC\" : Compares RC to RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RC, RD\" : Compares RC to RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RD, RA\" : Compares RD to RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RD, RB\" : Compares RD to RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RD, RC\" : Compares RD to RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RD, RD\" : Compares RD to RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RA, <byte>\" : Compares RA to a constant, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RB, <byte>\" : Compares RB to a constant, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RC, <byte>\" : Compares RC to a constant, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp RD, <byte>\" : Compares RD to a constant, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp [<page0 address>], RA\" : Compares the given location in the address space (zero page addressing mode) to RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp [<page0 address>], RB\" : Compares the given location in the address space (zero page addressing mode) to RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp [<page0 address>], RC\" : Compares the given location in the address space (zero page addressing mode) to RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"cmp [<page0 address>], RD\" : Compares the given location in the address space (zero page addressing mode) to RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"or RA, RA\" : Performs a bitwise OR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RA, RB\" : Performs a bitwise OR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RA, RC\" : Performs a bitwise OR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RA, RD\" : Performs a bitwise OR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RB, RA\" : Performs a bitwise OR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RB, RB\" : Performs a bitwise OR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RB, RC\" : Performs a bitwise OR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RB, RD\" : Performs a bitwise OR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RC, RA\" : Performs a bitwise OR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RC, RB\" : Performs a bitwise OR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RC, RC\" : Performs a bitwise OR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RC, RD\" : Performs a bitwise OR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RD, RA\" : Performs a bitwise OR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RD, RB\" : Performs a bitwise OR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RD, RC\" : Performs a bitwise OR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RD, RD\" : Performs a bitwise OR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RA, <byte>\" : Performs a bitwise OR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RB, <byte>\" : Performs a bitwise OR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RC, <byte>\" : Performs a bitwise OR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or RD, <byte>\" : Performs a bitwise OR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: -).
\"or [<page0 address>], RA\" : Performs a bitwise OR of a location in the address space (zero page addressing mode) and RA, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"or [<page0 address>], RB\" : Performs a bitwise OR of a location in the address space (zero page addressing mode) and RB, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"or [<page0 address>], RC\" : Performs a bitwise OR of a location in the address space (zero page addressing mode) and RC, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"or [<page0 address>], RD\" : Performs a bitwise OR of a location in the address space (zero page addressing mode) and RD, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RA, RA\" : Performs a bitwise AND of RA and RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RA, RB\" : Performs a bitwise AND of RA and RB, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RA, RC\" : Performs a bitwise AND of RA and RC, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RA, RD\" : Performs a bitwise AND of RA and RD, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RB, RA\" : Performs a bitwise AND of RB and RA, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RB, RB\" : Performs a bitwise AND of RB and RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RB, RC\" : Performs a bitwise AND of RB and RC, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RB, RD\" : Performs a bitwise AND of RB and RD, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RC, RA\" : Performs a bitwise AND of RC and RA, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RC, RB\" : Performs a bitwise AND of RC and RB, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RC, RC\" : Performs a bitwise AND of RC and RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RC, RD\" : Performs a bitwise AND of RC and RD, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RD, RA\" : Performs a bitwise AND of RD and RA, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RD, RB\" : Performs a bitwise AND of RD and RB, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RD, RC\" : Performs a bitwise AND of RD and RC, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RD, RD\" : Performs a bitwise AND of RD and RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RA, <byte>\" : Performs a bitwise AND of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RB, <byte>\" : Performs a bitwise AND of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RC, <byte>\" : Performs a bitwise AND of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and RD, <byte>\" : Performs a bitwise AND of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and [<page0 address>], RA\" : Performs a bitwise AND of a location in the address space (zero page addressing mode) and RA, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and [<page0 address>], RB\" : Performs a bitwise AND of a location in the address space (zero page addressing mode) and RB, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and [<page0 address>], RC\" : Performs a bitwise AND of a location in the address space (zero page addressing mode) and RC, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"and [<page0 address>], RD\" : Performs a bitwise AND of a location in the address space (zero page addressing mode) and RD, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RA, RA\" : Performs a bitwise XOR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RA, RB\" : Performs a bitwise XOR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RA, RC\" : Performs a bitwise XOR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RA, RD\" : Performs a bitwise XOR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RB, RA\" : Performs a bitwise XOR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RB, RB\" : Performs a bitwise XOR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RB, RC\" : Performs a bitwise XOR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RB, RD\" : Performs a bitwise XOR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RC, RA\" : Performs a bitwise XOR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RC, RB\" : Performs a bitwise XOR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RC, RC\" : Performs a bitwise XOR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RC, RD\" : Performs a bitwise XOR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RD, RA\" : Performs a bitwise XOR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RD, RB\" : Performs a bitwise XOR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RD, RC\" : Performs a bitwise XOR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RD, RD\" : Performs a bitwise XOR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RA, <byte>\" : Performs a bitwise XOR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RB, <byte>\" : Performs a bitwise XOR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RC, <byte>\" : Performs a bitwise XOR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor RD, <byte>\" : Performs a bitwise XOR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor [<page0 address>], RA\" : Performs a bitwise XOR of a location in the address space (zero page addressing mode) and RA, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor [<page0 address>], RB\" : Performs a bitwise XOR of a location in the address space (zero page addressing mode) and RB, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor [<page0 address>], RC\" : Performs a bitwise XOR of a location in the address space (zero page addressing mode) and RC, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"xor [<page0 address>], RD\" : Performs a bitwise XOR of a location in the address space (zero page addressing mode) and RD, stores the result back in the given location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"not RA\" : Performs a bitwise NOT of RA, stores the result back in RA, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"not RB\" : Performs a bitwise NOT of RB, stores the result back in RB, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"not RC\" : Performs a bitwise NOT of RC, stores the result back in RC, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"not RD\" : Performs a bitwise NOT of RD, stores the result back in RD, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"not [<page0 address>]\" : Performs a bitwise NOT of a location in the address space (page 0 addressing mode), stores the result back in the location, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"inc RA\" : Increments RA by 1, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"inc RB\" : Increments RB by 1, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"inc RC\" : Increments RC by 1, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"inc RD\" : Increments RD by 1, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"inc [<page0 address>]\" : Increments a location in the address space (zero page addressing mode) by 1, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"dec RA\" : Decrements RA by 1, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"dec RB\" : Decrements RB by 1, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"dec RC\" : Decrements RC by 1, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"dec RD\" : Decrements RD by 1, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"dec [<page0 address>]\" : Decrements a location in the address space (zero page addressing mode) by 1, then updates the FLAGS accordingly. (FLAGS modified: NZC).
\"shl RA\" : Performs a left bitwise shift of RA by 1 bit. (FLAGS modified: -).
\"shr RA\" : Performs a right bitwise shift of RA by 1 bit. (FLAGS modified: -).
\"swap RA, RB\" : Swaps the values of RA and RB. (FLAGS modified: -).
\"swap RA, RC\" : Swaps the values of RA and RC. (FLAGS modified: -).
\"swap RA, RD\" : Swaps the values of RA and RD. (FLAGS modified: -).
\"swap RB, RC\" : Swaps the values of RB and RC. (FLAGS modified: -).
\"swap RB, RD\" : Swaps the values of RB and RD. (FLAGS modified: -).
\"swap RC, RD\" : Swaps the values of RC and RD. (FLAGS modified: -).
\"jc <byte>\" : Jumps to the given short address (same code segment) if the carry flag is set. (FLAGS modified: -).
\"jn <byte>\" : Jumps to the given short address (same code segment) if the negative flag is set. (FLAGS modified: -).
\"jz <byte>\" : Jumps to the given short address (same code segment) if the zero flag is set. (FLAGS modified: -).
\"goto <byte>\" : Jumps to the given short address (same code segment). (FLAGS modified: -).
\"ljc <dcst>\" : Jumps to the given long address if the carry flag is set. (FLAGS modified: -).
\"ljn <dcst>\" : Jumps to the given long address if the negative flag is set. (FLAGS modified: -).
\"ljz <dcst>\" : Jumps to the given long address if the zero flag is set. (FLAGS modified: -).
\"lgoto <dcst>\" : Jumps to the given long address. (FLAGS modified: -).
\"lgoto RC:RD\" : Jumps to the given long address (full indirect addressing mode). (FLAGS modified: -).
\"lcall <dcst>\" : Calls the given long address. (FLAGS modified: -).
\"lcall [RC:RD]\" : Calls the given long address (full indirect addressing mode). (FLAGS modified: -).
\"ret \" : Returns from a call. (FLAGS modified: -).
\"iret \" : Returns from an interrupt. (FLAGS modified: -).
\"cid \" : Clears the interrupt disable flag. (FLAGS modified: I).
\"sid \" : Sets the interrupt disable flag. (FLAGS modified: I).
\"clc \" : Clears the carry flag. (FLAGS modified: C).
\"sec \" : Sets the carry flag. (FLAGS modified: C)."""

generation_config = {
    "max_output_tokens": 8192,
    "temperature": 1,
    "top_p": 0.95,
}

safety_settings = {
    generative_models.HarmCategory.HARM_CATEGORY_HATE_SPEECH: generative_models.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
    generative_models.HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: generative_models.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
    generative_models.HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: generative_models.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
    generative_models.HarmCategory.HARM_CATEGORY_HARASSMENT: generative_models.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
}

generate()

