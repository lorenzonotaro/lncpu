# lnasm

LNASM is an assembler for the `lncpu`.


## Building

Use [`maven`](https://maven.apache.org/) to assemble a runnable JAR with dependencies:

    mvn package

# Usage

## Command line syntax

To run `lnasm`, use:

    java -jar lnasm.jar <source file(s)> [options...]

Run with `--help` to show a list of available options.

## The basics
LNASM file have the extension `.lnasm`.

lnasm source code i

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


### Instruction set

Consult the [instruction set reference](instructionset.md) for the available instructions.