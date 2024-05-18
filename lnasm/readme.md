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


## The lnasm language

Lnasm code is a simple assembly language for the `lncpu`. It is a line-based language, with each line representing a single instruction or directive.

Leading and trailing whitespace in each line is ignored, but each line may contain only one instruction or directive. Instructions and directives are case-insensitive.

Lnasm code is organized in sections. Each section is a block of code that can be placed in a specific region in the address space. The rules for section placement are defined in a linker configuration script (see [below](#linker-configuration)).

### Directives

- `.section <section name>`

    Sets the current section. All subsequent instructions will be **appended** to this section, until a new `.section` directive is encountered.
    The section name must match a section defined in the linker configuration script (case-sensitive).
        
        .section CODE           

            mov 0x42,   RA      ; this instruction will be placed in the CODE section

- `.res <n>`

    Adds `n` bytes (zeroes) at the current location.

- `.data <value>[, <value>, ...]`

    Adds the given values at the current location, in the order they are given. Values can be:
  - integers (e.g. `0x42`, `42`, `0b1010`)
  - strings, ASCII-encoded and *not* null-terminated (e.g. `"Hello, world!"`)
  - characters, ASCII-encoded (e.g. `'A'`)

### Preprocessor directives

Lnasm supports a very basic preprocessor, with the following directives:

- `%define <identifier> [<value>]`

    Defines a macro with the given identifier and value, such that the identifier will be replaced with the given value.

- `%undefine <identifier>`

    Undefines a previously defined macro.

- `%ifdef <identifier> ... %endif`

    If the given identifier is defined, the code following this directive will be included in the output. Otherwise, it will be ignored.
- `%ifndef <identifier> ... %endif`

    If the given identifier is not defined, the code following this directive will be included in the output. Otherwise, it will be ignored.

- `%include "<filename>"`

    Parses and copies the content of the given file into the current file, at the directive's location.

    **Warning**: there is no check for circular dependencies or re-includes. Be careful. The safest option is to use a C-style include guard:

      %ifndef _MY_FILE_INCLUDED
      %define _MY_FILE_INCLUDED

        ; your code here

      %endif

### Labels and sublabels

Every instruction or directive (except `.section`) can be preceded by a label:

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

## Linker configuration
In order to successfully assemble a program, you must provide a **linker configuration script**.
A linker configuration script tells the linker information about each code section,
such as what device it belongs to, how to place it in the address space (see below), etc...

You can provide a linker configuration file via the command line:

    java -jar lnasm.jar <source file(s) -lf <linker cfg file>


Or you can provide the configuration script in the command line:

    java -jar lnasm.jar <source file(s) -lc "<configuration script>"

If no options are provided, lnasm looks for a file called `linker.cfg` in the current working directory.

The format of a configuration script is as follows:

    SECTIONS[
      <section name>: type = <section type>[, <property> = <value>, ...] ;
      <section name>: type = <section type>[, <property> = <value>, ...] ;
    ]

Each section name must be unique, may contain letters, numbers and underscores and cannot start with a digit.

For each section you must specify its properties. Section properties include:

* `type`. This is **mandatory**, as each section must have a type specified. Possible values:
  * `ROM`: the section will be placed in ROM and will be included into the binary output.
  * `RAM`: the section will not be included into the binary output.
  * `PAGE0`: the section represents the first page of RAM, and so will not be linked . `PAGE0` implies that the start address of the section is `0x2000` and the mode is `fixed`: these properties can therefore be omitted.
* `mode` (default: `fixed`). Possible values:
  * `fixed`: the section will be placed at a fixed address, specified by the `start` properties (required).
  * `page_align`: the section will be placed at the first available page-aligned address.
  * `page_fit`: the section will be placed at the first available address that ensures that the section fits in a single page. *Note*: a `page_fit` section cannot be bigger than 256 bytes.
  * `fit`: the section will be placed wherever it fits, regardless of page boundaries.
* `start` (required if mode is `fixed` or unspecified). The start address of the section.
* `multi` (default `false`). If `true`, the section can be referenced multiple times in the code via the `.section` directive: each block will be appended to the section.

**Note**: this is useful for modular programs and the page 0 section: each module can reserve its own space in the page 0 section.

Example:

    SECTIONS[
      CODE: type = ROM, mode = fixed, start = 0x0000;
      DATA: type = RAM, mode = fit;
      PAGE0: type = PAGE0;
      INTVEC: type = ROM, mode = fixed, start = 0x1f00;
    ]