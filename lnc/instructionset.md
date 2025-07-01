

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
| 0f | `mov DS,	RD` | Moves DS to RD | 1 | - |
| 10 | `mov RA,	RB` | Moves RA to RB | 1 | - |
| 11 | `mov RA,	RC` | Moves RA to RC | 1 | - |
| 12 | `mov RA,	RD` | Moves RA to RD | 1 | - |
| 13 | `mov RB,	RA` | Moves RB to RA | 1 | - |
| 14 | `mov RB,	RC` | Moves RB to RC | 1 | - |
| 15 | `mov RB,	RD` | Moves RB to RD | 1 | - |
| 16 | `mov RC,	RA` | Moves RC to RA | 1 | - |
| 17 | `mov RC,	RB` | Moves RC to RB | 1 | - |
| 18 | `mov RC,	RD` | Moves RC to RD | 1 | - |
| 19 | `mov RD,	RA` | Moves RD to RA | 1 | - |
| 1a | `mov RD,	RB` | Moves RD to RB | 1 | - |
| 1b | `mov RD,	RC` | Moves RD to RC | 1 | - |
| 1c | `mov RD,	SS` | Moves RD to SS | 1 | - |
| 1d | `mov RD,	SP` | Moves RD to SP | 1 | - |
| 1e | `mov RD,	DS` | Moves RD to DS | 1 | - |
| 1f | `mov [BP + <offset>],	RA` | Copies a from a location in the address space (stack frame offset addressing mode) to RA | 4 | - |
| 20 | `mov [BP + <offset>],	RB` | Copies a from a location in the address space (stack frame offset addressing mode) to RB | 4 | - |
| 21 | `mov [BP + <offset>],	RC` | Copies a from a location in the address space (stack frame offset addressing mode) to RC | 4 | - |
| 22 | `mov [BP + <offset>],	RD` | Copies a from a location in the address space (stack frame offset addressing mode) to RD | 4 | - |
| 23 | `mov RA,	[BP + <offset>]` | Copies RA to a location in the address space (stack frame offset addressing mode) | 4 | - |
| 24 | `mov RB,	[BP + <offset>]` | Copies RB to a location in the address space (stack frame offset addressing mode) | 4 | - |
| 25 | `mov RC,	[BP + <offset>]` | Copies RC to a location in the address space (stack frame offset addressing mode) | 4 | - |
| 26 | `mov RD,	[BP + <offset>]` | Copies RD to a location in the address space (stack frame offset addressing mode) | 4 | - |
| 27 | `mov RA,	[<data page address>]` | Copies RA to a location in the address space (data page addressing mode) | 2 | - |
| 28 | `mov RB,	[<data page address>]` | Copies RB to a location in the address space (data page addressing mode) | 2 | - |
| 29 | `mov RC,	[<data page address>]` | Copies RC to a location in the address space (data page addressing mode) | 2 | - |
| 2a | `mov RD,	[<data page address>]` | Copies RD to a location in the address space (data page addressing mode) | 2 | - |
| 2b | `mov [<data page address>],	RA` | Copies from location in the address space (data page addressing mode) to RA | 2 | - |
| 2c | `mov [<data page address>],	RB` | Copies from location in the address space (data page addressing mode) to RB | 2 | - |
| 2d | `mov [<data page address>],	RC` | Copies from location in the address space (data page addressing mode) to RC | 2 | - |
| 2e | `mov [<data page address>],	RD` | Copies from location in the address space (data page addressing mode) to RD | 2 | - |
| 2f | `mov <byte>,	[<data page address>]` | Copies a constant to a location in the address space (data page addressing mode) | 3 | - |
| 30 | `mov RA,	[RC:RD]` | Copies RA to a location in the address space (full indirect addressing mode) | 3 | - |
| 31 | `mov RB,	[RC:RD]` | Copies RB to a location in the address space (full indirect addressing mode) | 3 | - |
| 32 | `mov RC,	[RC:RD]` | Copies RC to a location in the address space (full indirect addressing mode) | 3 | - |
| 33 | `mov RD,	[RC:RD]` | Copies RD to a location in the address space (full indirect addressing mode) | 3 | - |
| 34 | `mov [RC:RD],	RA` | Copies from location in the address space (full indirect addressing mode) to RA | 3 | - |
| 35 | `mov [RC:RD],	RB` | Copies from location in the address space (full indirect addressing mode) to RB | 3 | - |
| 36 | `mov [RC:RD],	RC` | Copies from location in the address space (full indirect addressing mode) to RC | 3 | - |
| 37 | `mov [RC:RD],	RD` | Copies from location in the address space (full indirect addressing mode) to RD | 3 | - |
| 38 | `mov <byte>,	[RC:RD]` | Copies a constant to a location in the address space (full indirect addressing mode) | 4 | - |
| 39 | `mov RA,	[<full address>]` | Copies RA to a location in the address space (absolute addressing mode) | 3 | - |
| 3a | `mov RB,	[<full address>]` | Copies RB to a location in the address space (absolute addressing mode) | 3 | - |
| 3b | `mov RC,	[<full address>]` | Copies RC to a location in the address space (absolute addressing mode) | 3 | - |
| 3c | `mov RD,	[<full address>]` | Copies RD to a location in the address space (absolute addressing mode) | 3 | - |
| 3d | `mov [<full address>],	RA` | Copies from location in the address space (absolute addressing mode) to RA | 3 | - |
| 3e | `mov [<full address>],	RB` | Copies from location in the address space (absolute addressing mode) to RB | 3 | - |
| 3f | `mov [<full address>],	RC` | Copies from location in the address space (absolute addressing mode) to RC | 3 | - |
| 40 | `mov [<full address>],	RD` | Copies from location in the address space (absolute addressing mode) to RD | 3 | - |
| 41 | `mov <byte>,	[<full address>]` | Copies a constant to a location in the address space (absolute addressing mode) | 4 | - |
| 42 | `mov [<data page address>],	[<data page address>]` | Copies a location in the address space (data page addressing mode) to another location in the address space (data page addressing mode) | 4 | - |
| 43 | `push RA` | Pushes RA onto the stack | 2 | - |
| 44 | `push RB` | Pushes RB onto the stack | 2 | - |
| 45 | `push RC` | Pushes RC onto the stack | 2 | - |
| 46 | `push RD` | Pushes RD onto the stack | 2 | - |
| 47 | `push [<data page address>]` | Pushes a location in the address space (data page addressing mode) onto the stack | 4 | - |
| 48 | `push [RC:RD]` | Pushes a location in the address space (full indirect addressing mode) onto the stack | 5 | - |
| 49 | `push [<full address>]` | Pushes a location in the address space (absolute addressing mode) onto the stack | 5 | - |
| 4a | `push <byte>` | Pushes a constant onto the stack | 2 | - |
| 4b | `push BP` | Pushes the value of BP onto the stack | 2 | - |
| 4c | `pop RA` | Pops from the stack into RA | 1 | - |
| 4d | `pop RB` | Pops from the stack into RB | 1 | - |
| 4e | `pop RC` | Pops from the stack into RC | 1 | - |
| 4f | `pop RD` | Pops from the stack into RD | 1 | - |
| 50 | `pop [<data page address>]` | Pops from the stack into a location in the address space (data page addressing mode) | 3 | - |
| 51 | `pop [RC:RD]` | Pops from the stack into a location in the address space (full indirect addressing mode) | 4 | - |
| 52 | `pop [<full address>]` | Pops from the stack into a location in the address space (absolute addressing mode) | 4 | - |
| 53 | `pop BP` | Pops from the stack into BP | 1 | - |
| 54 | `add RA,	RA` | Adds RA to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 55 | `add RA,	RB` | Adds RB to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 56 | `add RA,	RC` | Adds RC to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 57 | `add RA,	RD` | Adds RD to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 58 | `add RB,	RA` | Adds RA to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 59 | `add RB,	RB` | Adds RB to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 5a | `add RB,	RC` | Adds RC to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 5b | `add RB,	RD` | Adds RD to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 5c | `add RC,	RA` | Adds RA to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 5d | `add RC,	RB` | Adds RB to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 5e | `add RC,	RC` | Adds RC to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 5f | `add RC,	RD` | Adds RD to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 60 | `add RD,	RA` | Adds RA to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 61 | `add RD,	RB` | Adds RB to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 62 | `add RD,	RC` | Adds RC to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 63 | `add RD,	RD` | Adds RD to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 64 | `add RA,	<byte>` | Adds a constant to RA, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| 65 | `add RB,	<byte>` | Adds a constant to RB, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| 66 | `add RC,	<byte>` | Adds a constant to RC, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| 67 | `add RD,	<byte>` | Adds a constant to RD, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| 68 | `add SP,	<byte>` | Adds a constant to SP, stores the result back in SP, then updates the FLAGS accordingly | 2 | `NZC` |
| 69 | `add BP,	<byte>` | Adds a constant to BP, stores the result back in BP, then updates the FLAGS accordingly | 2 | `NZC` |
| 6a | `sub RA,	RA` | Subtracts RA from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 6b | `sub RA,	RB` | Subtracts RB from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 6c | `sub RA,	RC` | Subtracts RC from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 6d | `sub RA,	RD` | Subtracts RD from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 6e | `sub RB,	RA` | Subtracts RA from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 6f | `sub RB,	RB` | Subtracts RB from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 70 | `sub RB,	RC` | Subtracts RC from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 71 | `sub RB,	RD` | Subtracts RD from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 72 | `sub RC,	RA` | Subtracts RA from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 73 | `sub RC,	RB` | Subtracts RB from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 74 | `sub RC,	RC` | Subtracts RC from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 75 | `sub RC,	RD` | Subtracts RD from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 76 | `sub RD,	RA` | Subtracts RA from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 77 | `sub RD,	RB` | Subtracts RB from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 78 | `sub RD,	RC` | Subtracts RC from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 79 | `sub RD,	RD` | Subtracts RD from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 7a | `sub RA,	<byte>` | Subtracts a constant from RA, stores the result back in RA, then updates the FLAGS accordingly | 2 | `NZC` |
| 7b | `sub RB,	<byte>` | Subtracts a constant from RB, stores the result back in RB, then updates the FLAGS accordingly | 2 | `NZC` |
| 7c | `sub RC,	<byte>` | Subtracts a constant from RC, stores the result back in RC, then updates the FLAGS accordingly | 2 | `NZC` |
| 7d | `sub RD,	<byte>` | Subtracts a constant from RD, stores the result back in RD, then updates the FLAGS accordingly | 2 | `NZC` |
| 7e | `sub SP,	<byte>` | Subtracts a constant from SP, stores the result back in SP, then updates the FLAGS accordingly | 2 | `NZC` |
| 7f | `sub BP,	<byte>` | Subtracts a constant from BP, stores the result back in BP, then updates the FLAGS accordingly | 2 | `NZC` |
| 80 | `cmp RA,	RA` | Compares RA to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 81 | `cmp RA,	RB` | Compares RA to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 82 | `cmp RA,	RC` | Compares RA to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 83 | `cmp RA,	RD` | Compares RA to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 84 | `cmp RB,	RA` | Compares RB to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 85 | `cmp RB,	RB` | Compares RB to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 86 | `cmp RB,	RC` | Compares RB to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 87 | `cmp RB,	RD` | Compares RB to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 88 | `cmp RC,	RA` | Compares RC to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 89 | `cmp RC,	RB` | Compares RC to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 8a | `cmp RC,	RC` | Compares RC to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 8b | `cmp RC,	RD` | Compares RC to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 8c | `cmp RD,	RA` | Compares RD to RA, then updates the FLAGS accordingly | 1 | `NZC` |
| 8d | `cmp RD,	RB` | Compares RD to RB, then updates the FLAGS accordingly | 1 | `NZC` |
| 8e | `cmp RD,	RC` | Compares RD to RC, then updates the FLAGS accordingly | 1 | `NZC` |
| 8f | `cmp RD,	RD` | Compares RD to RD, then updates the FLAGS accordingly | 1 | `NZC` |
| 90 | `cmp RA,	<byte>` | Compares RA to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 91 | `cmp RB,	<byte>` | Compares RB to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 92 | `cmp RC,	<byte>` | Compares RC to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 93 | `cmp RD,	<byte>` | Compares RD to a constant, then updates the FLAGS accordingly | 2 | `NZC` |
| 94 | `or RA,	RA` | Performs a bitwise OR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 95 | `or RA,	RB` | Performs a bitwise OR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 96 | `or RA,	RC` | Performs a bitwise OR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 97 | `or RA,	RD` | Performs a bitwise OR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 | - |
| 98 | `or RB,	RA` | Performs a bitwise OR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 99 | `or RB,	RB` | Performs a bitwise OR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 9a | `or RB,	RC` | Performs a bitwise OR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 9b | `or RB,	RD` | Performs a bitwise OR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 | - |
| 9c | `or RC,	RA` | Performs a bitwise OR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 9d | `or RC,	RB` | Performs a bitwise OR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 9e | `or RC,	RC` | Performs a bitwise OR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| 9f | `or RC,	RD` | Performs a bitwise OR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 | - |
| a0 | `or RD,	RA` | Performs a bitwise OR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| a1 | `or RD,	RB` | Performs a bitwise OR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| a2 | `or RD,	RC` | Performs a bitwise OR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| a3 | `or RD,	RD` | Performs a bitwise OR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | - |
| a4 | `or RA,	<byte>` | Performs a bitwise OR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 | - |
| a5 | `or RB,	<byte>` | Performs a bitwise OR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 | - |
| a6 | `or RC,	<byte>` | Performs a bitwise OR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 | - |
| a7 | `or RD,	<byte>` | Performs a bitwise OR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 | - |
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
| d0 | `not RA` | Performs a bitwise NOT of RA, stores the result back in RA, then updates the FLAGS accordingly | 1 | `NZC` |
| d1 | `not RB` | Performs a bitwise NOT of RB, stores the result back in RB, then updates the FLAGS accordingly | 1 | `NZC` |
| d2 | `not RC` | Performs a bitwise NOT of RC, stores the result back in RC, then updates the FLAGS accordingly | 1 | `NZC` |
| d3 | `not RD` | Performs a bitwise NOT of RD, stores the result back in RD, then updates the FLAGS accordingly | 1 | `NZC` |
| d4 | `inc RA` | Increments RA by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| d5 | `inc RB` | Increments RB by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| d6 | `inc RC` | Increments RC by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| d7 | `inc RD` | Increments RD by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| d8 | `dec RA` | Decrements RA by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| d9 | `dec RB` | Decrements RB by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| da | `dec RC` | Decrements RC by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| db | `dec RD` | Decrements RD by 1, then updates the FLAGS accordingly | 1 | `NZC` |
| dc | `shl RA` | Performs a left bitwise shift of RA by 1 bit | 1 | - |
| dd | `shr RA` | Performs a right bitwise shift of RA by 1 bit | 1 | - |
| de | `swap RA,	RB` | Swaps the values of RA and RB | 3 | - |
| df | `swap RA,	RC` | Swaps the values of RA and RC | 3 | - |
| e0 | `swap RA,	RD` | Swaps the values of RA and RD | 3 | - |
| e1 | `swap RB,	RC` | Swaps the values of RB and RC | 3 | - |
| e2 | `swap RB,	RD` | Swaps the values of RB and RD | 3 | - |
| e3 | `swap RC,	RD` | Swaps the values of RC and RD | 3 | - |
| e4 | `jc <byte>` | Jumps to the given short address (same code segment) if the carry flag is set | 3 | - |
| e5 | `jn <byte>` | Jumps to the given short address (same code segment) if the negative flag is set | 3 | - |
| e6 | `jz <byte>` | Jumps to the given short address (same code segment) if the zero flag is set | 3 | - |
| e7 | `goto <byte>` | Jumps to the given short address (same code segment) | 3 | - |
| e8 | `ljc <dcst>` | Jumps to the given long address if the carry flag is set | 4 | - |
| e9 | `ljn <dcst>` | Jumps to the given long address if the negative flag is set | 4 | - |
| ea | `ljz <dcst>` | Jumps to the given long address if the zero flag is set | 4 | - |
| eb | `lgoto <dcst>` | Jumps to the given long address | 4 | - |
| ec | `lgoto RC:RD` | Jumps to the long address specified by RC:RD | 4 | - |
| ed | `lcall <dcst>` | Calls the given long address | 6 | - |
| ee | `lcall RC:RD` | Calls the long address specified by RC:RD | 6 | - |
| ef | `ret ` | Returns from a call | 4 | - |
| f0 | `iret ` | Returns from an interrupt | 5 | - |
| f1 | `cid ` | Clears the interrupt disable flag | 1 | `I` |
| f2 | `sid ` | Sets the interrupt disable flag | 1 | `I` |
| f3 | `clc ` | Clears the carry flag | 1 | `C` |
| f4 | `sec ` | Sets the carry flag | 1 | `C` |
