

### lnasm instruction set


| Opcode | Syntax | Description | Clock cycles | Flags Affected |
|--------|--------|-------------|--------------|----------------|
| 00 | `nop ` | No operation | 1 | - |
| 01 | `hlt ` | Halts the CPU | 1 | - |
| 02 | `brk ` | Pushes CS:PC and FLAGS, then calls the interrupt vector | 7 | `I` |
| 03 | `mov <byte>,	RA` | Moves a constant to RA | 2 | - |
| 04 | `mov <byte>,	RB` | Moves a constant to RB | 2 | - |
| 05 | `mov <byte>,	RC` | Moves a constant to RC | 2 | - |
| 06 | `mov <byte>,	RD` | Moves a constant to RD | 2 | - |
| 07 | `mov <byte>,	SP` | Moves a constant to SP | 2 | - |
| 08 | `mov <byte>,	SS` | Moves a constant to SS | 2 | - |
| 09 | `mov SS,	RD` | Moves SS to RD | 1 | - |
| 0a | `mov SP,	RD` | Moves SP to RD | 1 | - |
| 0b | `mov RA,	RB` | Moves RA to RB | 1 | - |
| 0c | `mov RA,	RC` | Moves RA to RC | 1 | - |
| 0d | `mov RA,	RD` | Moves RA to RD | 1 | - |
| 0e | `mov RB,	RA` | Moves RB to RA | 1 | - |
| 0f | `mov RB,	RC` | Moves RB to RC | 1 | - |
| 10 | `mov RB,	RD` | Moves RB to RD | 1 | - |
| 11 | `mov RC,	RA` | Moves RC to RA | 1 | - |
| 12 | `mov RC,	RB` | Moves RC to RB | 1 | - |
| 13 | `mov RC,	RD` | Moves RC to RD | 1 | - |
| 14 | `mov RD,	RA` | Moves RD to RA | 1 | - |
| 15 | `mov RD,	RB` | Moves RD to RB | 1 | - |
| 16 | `mov RD,	RC` | Moves RD to RC | 1 | - |
| 17 | `mov RD,	SS` | Moves RD to SS | 1 | - |
| 18 | `mov RD,	SP` | Moves RD to SP | 1 | - |
| 19 | `mov RA,	[<page0 address>]` | Copies RA to a location in the address space (zero page addressing mode) | 2 | - |
| 1a | `mov RB,	[<page0 address>]` | Copies RB to a location in the address space (zero page addressing mode) | 2 | - |
| 1b | `mov RC,	[<page0 address>]` | Copies RC to a location in the address space (zero page addressing mode) | 2 | - |
| 1c | `mov RD,	[<page0 address>]` | Copies RD to a location in the address space (zero page addressing mode) | 2 | - |
| 1d | `mov [<page0 address>],	RA` | Copies from location in the address space (zero page addressing mode) to RA | 2 | - |
| 1e | `mov [<page0 address>],	RB` | Copies from location in the address space (zero page addressing mode) to RB | 2 | - |
| 1f | `mov [<page0 address>],	RC` | Copies from location in the address space (zero page addressing mode) to RC | 2 | - |
| 20 | `mov [<page0 address>],	RD` | Copies from location in the address space (zero page addressing mode) to RD | 2 | - |
| 21 | `mov <byte>,	[<page0 address>]` | Copies a constant to a location in the address space (zero page addressing mode) | 3 | - |
| 22 | `mov RA,	[RC:RD]` | Copies RA to a location in the address space (full indirect addressing mode) | 3 | - |
| 23 | `mov RB,	[RC:RD]` | Copies RB to a location in the address space (full indirect addressing mode) | 3 | - |
| 24 | `mov RC,	[RC:RD]` | Copies RC to a location in the address space (full indirect addressing mode) | 3 | - |
| 25 | `mov RD,	[RC:RD]` | Copies RD to a location in the address space (full indirect addressing mode) | 3 | - |
| 26 | `mov [RC:RD],	RA` | Copies from location in the address space (full indirect addressing mode) to RA | 3 | - |
| 27 | `mov [RC:RD],	RB` | Copies from location in the address space (full indirect addressing mode) to RB | 3 | - |
| 28 | `mov [RC:RD],	RC` | Copies from location in the address space (full indirect addressing mode) to RC | 3 | - |
| 29 | `mov [RC:RD],	RD` | Copies from location in the address space (full indirect addressing mode) to RD | 3 | - |
| 2a | `mov <byte>,	[RC:RD]` | Copies a constant to a location in the address space (full indirect addressing mode) | 4 | - |
| 2b | `mov RA,	[<full address>]` | Copies RA to a location in the address space (absolute addressing mode) | 3 | - |
| 2c | `mov RB,	[<full address>]` | Copies RB to a location in the address space (absolute addressing mode) | 3 | - |
| 2d | `mov RC,	[<full address>]` | Copies RC to a location in the address space (absolute addressing mode) | 3 | - |
| 2e | `mov RD,	[<full address>]` | Copies RD to a location in the address space (absolute addressing mode) | 3 | - |
| 2f | `mov [<full address>],	RA` | Copies from location in the address space (absolute addressing mode) to RA | 3 | - |
| 30 | `mov [<full address>],	RB` | Copies from location in the address space (absolute addressing mode) to RB | 3 | - |
| 31 | `mov [<full address>],	RC` | Copies from location in the address space (absolute addressing mode) to RC | 3 | - |
| 32 | `mov [<full address>],	RD` | Copies from location in the address space (absolute addressing mode) to RD | 3 | - |
| 33 | `mov <byte>,	[<full address>]` | Copies a constant to a location in the address space (absolute addressing mode) | 4 | - |
| 34 | `mov [<page0 address>],	[<page0 address>]` | Copies a location in the address space (zero page addressing mode) to another location in the address space (zero page addressing mode) | 4 | - |
| 35 | `push RA` | Pushes RA onto the stack | 2 | - |
| 36 | `push RB` | Pushes RB onto the stack | 2 | - |
| 37 | `push RC` | Pushes RC onto the stack | 2 | - |
| 38 | `push RD` | Pushes RD onto the stack | 2 | - |
| 39 | `push [<page0 address>]` | Pushes a location in the address space (zero page addressing mode) onto the stack | 4 | - |
| 3a | `push [RC:RD]` | Pushes a location in the address space (full indirect addressing mode) onto the stack | 5 | - |
| 3b | `push [<full address>]` | Pushes a location in the address space (absolute addressing mode) onto the stack | 5 | - |
| 3c | `push <byte>` | Pushes a constant onto the stack | 2 | - |
| 3d | `pop RA` | Pops from the stack into RA | 1 | - |
| 3e | `pop RB` | Pops from the stack into RB | 1 | - |
| 3f | `pop RC` | Pops from the stack into RC | 1 | - |
| 40 | `pop RD` | Pops from the stack into RD | 1 | - |
| 41 | `pop [<page0 address>]` | Pops from the stack into a location in the address space (zero page addressing mode) | 3 | - |
| 42 | `pop [RC:RD]` | Pops from the stack into a location in the address space (full indirect addressing mode) | 4 | - |
| 43 | `pop [<full address>]` | Pops from the stack into a location in the address space (absolute addressing mode) | 4 | - |
| 44 | `add RA,	RA` | Adds RA to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 45 | `add RA,	RB` | Adds RB to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 46 | `add RA,	RC` | Adds RC to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 47 | `add RA,	RD` | Adds RD to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 48 | `add RB,	RA` | Adds RA to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 49 | `add RB,	RB` | Adds RB to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 4a | `add RB,	RC` | Adds RC to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 4b | `add RB,	RD` | Adds RD to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 4c | `add RC,	RA` | Adds RA to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 4d | `add RC,	RB` | Adds RB to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 4e | `add RC,	RC` | Adds RC to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 4f | `add RC,	RD` | Adds RD to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 50 | `add RD,	RA` | Adds RA to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 51 | `add RD,	RB` | Adds RB to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 52 | `add RD,	RC` | Adds RC to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 53 | `add RD,	RD` | Adds RD to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 54 | `add RA,	<byte>` | Adds a constant to RA, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| 55 | `add RB,	<byte>` | Adds a constant to RB, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| 56 | `add RC,	<byte>` | Adds a constant to RC, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| 57 | `add RD,	<byte>` | Adds a constant to RD, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| 58 | `add [<page0 address>],	RA` | Adds RA to a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 59 | `add [<page0 address>],	RB` | Adds RB to a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 5a | `add [<page0 address>],	RC` | Adds RC to a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 5b | `add [<page0 address>],	RD` | Adds RD to a location in the address space (zero page addressing mode), stores the result back in the address space (zero page addressing mode), then updates the FLAGS accordingly | 3 | `NZC` |
| 5c | `sub RA,	RA` | Subtracts RA from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 5d | `sub RA,	RB` | Subtracts RB from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 5e | `sub RA,	RC` | Subtracts RC from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 5f | `sub RA,	RD` | Subtracts RD from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 60 | `sub RB,	RA` | Subtracts RA from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 61 | `sub RB,	RB` | Subtracts RB from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 62 | `sub RB,	RC` | Subtracts RC from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 63 | `sub RB,	RD` | Subtracts RD from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 64 | `sub RC,	RA` | Subtracts RA from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 65 | `sub RC,	RB` | Subtracts RB from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 66 | `sub RC,	RC` | Subtracts RC from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 67 | `sub RC,	RD` | Subtracts RD from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 68 | `sub RD,	RA` | Subtracts RA from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 69 | `sub RD,	RB` | Subtracts RB from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 6a | `sub RD,	RC` | Subtracts RC from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 6b | `sub RD,	RD` | Subtracts RD from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 6c | `sub RA,	<byte>` | Subtracts a constant from RA, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| 6d | `sub RB,	<byte>` | Subtracts a constant from RB, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| 6e | `sub RC,	<byte>` | Subtracts a constant from RC, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| 6f | `sub RD,	<byte>` | Subtracts a constant from RD, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| 70 | `sub [<page0 address>],	RA` | Subtracts RA from a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 71 | `sub [<page0 address>],	RB` | Subtracts RD from a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 72 | `sub [<page0 address>],	RC` | Subtracts RC from a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 73 | `sub [<page0 address>],	RD` | Subtracts RD from a location in the address space (zero page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 74 | `cmp RA,	RA` | Compares RA to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 75 | `cmp RA,	RB` | Compares RA to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 76 | `cmp RA,	RC` | Compares RA to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 77 | `cmp RA,	RD` | Compares RA to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 78 | `cmp RB,	RA` | Compares RB to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 79 | `cmp RB,	RB` | Compares RB to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 7a | `cmp RB,	RC` | Compares RB to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 7b | `cmp RB,	RD` | Compares RB to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 7c | `cmp RC,	RA` | Compares RC to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 7d | `cmp RC,	RB` | Compares RC to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 7e | `cmp RC,	RC` | Compares RC to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 7f | `cmp RC,	RD` | Compares RC to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 80 | `cmp RD,	RA` | Compares RD to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 81 | `cmp RD,	RB` | Compares RD to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 82 | `cmp RD,	RC` | Compares RD to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 83 | `cmp RD,	RD` | Compares RD to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 84 | `cmp RA,	<byte>` | Compares RA to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 85 | `cmp RB,	<byte>` | Compares RB to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 86 | `cmp RC,	<byte>` | Compares RC to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 87 | `cmp RD,	<byte>` | Compares RD to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 88 | `cmp [<page0 address>],	RA` | Compares the given location in the address space (zero page addressing mode) to RA, then updates the FLAGS accordingly | 3 | `NZC` |
| 89 | `cmp [<page0 address>],	RB` | Compares the given location in the address space (zero page addressing mode) to RB, then updates the FLAGS accordingly | 3 | `NZC` |
| 8a | `cmp [<page0 address>],	RC` | Compares the given location in the address space (zero page addressing mode) to RC, then updates the FLAGS accordingly | 3 | `NZC` |
| 8b | `cmp [<page0 address>],	RD` | Compares the given location in the address space (zero page addressing mode) to RD, then updates the FLAGS accordingly | 3 | `NZC` |
| 8c | `or RA,	RA` | Performs a bitwise OR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 8d | `or RA,	RB` | Performs a bitwise OR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 8e | `or RA,	RC` | Performs a bitwise OR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 8f | `or RA,	RD` | Performs a bitwise OR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 90 | `or RB,	RA` | Performs a bitwise OR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 91 | `or RB,	RB` | Performs a bitwise OR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 92 | `or RB,	RC` | Performs a bitwise OR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 93 | `or RB,	RD` | Performs a bitwise OR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 94 | `or RC,	RA` | Performs a bitwise OR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 95 | `or RC,	RB` | Performs a bitwise OR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 96 | `or RC,	RC` | Performs a bitwise OR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 97 | `or RC,	RD` | Performs a bitwise OR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 98 | `or RD,	RA` | Performs a bitwise OR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| 99 | `or RD,	RB` | Performs a bitwise OR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| 9a | `or RD,	RC` | Performs a bitwise OR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| 9b | `or RD,	RD` | Performs a bitwise OR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| 9c | `or RA,	<byte>` | Performs a bitwise OR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 | - |
| 9d | `or RB,	<byte>` | Performs a bitwise OR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 | - |
| 9e | `or RC,	<byte>` | Performs a bitwise OR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 | - |
| 9f | `or RD,	<byte>` | Performs a bitwise OR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 | - |
| a0 | `or [<page0 address>],	RA` | Performs a bitwise OR of a location in the address space (zero page addressing mode) and RA, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| a1 | `or [<page0 address>],	RB` | Performs a bitwise OR of a location in the address space (zero page addressing mode) and RB, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| a2 | `or [<page0 address>],	RC` | Performs a bitwise OR of a location in the address space (zero page addressing mode) and RC, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| a3 | `or [<page0 address>],	RD` | Performs a bitwise OR of a location in the address space (zero page addressing mode) and RD, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| a4 | `and RA,	RA` | Performs a bitwise AND of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| a5 | `and RA,	RB` | Performs a bitwise AND of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| a6 | `and RA,	RC` | Performs a bitwise AND of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| a7 | `and RA,	RD` | Performs a bitwise AND of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| a8 | `and RB,	RA` | Performs a bitwise AND of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| a9 | `and RB,	RB` | Performs a bitwise AND of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| aa | `and RB,	RC` | Performs a bitwise AND of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| ab | `and RB,	RD` | Performs a bitwise AND of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| ac | `and RC,	RA` | Performs a bitwise AND of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| ad | `and RC,	RB` | Performs a bitwise AND of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| ae | `and RC,	RC` | Performs a bitwise AND of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| af | `and RC,	RD` | Performs a bitwise AND of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| b0 | `and RD,	RA` | Performs a bitwise AND of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| b1 | `and RD,	RB` | Performs a bitwise AND of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| b2 | `and RD,	RC` | Performs a bitwise AND of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| b3 | `and RD,	RD` | Performs a bitwise AND of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| b4 | `and RA,	<byte>` | Performs a bitwise AND of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| b5 | `and RB,	<byte>` | Performs a bitwise AND of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| b6 | `and RC,	<byte>` | Performs a bitwise AND of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| b7 | `and RD,	<byte>` | Performs a bitwise AND of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| b8 | `and [<page0 address>],	RA` | Performs a bitwise AND of a location in the address space (zero page addressing mode) and RA, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| b9 | `and [<page0 address>],	RB` | Performs a bitwise AND of a location in the address space (zero page addressing mode) and RB, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| ba | `and [<page0 address>],	RC` | Performs a bitwise AND of a location in the address space (zero page addressing mode) and RC, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| bb | `and [<page0 address>],	RD` | Performs a bitwise AND of a location in the address space (zero page addressing mode) and RD, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| bc | `xor RA,	RA` | Performs a bitwise XOR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| bd | `xor RA,	RB` | Performs a bitwise XOR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| be | `xor RA,	RC` | Performs a bitwise XOR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| bf | `xor RA,	RD` | Performs a bitwise XOR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| c0 | `xor RB,	RA` | Performs a bitwise XOR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| c1 | `xor RB,	RB` | Performs a bitwise XOR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| c2 | `xor RB,	RC` | Performs a bitwise XOR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| c3 | `xor RB,	RD` | Performs a bitwise XOR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| c4 | `xor RC,	RA` | Performs a bitwise XOR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| c5 | `xor RC,	RB` | Performs a bitwise XOR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| c6 | `xor RC,	RC` | Performs a bitwise XOR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| c7 | `xor RC,	RD` | Performs a bitwise XOR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| c8 | `xor RD,	RA` | Performs a bitwise XOR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| c9 | `xor RD,	RB` | Performs a bitwise XOR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| ca | `xor RD,	RC` | Performs a bitwise XOR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| cb | `xor RD,	RD` | Performs a bitwise XOR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| cc | `xor RA,	<byte>` | Performs a bitwise XOR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| cd | `xor RB,	<byte>` | Performs a bitwise XOR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| ce | `xor RC,	<byte>` | Performs a bitwise XOR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| cf | `xor RD,	<byte>` | Performs a bitwise XOR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| d0 | `xor [<page0 address>],	RA` | Performs a bitwise XOR of a location in the address space (zero page addressing mode) and RA, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| d1 | `xor [<page0 address>],	RB` | Performs a bitwise XOR of a location in the address space (zero page addressing mode) and RB, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| d2 | `xor [<page0 address>],	RC` | Performs a bitwise XOR of a location in the address space (zero page addressing mode) and RC, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| d3 | `xor [<page0 address>],	RD` | Performs a bitwise XOR of a location in the address space (zero page addressing mode) and RD, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| d4 | `not RA` | Performs a bitwise NOT of RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| d5 | `not RB` | Performs a bitwise NOT of RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| d6 | `not RC` | Performs a bitwise NOT of RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| d7 | `not RD` | Performs a bitwise NOT of RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| d8 | `not [<page0 address>]` | Performs a bitwise NOT of a location in the address space (page 0 addressing mode), stores the result back in the location, then updates the FLAGS accordingly | 3 | `NZC` |
| d9 | `inc RA` | Increments RA by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| da | `inc RB` | Increments RB by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| db | `inc RC` | Increments RC by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| dc | `inc RD` | Increments RD by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| dd | `inc [<page0 address>]` | Increments a location in the address space (zero page addressing mode) by 1, then updates the FLAGS accordingly | 3 | `NZC` |
| de | `dec RA` | Decrements RA by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| df | `dec RB` | Decrements RB by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e0 | `dec RC` | Decrements RC by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e1 | `dec RD` | Decrements RD by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e2 | `dec [<page0 address>]` | Decrements a location in the address space (zero page addressing mode) by 1, then updates the FLAGS accordingly | 3 | `NZC` |
| e3 | `shl RA` | Performs a left bitwise shift of RA by 1 bit | 1 | - |
| e4 | `shr RA` | Performs a right bitwise shift of RA by 1 bit | 1 | - |
| e5 | `swap RA,	RB` | Swaps the values of RA and RB | 3 | - |
| e6 | `swap RA,	RC` | Swaps the values of RA and RC | 3 | - |
| e7 | `swap RA,	RD` | Swaps the values of RA and RD | 3 | - |
| e8 | `swap RB,	RC` | Swaps the values of RB and RC | 3 | - |
| e9 | `swap RB,	RD` | Swaps the values of RB and RD | 3 | - |
| ea | `swap RC,	RD` | Swaps the values of RC and RD | 3 | - |
| eb | `jc <byte>` | Jumps to the given short address (same code segment) if the carry flag is set | 3 | - |
| ec | `jn <byte>` | Jumps to the given short address (same code segment) if the negative flag is set | 3 | - |
| ed | `jz <byte>` | Jumps to the given short address (same code segment) if the zero flag is set | 3 | - |
| ee | `goto <byte>` | Jumps to the given short address (same code segment) | 3 | - |
| ef | `ljc <dcst>` | Jumps to the given long address if the carry flag is set | 4 | - |
| f0 | `ljn <dcst>` | Jumps to the given long address if the negative flag is set | 4 | - |
| f1 | `ljz <dcst>` | Jumps to the given long address if the zero flag is set | 4 | - |
| f2 | `lgoto <dcst>` | Jumps to the given long address | 4 | - |
| f3 | `lgoto RC:RD` | Jumps to the long address specified by RC:RD | 4 | - |
| f4 | `lcall <dcst>` | Calls the given long address | 6 | - |
| f5 | `lcall RC:RD` | Calls the long address specified by RC:RD | 6 | - |
| f6 | `ret ` | Returns from a call | 4 | - |
| f7 | `iret ` | Returns from an interrupt | 5 | - |
| f8 | `cid ` | Clears the interrupt disable flag | 1 | `I` |
| f9 | `sid ` | Sets the interrupt disable flag | 1 | `I` |
| fa | `clc ` | Clears the carry flag | 1 | `C` |
| fb | `sec ` | Sets the carry flag | 1 | `C` |
