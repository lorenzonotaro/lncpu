

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
| 0b | `mov <byte>,	BP` | Moves a constant to BP | 2 | - |
| 0c | `mov SS,	RD` | Moves SS to RD | 1 | - |
| 0d | `mov SP,	RD` | Moves SP to RD | 1 | - |
| 0e | `mov SP,	BP` | Moves SP to BP | 1 | - |
| 0f | `mov BP,	SP` | Moves BP to SP | 1 | - |
| 10 | `mov DS,	RD` | Moves DS to RD | 1 | - |
| 11 | `mov RA,	RB` | Moves RA to RB | 1 | - |
| 12 | `mov RA,	RC` | Moves RA to RC | 1 | - |
| 13 | `mov RA,	RD` | Moves RA to RD | 1 | - |
| 14 | `mov RB,	RA` | Moves RB to RA | 1 | - |
| 15 | `mov RB,	RC` | Moves RB to RC | 1 | - |
| 16 | `mov RB,	RD` | Moves RB to RD | 1 | - |
| 17 | `mov RC,	RA` | Moves RC to RA | 1 | - |
| 18 | `mov RC,	RB` | Moves RC to RB | 1 | - |
| 19 | `mov RC,	RD` | Moves RC to RD | 1 | - |
| 1a | `mov RD,	RA` | Moves RD to RA | 1 | - |
| 1b | `mov RD,	RB` | Moves RD to RB | 1 | - |
| 1c | `mov RD,	RC` | Moves RD to RC | 1 | - |
| 1d | `mov RD,	SS` | Moves RD to SS | 1 | - |
| 1e | `mov RD,	SP` | Moves RD to SP | 1 | - |
| 1f | `mov RD,	DS` | Moves RD to DS | 1 | - |
| 20 | `mov [BP +/- <offset>],	RA` | Copies a from a location in the address space (stack frame offset addressing mode) to RA | 4 | - |
| 21 | `mov [BP +/- <offset>],	RB` | Copies a from a location in the address space (stack frame offset addressing mode) to RB | 4 | - |
| 22 | `mov [BP +/- <offset>],	RC` | Copies a from a location in the address space (stack frame offset addressing mode) to RC | 4 | - |
| 23 | `mov [BP +/- <offset>],	RD` | Copies a from a location in the address space (stack frame offset addressing mode) to RD | 4 | - |
| 24 | `mov RA,	[BP +/- <offset>]` | Copies RA to a location in the address space (stack frame offset addressing mode) | 4 | - |
| 25 | `mov RB,	[BP +/- <offset>]` | Copies RB to a location in the address space (stack frame offset addressing mode) | 4 | - |
| 26 | `mov RC,	[BP +/- <offset>]` | Copies RC to a location in the address space (stack frame offset addressing mode) | 4 | - |
| 27 | `mov RD,	[BP +/- <offset>]` | Copies RD to a location in the address space (stack frame offset addressing mode) | 4 | - |
| 28 | `mov <byte>,	[BP +/- <offset>]` | Copies a constant to a location in the address space (stack frame offset addressing mode) | 5 | - |
| 29 | `mov RA,	[<data page address>]` | Copies RA to a location in the address space (data page addressing mode) | 2 | - |
| 2a | `mov RB,	[<data page address>]` | Copies RB to a location in the address space (data page addressing mode) | 2 | - |
| 2b | `mov RC,	[<data page address>]` | Copies RC to a location in the address space (data page addressing mode) | 2 | - |
| 2c | `mov RD,	[<data page address>]` | Copies RD to a location in the address space (data page addressing mode) | 2 | - |
| 2d | `mov [<data page address>],	RA` | Copies from location in the address space (data page addressing mode) to RA | 2 | - |
| 2e | `mov [<data page address>],	RB` | Copies from location in the address space (data page addressing mode) to RB | 2 | - |
| 2f | `mov [<data page address>],	RC` | Copies from location in the address space (data page addressing mode) to RC | 2 | - |
| 30 | `mov [<data page address>],	RD` | Copies from location in the address space (data page addressing mode) to RD | 2 | - |
| 31 | `mov <byte>,	[<data page address>]` | Copies a constant to a location in the address space (data page addressing mode) | 3 | - |
| 32 | `mov RA,	[RC:RD]` | Copies RA to a location in the address space (full indirect addressing mode) | 3 | - |
| 33 | `mov RB,	[RC:RD]` | Copies RB to a location in the address space (full indirect addressing mode) | 3 | - |
| 34 | `mov RC,	[RC:RD]` | Copies RC to a location in the address space (full indirect addressing mode) | 3 | - |
| 35 | `mov RD,	[RC:RD]` | Copies RD to a location in the address space (full indirect addressing mode) | 3 | - |
| 36 | `mov [RC:RD],	RA` | Copies from location in the address space (full indirect addressing mode) to RA | 3 | - |
| 37 | `mov [RC:RD],	RB` | Copies from location in the address space (full indirect addressing mode) to RB | 3 | - |
| 38 | `mov [RC:RD],	RC` | Copies from location in the address space (full indirect addressing mode) to RC | 3 | - |
| 39 | `mov [RC:RD],	RD` | Copies from location in the address space (full indirect addressing mode) to RD | 3 | - |
| 3a | `mov <byte>,	[RC:RD]` | Copies a constant to a location in the address space (full indirect addressing mode) | 4 | - |
| 3b | `mov RA,	[<full address>]` | Copies RA to a location in the address space (absolute addressing mode) | 3 | - |
| 3c | `mov RB,	[<full address>]` | Copies RB to a location in the address space (absolute addressing mode) | 3 | - |
| 3d | `mov RC,	[<full address>]` | Copies RC to a location in the address space (absolute addressing mode) | 3 | - |
| 3e | `mov RD,	[<full address>]` | Copies RD to a location in the address space (absolute addressing mode) | 3 | - |
| 3f | `mov [<full address>],	RA` | Copies from location in the address space (absolute addressing mode) to RA | 3 | - |
| 40 | `mov [<full address>],	RB` | Copies from location in the address space (absolute addressing mode) to RB | 3 | - |
| 41 | `mov [<full address>],	RC` | Copies from location in the address space (absolute addressing mode) to RC | 3 | - |
| 42 | `mov [<full address>],	RD` | Copies from location in the address space (absolute addressing mode) to RD | 3 | - |
| 43 | `mov <byte>,	[<full address>]` | Copies a constant to a location in the address space (absolute addressing mode) | 4 | - |
| 44 | `mov [<data page address>],	[<data page address>]` | Copies a location in the address space (data page addressing mode) to another location in the address space (data page addressing mode) | 4 | - |
| 45 | `mov [RD],	RA` | Copies from a location in the address space (short indirect addressing mode) to RA | 2 | - |
| 46 | `mov [RD],	RB` | Copies from a location in the address space (short indirect addressing mode) to RB | 2 | - |
| 47 | `mov [RD],	RC` | Copies from a location in the address space (short indirect addressing mode) to RC | 2 | - |
| 48 | `mov [RD],	RD` | Copies from a location in the address space (short indirect addressing mode) to RD | 2 | - |
| 49 | `mov RA,	[RD]` | Copies RA to a location in the address space (short indirect addressing mode) | 2 | - |
| 4a | `mov RB,	[RD]` | Copies RB to a location in the address space (short indirect addressing mode) | 2 | - |
| 4b | `mov RC,	[RD]` | Copies RC to a location in the address space (short indirect addressing mode) | 2 | - |
| 4c | `mov RD,	[RD]` | Copies RD to a location in the address space (short indirect addressing mode) | 2 | - |
| 4d | `mov <byte>,	[RD]` | Copies a constant to a location in the address space (short indirect addressing mode) | 3 | - |
| 4e | `push RA` | Pushes RA onto the stack | 2 | - |
| 4f | `push RB` | Pushes RB onto the stack | 2 | - |
| 50 | `push RC` | Pushes RC onto the stack | 2 | - |
| 51 | `push RD` | Pushes RD onto the stack | 2 | - |
| 52 | `push [<data page address>]` | Pushes a location in the address space (data page addressing mode) onto the stack | 4 | - |
| 53 | `push [RD]` | Pushes a location in the address space (short indirect addressing mode) onto the stack | 4 | - |
| 54 | `push [RC:RD]` | Pushes a location in the address space (full indirect addressing mode) onto the stack | 5 | - |
| 55 | `push [<full address>]` | Pushes a location in the address space (absolute addressing mode) onto the stack | 5 | - |
| 56 | `push <byte>` | Pushes a constant onto the stack | 2 | - |
| 57 | `push BP` | Pushes the value of BP onto the stack | 2 | - |
| 58 | `push DS` | Pushes the value of DS onto the stack | 2 | - |
| 59 | `pop RA` | Pops from the stack into RA | 1 | - |
| 5a | `pop RB` | Pops from the stack into RB | 1 | - |
| 5b | `pop RC` | Pops from the stack into RC | 1 | - |
| 5c | `pop RD` | Pops from the stack into RD | 1 | - |
| 5d | `pop [<data page address>]` | Pops from the stack into a location in the address space (data page addressing mode) | 3 | - |
| 5e | `pop [RD]` | Pops from the stack into a location in the address space (short indirect addressing mode) | 3 | - |
| 5f | `pop [RC:RD]` | Pops from the stack into a location in the address space (full indirect addressing mode) | 4 | - |
| 60 | `pop [<full address>]` | Pops from the stack into a location in the address space (absolute addressing mode) | 4 | - |
| 61 | `pop BP` | Pops from the stack into BP | 1 | - |
| 62 | `pop DS` | Pops from the stack into DS | 1 | - |
| 63 | `add RA,	RA` | Adds RA to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 64 | `add RA,	RB` | Adds RB to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 65 | `add RA,	RC` | Adds RC to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 66 | `add RA,	RD` | Adds RD to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 67 | `add RB,	RA` | Adds RA to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 68 | `add RB,	RB` | Adds RB to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 69 | `add RB,	RC` | Adds RC to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 6a | `add RB,	RD` | Adds RD to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 6b | `add RC,	RA` | Adds RA to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 6c | `add RC,	RB` | Adds RB to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 6d | `add RC,	RC` | Adds RC to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 6e | `add RC,	RD` | Adds RD to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 6f | `add RD,	RA` | Adds RA to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 70 | `add RD,	RB` | Adds RB to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 71 | `add RD,	RC` | Adds RC to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 72 | `add RD,	RD` | Adds RD to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 73 | `add RA,	<byte>` | Adds a constant to RA, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| 74 | `add RB,	<byte>` | Adds a constant to RB, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| 75 | `add RC,	<byte>` | Adds a constant to RC, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| 76 | `add RD,	<byte>` | Adds a constant to RD, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| 77 | `add SP,	<byte>` | Adds a constant to SP, stores the result back in SP, then updates the FLAGS accordingly | 2 | `NZC` |
| 78 | `add BP,	<byte>` | Adds a constant to BP, stores the result back in BP, then updates the FLAGS accordingly | 2 | `NZC` |
| 79 | `sub RA,	RA` | Subtracts RA from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 7a | `sub RA,	RB` | Subtracts RB from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 7b | `sub RA,	RC` | Subtracts RC from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 7c | `sub RA,	RD` | Subtracts RD from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 7d | `sub RB,	RA` | Subtracts RA from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 7e | `sub RB,	RB` | Subtracts RB from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 7f | `sub RB,	RC` | Subtracts RC from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 80 | `sub RB,	RD` | Subtracts RD from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 81 | `sub RC,	RA` | Subtracts RA from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 82 | `sub RC,	RB` | Subtracts RB from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 83 | `sub RC,	RC` | Subtracts RC from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 84 | `sub RC,	RD` | Subtracts RD from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 85 | `sub RD,	RA` | Subtracts RA from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 86 | `sub RD,	RB` | Subtracts RB from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 87 | `sub RD,	RC` | Subtracts RC from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 88 | `sub RD,	RD` | Subtracts RD from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 89 | `sub RA,	<byte>` | Subtracts a constant from RA, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| 8a | `sub RB,	<byte>` | Subtracts a constant from RB, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| 8b | `sub RC,	<byte>` | Subtracts a constant from RC, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| 8c | `sub RD,	<byte>` | Subtracts a constant from RD, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| 8d | `sub SP,	<byte>` | Subtracts a constant from SP, stores the result back in SP, then updates the FLAGS accordingly | 2 | `NZC` |
| 8e | `sub BP,	<byte>` | Subtracts a constant from BP, stores the result back in BP, then updates the FLAGS accordingly | 2 | `NZC` |
| 8f | `cmp RA,	RA` | Compares RA to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 90 | `cmp RA,	RB` | Compares RA to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 91 | `cmp RA,	RC` | Compares RA to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 92 | `cmp RA,	RD` | Compares RA to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 93 | `cmp RB,	RA` | Compares RB to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 94 | `cmp RB,	RB` | Compares RB to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 95 | `cmp RB,	RC` | Compares RB to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 96 | `cmp RB,	RD` | Compares RB to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 97 | `cmp RC,	RA` | Compares RC to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 98 | `cmp RC,	RB` | Compares RC to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 99 | `cmp RC,	RC` | Compares RC to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 9a | `cmp RC,	RD` | Compares RC to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 9b | `cmp RD,	RA` | Compares RD to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 9c | `cmp RD,	RB` | Compares RD to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 9d | `cmp RD,	RC` | Compares RD to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 9e | `cmp RD,	RD` | Compares RD to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 9f | `cmp RA,	<byte>` | Compares RA to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| a0 | `cmp RB,	<byte>` | Compares RB to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| a1 | `cmp RC,	<byte>` | Compares RC to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| a2 | `cmp RD,	<byte>` | Compares RD to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| a3 | `or RA,	RA` | Performs a bitwise OR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| a4 | `or RA,	RB` | Performs a bitwise OR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| a5 | `or RA,	RC` | Performs a bitwise OR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| a6 | `or RA,	RD` | Performs a bitwise OR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| a7 | `or RB,	RA` | Performs a bitwise OR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| a8 | `or RB,	RB` | Performs a bitwise OR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| a9 | `or RB,	RC` | Performs a bitwise OR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| aa | `or RB,	RD` | Performs a bitwise OR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| ab | `or RC,	RA` | Performs a bitwise OR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| ac | `or RC,	RB` | Performs a bitwise OR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| ad | `or RC,	RC` | Performs a bitwise OR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| ae | `or RC,	RD` | Performs a bitwise OR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| af | `or RD,	RA` | Performs a bitwise OR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| b0 | `or RD,	RB` | Performs a bitwise OR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| b1 | `or RD,	RC` | Performs a bitwise OR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| b2 | `or RD,	RD` | Performs a bitwise OR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| b3 | `or RA,	<byte>` | Performs a bitwise OR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 | - |
| b4 | `or RB,	<byte>` | Performs a bitwise OR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 | - |
| b5 | `or RC,	<byte>` | Performs a bitwise OR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 | - |
| b6 | `or RD,	<byte>` | Performs a bitwise OR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 | - |
| b7 | `and RA,	RA` | Performs a bitwise AND of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| b8 | `and RA,	RB` | Performs a bitwise AND of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| b9 | `and RA,	RC` | Performs a bitwise AND of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| ba | `and RA,	RD` | Performs a bitwise AND of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| bb | `and RB,	RA` | Performs a bitwise AND of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| bc | `and RB,	RB` | Performs a bitwise AND of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| bd | `and RB,	RC` | Performs a bitwise AND of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| be | `and RB,	RD` | Performs a bitwise AND of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| bf | `and RC,	RA` | Performs a bitwise AND of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| c0 | `and RC,	RB` | Performs a bitwise AND of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| c1 | `and RC,	RC` | Performs a bitwise AND of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| c2 | `and RC,	RD` | Performs a bitwise AND of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| c3 | `and RD,	RA` | Performs a bitwise AND of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| c4 | `and RD,	RB` | Performs a bitwise AND of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| c5 | `and RD,	RC` | Performs a bitwise AND of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| c6 | `and RD,	RD` | Performs a bitwise AND of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| c7 | `and RA,	<byte>` | Performs a bitwise AND of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| c8 | `and RB,	<byte>` | Performs a bitwise AND of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| c9 | `and RC,	<byte>` | Performs a bitwise AND of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| ca | `and RD,	<byte>` | Performs a bitwise AND of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| cb | `xor RA,	RA` | Performs a bitwise XOR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| cc | `xor RA,	RB` | Performs a bitwise XOR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| cd | `xor RA,	RC` | Performs a bitwise XOR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| ce | `xor RA,	RD` | Performs a bitwise XOR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| cf | `xor RB,	RA` | Performs a bitwise XOR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| d0 | `xor RB,	RB` | Performs a bitwise XOR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| d1 | `xor RB,	RC` | Performs a bitwise XOR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| d2 | `xor RB,	RD` | Performs a bitwise XOR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| d3 | `xor RC,	RA` | Performs a bitwise XOR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| d4 | `xor RC,	RB` | Performs a bitwise XOR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| d5 | `xor RC,	RC` | Performs a bitwise XOR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| d6 | `xor RC,	RD` | Performs a bitwise XOR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| d7 | `xor RD,	RA` | Performs a bitwise XOR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| d8 | `xor RD,	RB` | Performs a bitwise XOR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| d9 | `xor RD,	RC` | Performs a bitwise XOR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| da | `xor RD,	RD` | Performs a bitwise XOR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| db | `xor RA,	<byte>` | Performs a bitwise XOR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| dc | `xor RB,	<byte>` | Performs a bitwise XOR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| dd | `xor RC,	<byte>` | Performs a bitwise XOR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| de | `xor RD,	<byte>` | Performs a bitwise XOR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| df | `not RA` | Performs a bitwise NOT of RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| e0 | `not RB` | Performs a bitwise NOT of RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| e1 | `not RC` | Performs a bitwise NOT of RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| e2 | `not RD` | Performs a bitwise NOT of RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| e3 | `inc RA` | Increments RA by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e4 | `inc RB` | Increments RB by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e5 | `inc RC` | Increments RC by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e6 | `inc RD` | Increments RD by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e7 | `dec RA` | Decrements RA by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e8 | `dec RB` | Decrements RB by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| e9 | `dec RC` | Decrements RC by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| ea | `dec RD` | Decrements RD by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| eb | `shl RA` | Performs a left bitwise shift of RA by 1 bit | 1 | - |
| ec | `shr RA` | Performs a right bitwise shift of RA by 1 bit | 1 | - |
| ed | `jc <byte>` | Jumps to the given short address (same code segment) if the carry flag is set | 3 | - |
| ee | `jn <byte>` | Jumps to the given short address (same code segment) if the negative flag is set | 3 | - |
| ef | `jz <byte>` | Jumps to the given short address (same code segment) if the zero flag is set | 3 | - |
| f0 | `goto <byte>` | Jumps to the given short address (same code segment) | 3 | - |
| f1 | `ljc <dcst>` | Jumps to the given long address if the carry flag is set | 4 | - |
| f2 | `ljn <dcst>` | Jumps to the given long address if the negative flag is set | 4 | - |
| f3 | `ljz <dcst>` | Jumps to the given long address if the zero flag is set | 4 | - |
| f4 | `lgoto <dcst>` | Jumps to the given long address | 4 | - |
| f5 | `lgoto RC:RD` | Jumps to the long address specified by RC:RD | 4 | - |
| f6 | `lcall <dcst>` | Calls the given long address | 6 | - |
| f7 | `lcall RC:RD` | Calls the long address specified by RC:RD | 6 | - |
| f8 | `ret ` | Returns from a call | 4 | - |
| f9 | `ret <byte>` | Returns from a call and decrements the stack pointer by a constant | 6 | - |
| fa | `iret ` | Returns from an interrupt | 5 | - |
| fb | `cid ` | Clears the interrupt disable flag | 1 | `I` |
| fc | `sid ` | Sets the interrupt disable flag | 1 | `I` |
| fd | `clc ` | Clears the carry flag | 1 | `C` |
| fe | `sec ` | Sets the carry flag | 1 | `C` |
