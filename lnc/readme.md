# lnc

LNC is a pseudo-C (see below) compiler and assembler for the `lncpu`.


## Building

Use [`maven`](https://maven.apache.org/) to assemble a runnable JAR with dependencies:

    mvn package

As an alternative, use `make.sh` in the repository root folder to make and install everything in an `/output` folder, which you may then add to your `PATH`.

# Usage

## Command line syntax

To run `lnc`, use:

    java -jar lnc.jar <source file(s)> [options...]

Run with `--help` to show a list of available options.


## The `lnasm` language

`lnasm` is a simple assembly language for the `lncpu`. It is a line-based language, with each line representing a single instruction or directive.

Leading and trailing whitespace in each line is ignored, but each line may contain only one instruction or directive. Instructions and directives are case-insensitive.

`lnasm` code is organized in sections. Each section is a block of code that can be placed in a specific region in the address space. The rules for section placement are defined in a linker configuration script (see [below](#linker-configuration)).

### Order of operands
In `lnasm`, the order of operands is important.

For instructions that copy data, the source operand comes before the destination operand, as in:

    mov 0x42, RA ; move the value 0x42 into the RA register

For instructions that perform arithmetic or logical operations, the operand in which to store the result comes first, followed by the operands to operate on, as in:

    add RA, RB ; add the value in RB to RA and store the result in RA


### Directives

- `.section <section name>`

    Sets the current section. All subsequent instructions will be added to this section, until a new `.section` directive is encountered.
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

`lnasm` provides a very basic preprocessor, with the following directives:

- `%define <identifier> [<value>]`

    Defines a macro with the given identifier and value, such that the identifier will be replaced with the given value.

- `%undefine <identifier>`

    Undefines a previously defined macro.

- `%ifdef <identifier> ... %endif`

    If the given identifier is defined, the code following this directive will be included in the output. Otherwise, it will be ignored.

- `%ifndef <identifier> ... %endif`

    If the given identifier is not defined, the code following this directive will be included in the output. Otherwise, it will be ignored.

- `%ifdef SECTION <section> ... %endif`

    If a section with the given name exists, the code following this directive will be included in the output. Otherwise, it will be ignored.

- `%ifndef SECTION <section> ... %endif`

    If a section with the given name does not exist, the code following this directive will be included in the output. Otherwise, it will be ignored.

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

When used, labels are evaluated as:

* `dcst` (16-bit words) if they point to a non-data page section.
* `cst` (byte) if they point to a data page section.

*Note*: section names can be referenced just like labels. They always evaluate to the start of the section as specified in the linker configuration (`dcst`), even if the section is a data page section.

### Expressions

Lnasm supports basic arithmetic and logic binary operators. In order of decreasing precedence:

1. multipication (`*`) and division (`/`)
2. addition (`+`) and subtraction (`-`)
3. left (`<<`) and right (`>>`) bitwise shift
4. bitwise logic OR (`|`), AND (`&`) and XOR (`^`)

Parethesis can be used to clarify expressions.

Valid operands for binary operator are:

* 8-bit (`cst`) and 16-bit (`dcst`) values.
* labels

**Note**: expressions preserve the type (byte or word) of the left operand. You may need to [cast](#Casts) to the desired type.

### Casts

The cast operator `::` allows you to trucate 16-bit words to 8-bit values and to extend 8-bit values to 16-bit words. 

The cast operator has the highest precedence, second only to primary expressions (identifiers, constant values, string literals).

Examples:

    ; 0xFF::16, 0xFF::word evaluate to 0x00FF
    ; 0x1234::8, 0x1234::byte evaluate to 0x34

    ; You can combine binary operators and casts.
    ; The following example loads RC:RD with the label SUBROUTINE and then calls it

    ...

    mov     (SUBROUTINE >> 8)::byte, RC
    mov     (SUBROUTINE & 0xFF)::byte, RD ; The & operator is not really necessary since the cast operator truncates the 8 MSB
    lcall   RC:RD

    ...

    .section OTHER_CODE
    SUBROUTINE:
        ...
        ret

### Comments

Comments can be initiated with `;`: the remainder of the line after the semicolon will be ignored by the parser.


### Instruction set

Consult the [instruction set reference](instructionset.md) for the available instructions.

### Linker configuration
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
      <section name>: property1 = value1, property2 = value2, ...;
      ...
    ]

Each section name must be unique, may contain letters, numbers and underscores and cannot start with a digit.

Use the following properties to define a section:

* `mode`: the placement mode of the section. Possible values are:
  * `fixed`: the section is placed at a fixed address. You must specify the start address using the `start` property.
  * `page_align`: the section is placed at the first available address that is page-aligned (`xx00`). You must specify the target device using the `target` property.
  * `page_fit`: the section is placed at the first available address that fits its size and so that the section does not cross a page boundary. You must specify the target device using the `target` property.
  * `fit`: the section is placed at the first available address that fits its size, regardless of page boundaries. You must specify the target device using the `target` property.
* `target`: the target device for the section. This property is mandatory for `page_align`, `page_fit` and `fit` sections. One of `ROM`, `RAM`, `D1` through `D6`.
* `datapage`: if present, indicates that the section represents a data page. All labels in this section will evaluate to a `cst` (byte) value.
* `virtual`: only applicable to a `datapage`. If present, the section will not be placed in the address space. It may be used as a dynamic data page, whose address is determined at runtime via the `DS` register.
* `multi`: if present, the section can be referenced multiple times. Each 'block' will be appended to the final binary. This is useful for data pages, as each module can reserve its own space in the data page.

Example:

    SECTIONS[
      STARTUP_CODE: mode = fixed, start = 0x0000;
      PROGRAM: mode = page_fit, target = ROM;
      DATA_PAGE: mode = page_align, target = RAM, datapage;
    ]

## The lnc language

The lnc is a C-like language that compiles to lnasm.
The compiler is still very rudimental and produces *very* unoptimal code.

lnc is (and probably will never be) neither C-standard compliant nor a simple subset of C: some features of the lnc architecture (such as the existence of different memory addressing modes, data page and absolute) require some extra features that C doesn't provide on its own.

(This is the main reason why I didn't simply build a backend for an existing compiler such as `clang/llvm`; it would have been very challenging if not outright impossible.)

### Features

 - [x] Basic arithmetic expressions (currently add/sub)
 - [x] Basic flow control (for, while, if/else)
 - [ ] Other forms of flow control (switch)
 - [x] Continue and break statements 
 - [x] Data page variables and pointers
 - [ ] Absolute variables and pointers
 - [x] Data page arrays
 - [ ] Absolute arrays
 - [x] Structs (data page only)
 - [ ] Unions
 - [x] Reentrant functions


### Calling convention

#### Parameters

The calling convention for functions in `lnc` allows for up to 4 1-byte paramters and 2 2-byte parameters to be passed in the general purpose registers `RA`, `RB`, `RC` and `RD`. If more than 4 parameters are needed, the remaining parameters must be passed via the stack. Specifically:
- if the callee doesn't require any 2-byte parameters, the first 4 parameters are passed in `RA`, `RB`, `RC` and `RD`; the remaining parameters are passed on the stack.
- if the callee requires a 2-byte parameter, such parameter is passed in `RC:RD`; `RA` and `RB` are used for the first two 1-byte parameters, while the remaining parameters are passed on the stack.

In case any parameters are passed on the stack, the *callee* is responsible for clearing the stack (including its parameters) before returning. This can be easily done by using the `ret <stack size>` instruction, specifying the number of bytes to pop from the stack after the function returns.

#### Return values
If the function returns a 1-byte value, it is returned in the `RB` register. If the function returns a 2-byte value, it is returned in the `RC:RD` registers.

#### Register preservation
The calling convention requires that the *callee* preserves the values of any registers it uses, except for the register(s) used to return a value (`RB` or `RC:RD`, see above).

#### Interfacing with lnasm

To inform the C compiler of the existence of an `lnasm` function that will be linked with the compiler output, you may declare it as `extern`.
In the lnasm code, the function must follow lnc's calling convention and register preservation.
Example:
```C
    //lnc:
    extern int doublei(int i);
```
The function code in lnasm could then be:
```
    doublei:
        mov     RA,     RB ; move RA to the return register
        add     RB,     RB ; double the value in RB
        ret
```
The same goes the other way around:
```c
    int doublei(int i){
        return i + i;
    }
```
```
    mov     15,             RA
    lcall   doublei
    ; at this point RB will contain 30
```