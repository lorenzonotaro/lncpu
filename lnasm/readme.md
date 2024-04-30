# lnasm

LNASM is an assembler for the `lncpu`.


## Building

Use [`maven`](https://maven.apache.org/) to assemble a runnable JAR with dependencies:

    mvn package

# Usage

To run `lnasm`, use:

    java -jar lnasm.jar <source file(s)> [options...]

Run with `--help` to show a list of available options.

# lnasm language

LNASM file have the extension `.lnasm`.
Each lnasm file must have with a `.org` directive before any code is added.


### Directives

- `.org <address>`

    Sets the start address for the instructions that follow it.
        
        .org 0x0100             ; set the origin to the second page of ROM

            mov 0x42,   RA      ; this instruction will be at address 0x0

        
    **Note**: at least one `.org` directive is required in a program.
- `.data <values...>`

    Encodes the provided values into the program. Values can be:
        
    - numbers (up to 16-bit values), encoded in big endian order.
    - strings, ASCII encoded but NOT automatically null-terminated.

- `.pad <n>`

    Adds `n` zeroes to the compiled binary in the current location.

### Preprocessor macros

- `%define <identifier> <value>`

    Defines a macro with the given identifier and value, such that the identifier will be replaced with the given value.

- `%undefine <identifier>`

    Undefines a previously defined macro.

- `%include "<filename>"`

    Parses and copies the content of the given file into the current file, at the directive's location.

    **Warning**: there is no check for circular dependencies. Be careful.

### Labels and sublabels

Every instruction can be preceded by a label:

    LABELNAME:
        inc         RA
        goto        LABELNAME

Labels starting with `_` are interpreted as sublabels (if preceded by a label that does NOT start with `_`). They can only be referenced by code in the same top-level label.

    A:
        mov         [0x01],     RC
    _sub15:
        sub         RC,         15

    B:
        goto        _sub15          ; <---- ERROR!             

Currently, labels can only be used in jump instructions.

### Comments

Comments can be initiated with `;`: the remainder of the line after the semicolon will be ignored by the parser.


### Instruction list

For a list of available instructions, refer to [this .tsv file](src/main/resources/opcodes.tsv). This file is a resource file for the compiler and it is automatically generated when generating the control unit EEPROMS using the script in [v1/controlunit](/v1/controlunit/gen_eeproms.py).

The second column of this TSV file contains the _immediate instruction code_ which is, although not directly usable in lnasm, easily translatable to valid lnasm code.

(Since for the time being the implementation is constantly changing, this is the only form of "documentation" available until the project is finished.)

Basically, each immediate instruction code is made of (separated by `_`):

1. The instruction identifier, that can be copied as is in lnasm.

2. Between 0 and 2 instruction parameters. These can be:
    - registers: `ra`, `rb`, `rc`, etc...
        
        In LNASM, register names are always uppercase (e.g. `ra` becomes `RA`)
    - constant values: the immediate instruction will contain `cst`. In lnasm this can be any 8-bit value in hex (prefixed with `0x`), binary (`0b`) or decimal form (no prefix), or an ASCII character (enclosed by `'`).

    - addressing modes. These indicate a source/target in the addressing space. In lnasm the addressing space is referenced by using square brackets (`[location]`). The immediate instruction will contain the addressing mode:
    
        - `page0`: zero page addressing mode. The target address will be in the first page of RAM (`0x2000-0x20ff`). In lnasm you will specify an 8-bit value in the square brackets, corresponding to the address in page zero you want to write to/read from (`[8-bit address]`)

        - `ifullrcrd`: full indirect addressing mode. In lnasm this is achieved with `[RC:RD]`.

        - `abs`: absolute addressing mode. In lnasm this is achieved with `[<8-bit page>:<8-bit address>]` or `[<16-bit full address>]`.

    Jump instructions do not have a parameter in their immediate instruction code, but require one:
        
    - *short jump instructions* require an 8-bit constant OR a label.

    - *long jump instructions* require a 16-bit constant address or a label.

    The compiler will issue a warning if a distant label (not in the same code segment) is used within a short jump or if a local label (in the same code segment) is used withing a long jump.

Some examples to clarify:

    START:
        mov        RA,        RB      ; mov_ra_rb: moves the contents of RA into RB

        mov        [0x42],    RA      ; mov_page0_ra: moves the contents of address 0x2042 into RA

        mov        [RD],      RA      ; mov_ipage0rd_ra: moves the contents of address 0x20:RD into RA

        mov        RC,        [RC:RD] ; mov_rc_ifullrcrd: moves the contents of RC to address RC:RD

        mov        0x24f1,    [RD]    ; mov_abs_rd: moves the content of address 0x24f1 to RD
