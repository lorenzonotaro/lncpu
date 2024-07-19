import base64
import vertexai
import os
import time
from vertexai.generative_models import GenerativeModel, Part, FinishReason
import vertexai.preview.generative_models as generative_models

def generate(immediate_name, lnasm, description, flags, code):
    if flags.strip() == "-":
        flags = ""
    else:
        flags = f"Flags modified: {flags}"

    vertexai.init(project="starry-embassy-429412-c4", location="us-central1")
    model = GenerativeModel(
            "gemini-1.5-pro-001",
        )
    responses = model.generate_content(
      [f"""You will be provided with an AI-generated test code for an instruction of a custom assembly language for an 8-bit CPU.
The first line is a comment specifying the instruction being tested and what it does.
The code may or may not work as is. Your goal is to revise it keeping in mind  the following considerations You may send back the same code you are provided if you believe it works..

The code must be followed by a hlt instruction and then by a series of comments (initiated by ;) describing the conditions for which the test is passed.
Do not add registers/memory locations that aren\'t touched in the pass conditions.
The test conditions must be in the following format, either:
- the register name, followed by = and the expected value at the end of execution (e.g. RA = 0x42)
- a memory address (either page 0 addressing mode or absolute addressing mode), enclosed in square brackets followed by = and the expected value at the end of execution (e.g. [0x00] = 0x42, [0x2100] = 0x42)

Do not use markdown.
Do not add unnecessary comments.
Always end with the pass conditions.

The code must be preceded by the section directive:

.section CODE will place the following code at the beginning of ROM to be executed.
.section DATA will place the following code somewhere in ROM to store data. You can use a label (<LABEL NAME>:) along with a .data <value> directive to then reference the address in the code.

You must have a CODE section, you may have a DATA section if you need to read data from ROM. This section is in ROM so will be READ ONLY.

The language uses AT&T syntax (mov SRC, DEST), although constants are not preceded by $. Addresses are enclosed in [].
Constant values can be expressed in hexadecimal (0x) or binary (0b, useful for FLAGS pass conditions).
You may use other instructions to setup your test.

RA, RB, RC, RD, SS, SP are all 8-bit registers.

When adding FLAGS to the pass conditions, add it as a single entry (FLAGS = <value>, not N = x, Z = x, etc.). FLAGS is a 4-bit register whose bits are, most to least significant: I (interrupt disable), N (negative), Z (zero), C (carry).

Address space layout:

0x0000-0x1fff: ROM. Read-only for code/data.
0x2000-0x3fff: RAM. Page 0 is in the first page of RAM (0x00-0xff -> 0x2000-0x20ff).

Addressing modes:
- zero page: [<byte>] -> translates to [0x2100 + <byte]
- full indirect: [RC:RD] -> translates to [RC << 8 + RD]
- absolute (full address) -> [<word>]

If you use instructions that require the stack (push, pop, call, ret), you must set it up by setting SS, usually to 0x21, and SP to 0. In this manner, the pushed value will be located at 0x2100.

Previously generated code:
; {lnasm} - {description} - {flags}
{code}
	
Revised code:
"""],
      generation_config=generation_config,
      safety_settings=safety_settings,
  )

    return responses


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
    return opcode + " " + ", ".join([convert_immediate_param(e) for e in splitted[1:]])    

def save_and_process_generated(immediate_name, generated):
    # code is everything in generated.text
    # pass conditions are every line beginning with ; from from the end

    code = generated.text

    pass_conditions = []

    for line in code.split("\n")[::-1]:
        if line.strip().startswith(";"):
            pass_conditions.append(line.strip()[1:].strip())
        elif len(line.strip()) != 0:
            break

    with open(f"{immediate_name}/test.lnasm", "w") as test_lnasm:
        test_lnasm.write(code)
    
    with open(f"{immediate_name}/pass.txt", "w") as test_pass:
        test_pass.write("\n".join(pass_conditions))

generation_config = {
    "max_output_tokens": 8192,
    "temperature": 2,
    "top_p": 0.95,
}

safety_settings = {
    generative_models.HarmCategory.HARM_CATEGORY_HATE_SPEECH: generative_models.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
    generative_models.HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: generative_models.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
    generative_models.HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: generative_models.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
    generative_models.HarmCategory.HARM_CATEGORY_HARASSMENT: generative_models.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
}

# if there is not file called revised.txt, create it
if not os.path.isfile("revised.txt"):
  with open("revised.txt", "w") as revised:
    revised.write("")

with open("revised.txt", "r") as revised_txt:
    already_revised = revised_txt.read().split("\n")

with open("../lnasm/src/main/resources/opcodes.tsv", "r") as opcodes_tsv:
  opcodes = opcodes_tsv.readlines()
  for opcode in opcodes[4:]:
    opcode = opcode.strip().split("\t")

    if opcode[1] in already_revised:
        print(f"{opcode[1]} already revised.")
        continue

    with open(f"{opcode[1]}/test.lnasm", "r") as test_lnasm:
        code = test_lnasm.read()


    try:
        generated = generate(opcode[1], immediate_to_lnasm(opcode[1]), opcode[4], opcode[5], code)
    except vertexai.errors.VertexAIError as e:
        print(f"Error generating {opcode[1]}: {e}")
        continue

    try:
        save_and_process_generated(opcode[1], generated)
    except Exception as e:
        print(f"Error saving and processing generated {opcode[1]}: {e}")
        continue

    with open("revised.txt", "a") as revised_txt:
        revised_txt.write(opcode[1] + "\n")
    
    print(f"{opcode[1]} revised.")

    time.sleep(12) # sleep to match quota of 5 requests per minute


