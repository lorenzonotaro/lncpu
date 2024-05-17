

### lnasm instruction set


| Opcode | Syntax | Description | Clock cycles | Flags Affected |
|--------|--------|-------------|--------------|----------------|
| 00 | `nop ` | No operation | 1 |  |
| 01 | `hlt ` | Halts the CPU | 1 |  |
| 02 | `brk ` | Pushes CS:PC and FLAGS, then calls the interrupt vector | 7 |  |
| 03 | `mov <byte>,	RA` | Moves a constant to RA | 2 |  |
| 04 | `mov <byte>,	RB` | Moves a constant to RB | 2 |  |
| 05 | `mov <byte>,	RC` | Moves a constant to RC | 2 |  |
| 06 | `mov <byte>,	RD` | Moves a constant to RD | 2 |  |
| 07 | `mov <byte>,	SP` | Moves a constant to SP | 2 |  |
| 08 | `mov <byte>,	SS` | Moves a constant to SS | 2 |  |
| 09 | `mov SS,	RD` | Moves SS to RD | 1 |  |
| 0a | `mov SP,	RD` | Moves SP to RD | 1 |  |
| 0b | `mov RA,	RB` | Moves RA to RB | 1 |  |
| 0c | `mov RA,	RC` | Moves RA to RC | 1 |  |
| 0d | `mov RA,	RD` | Moves RA to RD | 1 |  |
| 0e | `mov RB,	RA` | Moves RB to RA | 1 |  |
| 0f | `mov RB,	RC` | Moves RB to RC | 1 |  |
| 10 | `mov RB,	RD` | Moves RB to RD | 1 |  |
| 11 | `mov RC,	RA` | Moves RC to RA | 1 |  |
| 12 | `mov RC,	RB` | Moves RC to RB | 1 |  |
| 13 | `mov RC,	RD` | Moves RC to RD | 1 |  |
| 14 | `mov RD,	RA` | Moves RD to RA | 1 |  |
| 15 | `mov RD,	RB` | Moves RD to RB | 1 |  |
| 16 | `mov RD,	RC` | Moves RD to RC | 1 |  |
| 17 | `mov RD,	SS` | Moves RD to SS | 1 |  |
| 18 | `mov RD,	SP` | Moves RD to SP | 1 |  |
| 19 | `mov RA,	[<page0 address>]` | Copies RA to a location in the address space (zero page addressing mode) | 2 |  |
| 1a | `mov RB,	[<page0 address>]` | Copies RB to a location in the address space (zero page addressing mode) | 2 |  |
| 1b | `mov RC,	[<page0 address>]` | Copies RC to a location in the address space (zero page addressing mode) | 2 |  |
| 1c | `mov RD,	[<page0 address>]` | Copies RD to a location in the address space (zero page addressing mode) | 2 |  |
| 1d | `mov [<page0 address>],	RA` | Copies from location in the address space (zero page addressing mode) to RA | 2 |  |
| 1e | `mov [<page0 address>],	RB` | Copies from location in the address space (zero page addressing mode) to RB | 2 |  |
| 1f | `mov [<page0 address>],	RC` | Copies from location in the address space (zero page addressing mode) to RC | 2 |  |
| 20 | `mov [<page0 address>],	RD` | Copies from location in the address space (zero page addressing mode) to RD | 2 |  |
| 21 | `mov <byte>,	[<page0 address>]` | Copies a constant to a location in the address space (zero page addressing mode) | 3 |  |
| 22 | `mov RA,	[RC:RD]` | Copies RA to a location in the address space (full indirect addressing mode) | 3 |  |
| 23 | `mov RB,	[RC:RD]` | Copies RB to a location in the address space (full indirect addressing mode) | 3 |  |
| 24 | `mov RC,	[RC:RD]` | Copies RC to a location in the address space (full indirect addressing mode) | 3 |  |
| 25 | `mov RD,	[RC:RD]` | Copies RD to a location in the address space (full indirect addressing mode) | 3 |  |
| 26 | `mov [RC:RD],	RA` | Copies from location in the address space (full indirect addressing mode) to RA | 3 |  |
| 27 | `mov [RC:RD],	RB` | Copies from location in the address space (full indirect addressing mode) to RB | 3 |  |
| 28 | `mov [RC:RD],	RC` | Copies from location in the address space (full indirect addressing mode) to RC | 3 |  |
| 29 | `mov [RC:RD],	RD` | Copies from location in the address space (full indirect addressing mode) to RD | 3 |  |
| 2a | `mov <byte>,	[RC:RD]` | Copies a constant to a location in the address space (full indirect addressing mode) | 4 |  |
| 2b | `mov RA,	[<full address>]` | Copies RA to a location in the address space (absolute addressing mode) | 3 |  |
| 2c | `mov RB,	[<full address>]` | Copies RB to a location in the address space (absolute addressing mode) | 3 |  |
| 2d | `mov RC,	[<full address>]` | Copies RC to a location in the address space (absolute addressing mode) | 3 |  |
| 2e | `mov RD,	[<full address>]` | Copies RD to a location in the address space (absolute addressing mode) | 3 |  |
| 2f | `mov [<full address>],	RA` | Copies from location in the address space (absolute addressing mode) to RA | 3 |  |
| 30 | `mov [<full address>],	RB` | Copies from location in the address space (absolute addressing mode) to RB | 3 |  |
| 31 | `mov [<full address>],	RC` | Copies from location in the address space (absolute addressing mode) to RC | 3 |  |
| 32 | `mov [<full address>],	RD` | Copies from location in the address space (absolute addressing mode) to RD | 3 |  |
| 33 | `mov <byte>,	[<full address>]` | Copies a constant to a location in the address space (absolute addressing mode) | 4 |  |
| 34 | `mov [<page0 address>],	[<page0 address>]` | Copies a location in the address space (zero page addressing mode) to another location in the address space (zero page addressing mode) | 4 |  |
| 35 | `push RA` | Pushes RA onto the stack | 2 |  |
| 36 | `push RB` | Pushes RB onto the stack | 2 |  |
| 37 | `push RC` | Pushes RC onto the stack | 2 |  |
| 38 | `push RD` | Pushes RD onto the stack | 2 |  |
| 39 | `push [<page0 address>]` | Pushes a location in the address space (zero page addressing mode) onto the stack | 4 |  |
| 3a | `push [RC:RD]` | Pushes a location in the address space (full indirect addressing mode) onto the stack | 5 |  |
| 3b | `push [<full address>]` | Pushes a location in the address space (absolute addressing mode) onto the stack | 5 |  |
| 3c | `push <byte>` | Pushes a constant onto the stack | 2 |  |
| 3d | `pop RA` | Pops from the stack into RA | 1 |  |
| 3e | `pop RB` | Pops from the stack into RB | 1 |  |
| 3f | `pop RC` | Pops from the stack into RC | 1 |  |
| 40 | `pop RD` | Pops from the stack into RD | 1 |  |
| 41 | `pop [<page0 address>]` | Pops from the stack into a location in the address space (zero page addressing mode) | 3 |  |
| 42 | `pop [RC:RD]` | Pops from the stack into a location in the address space (full indirect addressing mode) | 4 |  |
| 43 | `pop [<full address>]` | Pops from the stack into a location in the address space (absolute addressing mode) | 4 |  |
| 44 | `add RA,	RA` | Adds RA to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 45 | `add RA,	RB` | Adds RB to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 46 | `add RA,	RC` | Adds RC to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 47 | `add RA,	RD` | Adds RD to RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 48 | `add RB,	RA` | Adds RA to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 49 | `add RB,	RB` | Adds RB to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 4a | `add RB,	RC` | Adds RC to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 4b | `add RB,	RD` | Adds RD to RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 4c | `add RC,	RA` | Adds RA to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 4d | `add RC,	RB` | Adds RB to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 4e | `add RC,	RC` | Adds RC to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 4f | `add RC,	RD` | Adds RD to RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 50 | `add RD,	RA` | Adds RA to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 51 | `add RD,	RB` | Adds RB to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 52 | `add RD,	RC` | Adds RC to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 53 | `add RD,	RD` | Adds RD to RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 54 | `add RA,	<byte>` | Adds a constant to RA, stores the result back in RA, then updates the FLAGS accordingly | 2 |  |
| 55 | `add RB,	<byte>` | Adds a constant to RB, stores the result back in RB, then updates the FLAGS accordingly | 2 |  |
| 56 | `add RC,	<byte>` | Adds a constant to RC, stores the result back in RC, then updates the FLAGS accordingly | 2 |  |
| 57 | `add RD,	<byte>` | Adds a constant to RD, stores the result back in RD, then updates the FLAGS accordingly | 2 |  |
| 58 | `sub RA,	RA` | Subtracts RA from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 59 | `sub RA,	RB` | Subtracts RB from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 5a | `sub RA,	RC` | Subtracts RC from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 5b | `sub RA,	RD` | Subtracts RD from RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 5c | `sub RB,	RA` | Subtracts RA from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 5d | `sub RB,	RB` | Subtracts RB from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 5e | `sub RB,	RC` | Subtracts RC from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 5f | `sub RB,	RD` | Subtracts RD from RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 60 | `sub RC,	RA` | Subtracts RA from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 61 | `sub RC,	RB` | Subtracts RB from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 62 | `sub RC,	RC` | Subtracts RC from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 63 | `sub RC,	RD` | Subtracts RD from RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 64 | `sub RD,	RA` | Subtracts RA from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 65 | `sub RD,	RB` | Subtracts RB from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 66 | `sub RD,	RC` | Subtracts RC from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 67 | `sub RD,	RD` | Subtracts RD from RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 68 | `sub RA,	<byte>` | Subtracts a constant from RA, stores the result back in RA, then updates the FLAGS accordingly | 2 |  |
| 69 | `sub RB,	<byte>` | Subtracts a constant from RB, stores the result back in RB, then updates the FLAGS accordingly | 2 |  |
| 6a | `sub RC,	<byte>` | Subtracts a constant from RC, stores the result back in RC, then updates the FLAGS accordingly | 2 |  |
| 6b | `sub RD,	<byte>` | Subtracts a constant from RD, stores the result back in RD, then updates the FLAGS accordingly | 2 |  |
| 6c | `cmp RA,	RA` | Compares RA to RA, then updates the FLAGS accordingly | 1 |  |
| 6d | `cmp RA,	RB` | Compares RA to RB, then updates the FLAGS accordingly | 1 |  |
| 6e | `cmp RA,	RC` | Compares RA to RC, then updates the FLAGS accordingly | 1 |  |
| 6f | `cmp RA,	RD` | Compares RA to RD, then updates the FLAGS accordingly | 1 |  |
| 70 | `cmp RB,	RA` | Compares RB to RA, then updates the FLAGS accordingly | 1 |  |
| 71 | `cmp RB,	RB` | Compares RB to RB, then updates the FLAGS accordingly | 1 |  |
| 72 | `cmp RB,	RC` | Compares RB to RC, then updates the FLAGS accordingly | 1 |  |
| 73 | `cmp RB,	RD` | Compares RB to RD, then updates the FLAGS accordingly | 1 |  |
| 74 | `cmp RC,	RA` | Compares RC to RA, then updates the FLAGS accordingly | 1 |  |
| 75 | `cmp RC,	RB` | Compares RC to RB, then updates the FLAGS accordingly | 1 |  |
| 76 | `cmp RC,	RC` | Compares RC to RC, then updates the FLAGS accordingly | 1 |  |
| 77 | `cmp RC,	RD` | Compares RC to RD, then updates the FLAGS accordingly | 1 |  |
| 78 | `cmp RD,	RA` | Compares RD to RA, then updates the FLAGS accordingly | 1 |  |
| 79 | `cmp RD,	RB` | Compares RD to RB, then updates the FLAGS accordingly | 1 |  |
| 7a | `cmp RD,	RC` | Compares RD to RC, then updates the FLAGS accordingly | 1 |  |
| 7b | `cmp RD,	RD` | Compares RD to RD, then updates the FLAGS accordingly | 1 |  |
| 7c | `cmp RA,	<byte>` | Compares RA to a constant, then updates the FLAGS accordingly | 2 |  |
| 7d | `cmp RB,	<byte>` | Compares RB to a constant, then updates the FLAGS accordingly | 2 |  |
| 7e | `cmp RC,	<byte>` | Compares RC to a constant, then updates the FLAGS accordingly | 2 |  |
| 7f | `cmp RD,	<byte>` | Compares RD to a constant, then updates the FLAGS accordingly | 2 |  |
| 80 | `or RA,	RA` | Performs a bitwise OR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 81 | `or RA,	RB` | Performs a bitwise OR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 82 | `or RA,	RC` | Performs a bitwise OR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 83 | `or RA,	RD` | Performs a bitwise OR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 84 | `or RB,	RA` | Performs a bitwise OR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 85 | `or RB,	RB` | Performs a bitwise OR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 86 | `or RB,	RC` | Performs a bitwise OR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 87 | `or RB,	RD` | Performs a bitwise OR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 88 | `or RC,	RA` | Performs a bitwise OR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 89 | `or RC,	RB` | Performs a bitwise OR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 8a | `or RC,	RC` | Performs a bitwise OR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 8b | `or RC,	RD` | Performs a bitwise OR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 8c | `or RD,	RA` | Performs a bitwise OR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 8d | `or RD,	RB` | Performs a bitwise OR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 8e | `or RD,	RC` | Performs a bitwise OR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 8f | `or RD,	RD` | Performs a bitwise OR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| 90 | `or RA,	<byte>` | Performs a bitwise OR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 |  |
| 91 | `or RB,	<byte>` | Performs a bitwise OR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 |  |
| 92 | `or RC,	<byte>` | Performs a bitwise OR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 |  |
| 93 | `or RD,	<byte>` | Performs a bitwise OR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 |  |
| 94 | `and RA,	RA` | Performs a bitwise AND of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 95 | `and RA,	RB` | Performs a bitwise AND of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 96 | `and RA,	RC` | Performs a bitwise AND of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 97 | `and RA,	RD` | Performs a bitwise AND of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| 98 | `and RB,	RA` | Performs a bitwise AND of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 99 | `and RB,	RB` | Performs a bitwise AND of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 9a | `and RB,	RC` | Performs a bitwise AND of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 9b | `and RB,	RD` | Performs a bitwise AND of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| 9c | `and RC,	RA` | Performs a bitwise AND of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 9d | `and RC,	RB` | Performs a bitwise AND of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 9e | `and RC,	RC` | Performs a bitwise AND of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| 9f | `and RC,	RD` | Performs a bitwise AND of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| a0 | `and RD,	RA` | Performs a bitwise AND of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| a1 | `and RD,	RB` | Performs a bitwise AND of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| a2 | `and RD,	RC` | Performs a bitwise AND of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| a3 | `and RD,	RD` | Performs a bitwise AND of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| a4 | `and RA,	<byte>` | Performs a bitwise AND of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 |  |
| a5 | `and RB,	<byte>` | Performs a bitwise AND of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 |  |
| a6 | `and RC,	<byte>` | Performs a bitwise AND of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 |  |
| a7 | `and RD,	<byte>` | Performs a bitwise AND of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 |  |
| a8 | `xor RA,	RA` | Performs a bitwise XOR of RA and RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| a9 | `xor RA,	RB` | Performs a bitwise XOR of RA and RB, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| aa | `xor RA,	RC` | Performs a bitwise XOR of RA and RC, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| ab | `xor RA,	RD` | Performs a bitwise XOR of RA and RD, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| ac | `xor RB,	RA` | Performs a bitwise XOR of RB and RA, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| ad | `xor RB,	RB` | Performs a bitwise XOR of RB and RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| ae | `xor RB,	RC` | Performs a bitwise XOR of RB and RC, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| af | `xor RB,	RD` | Performs a bitwise XOR of RB and RD, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| b0 | `xor RC,	RA` | Performs a bitwise XOR of RC and RA, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| b1 | `xor RC,	RB` | Performs a bitwise XOR of RC and RB, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| b2 | `xor RC,	RC` | Performs a bitwise XOR of RC and RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| b3 | `xor RC,	RD` | Performs a bitwise XOR of RC and RD, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| b4 | `xor RD,	RA` | Performs a bitwise XOR of RD and RA, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| b5 | `xor RD,	RB` | Performs a bitwise XOR of RD and RB, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| b6 | `xor RD,	RC` | Performs a bitwise XOR of RD and RC, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| b7 | `xor RD,	RD` | Performs a bitwise XOR of RD and RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| b8 | `xor RA,	<byte>` | Performs a bitwise XOR of RA and a constant, stores the result back in RA, then updates the FLAGS accordingly | 2 |  |
| b9 | `xor RB,	<byte>` | Performs a bitwise XOR of RB and a constant, stores the result back in RB, then updates the FLAGS accordingly | 2 |  |
| ba | `xor RC,	<byte>` | Performs a bitwise XOR of RC and a constant, stores the result back in RC, then updates the FLAGS accordingly | 2 |  |
| bb | `xor RD,	<byte>` | Performs a bitwise XOR of RD and a constant, stores the result back in RD, then updates the FLAGS accordingly | 2 |  |
| bc | `not RA` | Performs a bitwise NOT of RA, stores the result back in RA, then updates the FLAGS accordingly | 1 |  |
| bd | `not RB` | Performs a bitwise NOT of RB, stores the result back in RB, then updates the FLAGS accordingly | 1 |  |
| be | `not RC` | Performs a bitwise NOT of RC, stores the result back in RC, then updates the FLAGS accordingly | 1 |  |
| bf | `not RD` | Performs a bitwise NOT of RD, stores the result back in RD, then updates the FLAGS accordingly | 1 |  |
| c0 | `inc RA` | Increments RA by 1, then updates the FLAGS accordingly | 1 |  |
| c1 | `inc RB` | Increments RB by 1, then updates the FLAGS accordingly | 1 |  |
| c2 | `inc RC` | Increments RC by 1, then updates the FLAGS accordingly | 1 |  |
| c3 | `inc RD` | Increments RD by 1, then updates the FLAGS accordingly | 1 |  |
| c4 | `dec RA` | Decrements RA by 1, then updates the FLAGS accordingly | 1 |  |
| c5 | `dec RB` | Decrements RB by 1, then updates the FLAGS accordingly | 1 |  |
| c6 | `dec RC` | Decrements RC by 1, then updates the FLAGS accordingly | 1 |  |
| c7 | `dec RD` | Decrements RD by 1, then updates the FLAGS accordingly | 1 |  |
| c8 | `shl RA` | Performs a left bitwise shift of RA by 1 bit | 1 |  |
| c9 | `shr RA` | Performs a right bitwise shift of RA by 1 bit | 1 |  |
| ca | `swap RA,	RB` | Swaps the values of RA and RB | 3 |  |
| cb | `swap RA,	RC` | Swaps the values of RA and RC | 3 |  |
| cc | `swap RA,	RD` | Swaps the values of RA and RD | 3 |  |
| cd | `swap RB,	RC` | Swaps the values of RB and RC | 3 |  |
| ce | `swap RB,	RD` | Swaps the values of RB and RD | 3 |  |
| cf | `swap RC,	RD` | Swaps the values of RC and RD | 3 |  |
| d0 | `jc <byte>` | Jumps to the given short address (same code segment) if the carry flag is set | 3 |  |
| d1 | `jn <byte>` | Jumps to the given short address (same code segment) if the negative flag is set | 3 |  |
| d2 | `jz <byte>` | Jumps to the given short address (same code segment) if the zero flag is set | 3 |  |
| d3 | `goto <byte>` | Jumps to the given short address (same code segment) | 3 |  |
| d4 | `ljc <dcst>` | Jumps to the given long address if the carry flag is set | 4 |  |
| d5 | `ljn <dcst>` | Jumps to the given long address if the negative flag is set | 4 |  |
| d6 | `ljz <dcst>` | Jumps to the given long address if the zero flag is set | 4 |  |
| d7 | `lgoto <dcst>` | Jumps to the given long address | 4 |  |
| d8 | `lgoto RC:RD` | Jumps to the given long address (full indirect addressing mode) | 4 |  |
| d9 | `lcall <dcst>` | Calls the given long address | 6 |  |
| da | `lcall [RC:RD]` | Calls the given long address (full indirect addressing mode) | 6 |  |
| db | `ret ` | Returns from a call | 4 |  |
| dc | `iret ` | Returns from an interrupt | 5 |  |
| dd | `cid ` | Clears the interrupt disable flag | 1 |  |
| de | `sid ` | Sets the interrupt disable flag | 1 |  |
| df | `clc ` | Clears the carry flag | 1 |  |
| e0 | `sec ` | Sets the carry flag | 1 |  |
