

### lnasm instruction set


| Opcode | Syntax | Description | Clock cycles | Flags Affected |
|--------|--------|-------------|--------------|----------------|
| 00 | `nop ` | No operation | 1 | - |
| 01 | `hlt ` | Halts the CPU | 1 | - |
| 02 | `$brk$ ` | Reserved opcode (hardware interrupt). Do not use. | 7 | `I` |
| 03 | `int ` | Software interrupt. Pushes CS:PC and FLAGS, then calls the interrupt vector | 7 | `I` |
| 04 | `mov <byte>,	RA` | Moves a constant to RA | 2 | - |
| 05 | `mov <byte>,	RB` | Moves a constant to RB | 2 | - |
| 06 | `mov <byte>,	RC` | Moves a constant to RC | 2 | - |
| 07 | `mov <byte>,	RD` | Moves a constant to RD | 2 | - |
| 08 | `mov <byte>,	SP` | Moves a constant to SP | 2 | - |
| 09 | `mov <byte>,	SS` | Moves a constant to SS | 2 | - |
| 0a | `mov <byte>,	DS` | Moves a constant to DS | 2 | - |
| 0b | `mov SS,	RD` | Moves SS to RD | 1 | - |
| 0c | `mov SP,	RD` | Moves SP to RD | 1 | - |
| 0d | `mov DS,	RD` | Moves DS to RD | 1 | - |
| 0e | `mov RA,	RB` | Moves RA to RB | 1 | - |
| 0f | `mov RA,	RC` | Moves RA to RC | 1 | - |
| 10 | `mov RA,	RD` | Moves RA to RD | 1 | - |
| 11 | `mov RB,	RA` | Moves RB to RA | 1 | - |
| 12 | `mov RB,	RC` | Moves RB to RC | 1 | - |
| 13 | `mov RB,	RD` | Moves RB to RD | 1 | - |
| 14 | `mov RC,	RA` | Moves RC to RA | 1 | - |
| 15 | `mov RC,	RB` | Moves RC to RB | 1 | - |
| 16 | `mov RC,	RD` | Moves RC to RD | 1 | - |
| 17 | `mov RD,	RA` | Moves RD to RA | 1 | - |
| 18 | `mov RD,	RB` | Moves RD to RB | 1 | - |
| 19 | `mov RD,	RC` | Moves RD to RC | 1 | - |
| 1a | `mov RD,	SS` | Moves RD to SS | 1 | - |
| 1b | `mov RD,	SP` | Moves RD to SP | 1 | - |
| 1c | `mov RD,	DS` | Moves RD to DS | 1 | - |
| 1d | `mov RA,	[<data page address>]` | Copies RA to a location in the address space (data page addressing mode) | 2 | - |
| 1e | `mov RB,	[<data page address>]` | Copies RB to a location in the address space (data page addressing mode) | 2 | - |
| 1f | `mov RC,	[<data page address>]` | Copies RC to a location in the address space (data page addressing mode) | 2 | - |
| 20 | `mov RD,	[<data page address>]` | Copies RD to a location in the address space (data page addressing mode) | 2 | - |
| 21 | `mov [<data page address>],	RA` | Copies from location in the address space (data page addressing mode) to RA | 2 | - |
| 22 | `mov [<data page address>],	RB` | Copies from location in the address space (data page addressing mode) to RB | 2 | - |
| 23 | `mov [<data page address>],	RC` | Copies from location in the address space (data page addressing mode) to RC | 2 | - |
| 24 | `mov [<data page address>],	RD` | Copies from location in the address space (data page addressing mode) to RD | 2 | - |
| 25 | `mov <byte>,	[<data page address>]` | Copies a constant to a location in the address space (data page addressing mode) | 3 | - |
| 26 | `mov RA,	[RC:RD]` | Copies RA to a location in the address space (full indirect addressing mode) | 3 | - |
| 27 | `mov RB,	[RC:RD]` | Copies RB to a location in the address space (full indirect addressing mode) | 3 | - |
| 28 | `mov RC,	[RC:RD]` | Copies RC to a location in the address space (full indirect addressing mode) | 3 | - |
| 29 | `mov RD,	[RC:RD]` | Copies RD to a location in the address space (full indirect addressing mode) | 3 | - |
| 2a | `mov [RC:RD],	RA` | Copies from location in the address space (full indirect addressing mode) to RA | 3 | - |
| 2b | `mov [RC:RD],	RB` | Copies from location in the address space (full indirect addressing mode) to RB | 3 | - |
| 2c | `mov [RC:RD],	RC` | Copies from location in the address space (full indirect addressing mode) to RC | 3 | - |
| 2d | `mov [RC:RD],	RD` | Copies from location in the address space (full indirect addressing mode) to RD | 3 | - |
| 2e | `mov <byte>,	[RC:RD]` | Copies a constant to a location in the address space (full indirect addressing mode) | 4 | - |
| 2f | `mov RA,	[<full address>]` | Copies RA to a location in the address space (absolute addressing mode) | 3 | - |
| 30 | `mov RB,	[<full address>]` | Copies RB to a location in the address space (absolute addressing mode) | 3 | - |
| 31 | `mov RC,	[<full address>]` | Copies RC to a location in the address space (absolute addressing mode) | 3 | - |
| 32 | `mov RD,	[<full address>]` | Copies RD to a location in the address space (absolute addressing mode) | 3 | - |
| 33 | `mov [<full address>],	RA` | Copies from location in the address space (absolute addressing mode) to RA | 3 | - |
| 34 | `mov [<full address>],	RB` | Copies from location in the address space (absolute addressing mode) to RB | 3 | - |
| 35 | `mov [<full address>],	RC` | Copies from location in the address space (absolute addressing mode) to RC | 3 | - |
| 36 | `mov [<full address>],	RD` | Copies from location in the address space (absolute addressing mode) to RD | 3 | - |
| 37 | `mov <byte>,	[<full address>]` | Copies a constant to a location in the address space (absolute addressing mode) | 4 | - |
| 38 | `mov [<data page address>],	[<data page address>]` | Copies a location in the address space (data page addressing mode) to another location in the address space (data page addressing mode) | 4 | - |
| 39 | `push RA` | Pushes RA onto the stack | 2 | - |
| 3a | `push RB` | Pushes RB onto the stack | 2 | - |
| 3b | `push RC` | Pushes RC onto the stack | 2 | - |
| 3c | `push RD` | Pushes RD onto the stack | 2 | - |
| 3d | `push [<data page address>]` | Pushes a location in the address space (data page addressing mode) onto the stack | 4 | - |
| 3e | `push [RC:RD]` | Pushes a location in the address space (full indirect addressing mode) onto the stack | 5 | - |
| 3f | `push [<full address>]` | Pushes a location in the address space (absolute addressing mode) onto the stack | 5 | - |
| 40 | `push <byte>` | Pushes a constant onto the stack | 2 | - |
| 41 | `pop RA` | Pops from the stack into RA | 1 | - |
| 42 | `pop RB` | Pops from the stack into RB | 1 | - |
| 43 | `pop RC` | Pops from the stack into RC | 1 | - |
| 44 | `pop RD` | Pops from the stack into RD | 1 | - |
| 45 | `pop [<data page address>]` | Pops from the stack into a location in the address space (data page addressing mode) | 3 | - |
| 46 | `pop [RC:RD]` | Pops from the stack into a location in the address space (full indirect addressing mode) | 4 | - |
| 47 | `pop [<full address>]` | Pops from the stack into a location in the address space (absolute addressing mode) | 4 | - |
| 48 | `add RA,	RA` | Adds RA to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 49 | `add RA,	RB` | Adds RB to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 4a | `add RA,	RC` | Adds RC to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 4b | `add RA,	RD` | Adds RD to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 4c | `add RB,	RA` | Adds RA to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 4d | `add RB,	RB` | Adds RB to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 4e | `add RB,	RC` | Adds RC to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 4f | `add RB,	RD` | Adds RD to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 50 | `add RC,	RA` | Adds RA to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 51 | `add RC,	RB` | Adds RB to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 52 | `add RC,	RC` | Adds RC to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 53 | `add RC,	RD` | Adds RD to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 54 | `add RD,	RA` | Adds RA to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 55 | `add RD,	RB` | Adds RB to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 56 | `add RD,	RC` | Adds RC to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 57 | `add RD,	RD` | Adds RD to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 58 | `add RA,	<byte>` | Adds a constant to RA, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| 59 | `add RB,	<byte>` | Adds a constant to RB, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| 5a | `add RC,	<byte>` | Adds a constant to RC, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| 5b | `add RD,	<byte>` | Adds a constant to RD, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| 5c | `add [<data page address>],	RA` | Adds RA to a location in the address space (data page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 5d | `add [<data page address>],	RB` | Adds RB to a location in the address space (data page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 5e | `add [<data page address>],	RC` | Adds RC to a location in the address space (data page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 5f | `add [<data page address>],	RD` | Adds RD to a location in the address space (data page addressing mode), stores the result back in the address space (data page addressing mode), then updates the FLAGS accordingly | 3 | `NZC` |
| 60 | `sub RA,	RA` | Subtracts RA from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 61 | `sub RA,	RB` | Subtracts RB from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 62 | `sub RA,	RC` | Subtracts RC from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 63 | `sub RA,	RD` | Subtracts RD from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 64 | `sub RB,	RA` | Subtracts RA from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 65 | `sub RB,	RB` | Subtracts RB from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 66 | `sub RB,	RC` | Subtracts RC from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 67 | `sub RB,	RD` | Subtracts RD from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 68 | `sub RC,	RA` | Subtracts RA from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 69 | `sub RC,	RB` | Subtracts RB from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 6a | `sub RC,	RC` | Subtracts RC from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 6b | `sub RC,	RD` | Subtracts RD from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 6c | `sub RD,	RA` | Subtracts RA from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 6d | `sub RD,	RB` | Subtracts RB from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 6e | `sub RD,	RC` | Subtracts RC from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 6f | `sub RD,	RD` | Subtracts RD from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 70 | `sub RA,	<byte>` | Subtracts a constant from RA, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| 71 | `sub RB,	<byte>` | Subtracts a constant from RB, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| 72 | `sub RC,	<byte>` | Subtracts a constant from RC, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| 73 | `sub RD,	<byte>` | Subtracts a constant from RD, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| 74 | `sub [<data page address>],	RA` | Subtracts RA from a location in the address space (data page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 75 | `sub [<data page address>],	RB` | Subtracts RD from a location in the address space (data page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 76 | `sub [<data page address>],	RC` | Subtracts RC from a location in the address space (data page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 77 | `sub [<data page address>],	RD` | Subtracts RD from a location in the address space (data page addressing mode), stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| 78 | `cmp RA,	RA` | Compares RA to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 79 | `cmp RA,	RB` | Compares RA to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 7a | `cmp RA,	RC` | Compares RA to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 7b | `cmp RA,	RD` | Compares RA to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 7c | `cmp RB,	RA` | Compares RB to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 7d | `cmp RB,	RB` | Compares RB to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 7e | `cmp RB,	RC` | Compares RB to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 7f | `cmp RB,	RD` | Compares RB to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 80 | `cmp RC,	RA` | Compares RC to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 81 | `cmp RC,	RB` | Compares RC to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 82 | `cmp RC,	RC` | Compares RC to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 83 | `cmp RC,	RD` | Compares RC to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 84 | `cmp RD,	RA` | Compares RD to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 85 | `cmp RD,	RB` | Compares RD to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 86 | `cmp RD,	RC` | Compares RD to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 87 | `cmp RD,	RD` | Compares RD to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 88 | `cmp RA,	<byte>` | Compares RA to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 89 | `cmp RB,	<byte>` | Compares RB to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 8a | `cmp RC,	<byte>` | Compares RC to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 8b | `cmp RD,	<byte>` | Compares RD to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 8c | `cmp [<data page address>],	RA` | Compares the given location in the address space (data page addressing mode) to RA, then updates the FLAGS accordingly | 3 | `NZC` |
| 8d | `cmp [<data page address>],	RB` | Compares the given location in the address space (data page addressing mode) to RB, then updates the FLAGS accordingly | 3 | `NZC` |
| 8e | `cmp [<data page address>],	RC` | Compares the given location in the address space (data page addressing mode) to RC, then updates the FLAGS accordingly | 3 | `NZC` |
| 8f | `cmp [<data page address>],	RD` | Compares the given location in the address space (data page addressing mode) to RD, then updates the FLAGS accordingly | 3 | `NZC` |
| 90 | `or RA,	RA` | Performs a bitwise OR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 91 | `or RA,	RB` | Performs a bitwise OR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 92 | `or RA,	RC` | Performs a bitwise OR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 93 | `or RA,	RD` | Performs a bitwise OR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 94 | `or RB,	RA` | Performs a bitwise OR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 95 | `or RB,	RB` | Performs a bitwise OR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 96 | `or RB,	RC` | Performs a bitwise OR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 97 | `or RB,	RD` | Performs a bitwise OR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 98 | `or RC,	RA` | Performs a bitwise OR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 99 | `or RC,	RB` | Performs a bitwise OR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 9a | `or RC,	RC` | Performs a bitwise OR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 9b | `or RC,	RD` | Performs a bitwise OR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 9c | `or RD,	RA` | Performs a bitwise OR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| 9d | `or RD,	RB` | Performs a bitwise OR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| 9e | `or RD,	RC` | Performs a bitwise OR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| 9f | `or RD,	RD` | Performs a bitwise OR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| a0 | `or RA,	<byte>` | Performs a bitwise OR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 | - |
| a1 | `or RB,	<byte>` | Performs a bitwise OR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 | - |
| a2 | `or RC,	<byte>` | Performs a bitwise OR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 | - |
| a3 | `or RD,	<byte>` | Performs a bitwise OR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 | - |
| a4 | `or [<data page address>],	RA` | Performs a bitwise OR of a location in the address space (data page addressing mode) and RA, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| a5 | `or [<data page address>],	RB` | Performs a bitwise OR of a location in the address space (data page addressing mode) and RB, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| a6 | `or [<data page address>],	RC` | Performs a bitwise OR of a location in the address space (data page addressing mode) and RC, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| a7 | `or [<data page address>],	RD` | Performs a bitwise OR of a location in the address space (data page addressing mode) and RD, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| a8 | `and RA,	RA` | Performs a bitwise AND of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| a9 | `and RA,	RB` | Performs a bitwise AND of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| aa | `and RA,	RC` | Performs a bitwise AND of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| ab | `and RA,	RD` | Performs a bitwise AND of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| ac | `and RB,	RA` | Performs a bitwise AND of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| ad | `and RB,	RB` | Performs a bitwise AND of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| ae | `and RB,	RC` | Performs a bitwise AND of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| af | `and RB,	RD` | Performs a bitwise AND of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| b0 | `and RC,	RA` | Performs a bitwise AND of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| b1 | `and RC,	RB` | Performs a bitwise AND of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| b2 | `and RC,	RC` | Performs a bitwise AND of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| b3 | `and RC,	RD` | Performs a bitwise AND of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| b4 | `and RD,	RA` | Performs a bitwise AND of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| b5 | `and RD,	RB` | Performs a bitwise AND of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| b6 | `and RD,	RC` | Performs a bitwise AND of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| b7 | `and RD,	RD` | Performs a bitwise AND of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| b8 | `and RA,	<byte>` | Performs a bitwise AND of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| b9 | `and RB,	<byte>` | Performs a bitwise AND of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| ba | `and RC,	<byte>` | Performs a bitwise AND of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| bb | `and RD,	<byte>` | Performs a bitwise AND of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| bc | `and [<data page address>],	RA` | Performs a bitwise AND of a location in the address space (data page addressing mode) and RA, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| bd | `and [<data page address>],	RB` | Performs a bitwise AND of a location in the address space (data page addressing mode) and RB, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| be | `and [<data page address>],	RC` | Performs a bitwise AND of a location in the address space (data page addressing mode) and RC, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| bf | `and [<data page address>],	RD` | Performs a bitwise AND of a location in the address space (data page addressing mode) and RD, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| c0 | `xor RA,	RA` | Performs a bitwise XOR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| c1 | `xor RA,	RB` | Performs a bitwise XOR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| c2 | `xor RA,	RC` | Performs a bitwise XOR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| c3 | `xor RA,	RD` | Performs a bitwise XOR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| c4 | `xor RB,	RA` | Performs a bitwise XOR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| c5 | `xor RB,	RB` | Performs a bitwise XOR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| c6 | `xor RB,	RC` | Performs a bitwise XOR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| c7 | `xor RB,	RD` | Performs a bitwise XOR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| c8 | `xor RC,	RA` | Performs a bitwise XOR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| c9 | `xor RC,	RB` | Performs a bitwise XOR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| ca | `xor RC,	RC` | Performs a bitwise XOR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| cb | `xor RC,	RD` | Performs a bitwise XOR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| cc | `xor RD,	RA` | Performs a bitwise XOR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| cd | `xor RD,	RB` | Performs a bitwise XOR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| ce | `xor RD,	RC` | Performs a bitwise XOR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| cf | `xor RD,	RD` | Performs a bitwise XOR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| d0 | `xor RA,	<byte>` | Performs a bitwise XOR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| d1 | `xor RB,	<byte>` | Performs a bitwise XOR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| d2 | `xor RC,	<byte>` | Performs a bitwise XOR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| d3 | `xor RD,	<byte>` | Performs a bitwise XOR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| d4 | `xor [<data page address>],	RA` | Performs a bitwise XOR of a location in the address space (data page addressing mode) and RA, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| d5 | `xor [<data page address>],	RB` | Performs a bitwise XOR of a location in the address space (data page addressing mode) and RB, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| d6 | `xor [<data page address>],	RC` | Performs a bitwise XOR of a location in the address space (data page addressing mode) and RC, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| d7 | `xor [<data page address>],	RD` | Performs a bitwise XOR of a location in the address space (data page addressing mode) and RD, stores the result back in the given location, then updates the FLAGS accordingly | 3 | `NZC` |
| d8 | `not RA` | Performs a bitwise NOT of RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| d9 | `not RB` | Performs a bitwise NOT of RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| da | `not RC` | Performs a bitwise NOT of RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| db | `not RD` | Performs a bitwise NOT of RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| dc | `not [<data page address>]` | Performs a bitwise NOT of a location in the address space (page 0 addressing mode), stores the result back in the location, then updates the FLAGS accordingly | 3 | `NZC` |
| dd | `inc RA` | Increments RA by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| de | `inc RB` | Increments RB by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| df | `inc RC` | Increments RC by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e0 | `inc RD` | Increments RD by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e1 | `inc [<data page address>]` | Increments a location in the address space (data page addressing mode) by 1, then updates the FLAGS accordingly | 3 | `NZC` |
| e2 | `dec RA` | Decrements RA by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e3 | `dec RB` | Decrements RB by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e4 | `dec RC` | Decrements RC by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e5 | `dec RD` | Decrements RD by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e6 | `dec [<data page address>]` | Decrements a location in the address space (data page addressing mode) by 1, then updates the FLAGS accordingly | 3 | `NZC` |
| e7 | `shl RA` | Performs a left bitwise shift of RA by 1 bit | 1 | - |
| e8 | `shr RA` | Performs a right bitwise shift of RA by 1 bit | 1 | - |
| e9 | `swap RA,	RB` | Swaps the values of RA and RB | 3 | - |
| ea | `swap RA,	RC` | Swaps the values of RA and RC | 3 | - |
| eb | `swap RA,	RD` | Swaps the values of RA and RD | 3 | - |
| ec | `swap RB,	RC` | Swaps the values of RB and RC | 3 | - |
| ed | `swap RB,	RD` | Swaps the values of RB and RD | 3 | - |
| ee | `swap RC,	RD` | Swaps the values of RC and RD | 3 | - |
| ef | `jc <byte>` | Jumps to the given short address (same code segment) if the carry flag is set | 3 | - |
| f0 | `jn <byte>` | Jumps to the given short address (same code segment) if the negative flag is set | 3 | - |
| f1 | `jz <byte>` | Jumps to the given short address (same code segment) if the zero flag is set | 3 | - |
| f2 | `goto <byte>` | Jumps to the given short address (same code segment) | 3 | - |
| f3 | `ljc <dcst>` | Jumps to the given long address if the carry flag is set | 4 | - |
| f4 | `ljn <dcst>` | Jumps to the given long address if the negative flag is set | 4 | - |
| f5 | `ljz <dcst>` | Jumps to the given long address if the zero flag is set | 4 | - |
| f6 | `lgoto <dcst>` | Jumps to the given long address | 4 | - |
| f7 | `lgoto RC:RD` | Jumps to the long address specified by RC:RD | 4 | - |
| f8 | `lcall <dcst>` | Calls the given long address | 6 | - |
| f9 | `lcall RC:RD` | Calls the long address specified by RC:RD | 6 | - |
| fa | `ret ` | Returns from a call | 4 | - |
| fb | `iret ` | Returns from an interrupt | 5 | - |
| fc | `cid ` | Clears the interrupt disable flag | 1 | `I` |
| fd | `sid ` | Sets the interrupt disable flag | 1 | `I` |
| fe | `clc ` | Clears the carry flag | 1 | `C` |
| ff | `sec ` | Sets the carry flag | 1 | `C` |
