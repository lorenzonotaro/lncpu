** _This page is a work in progress._ **
V1 is the current implementation of the lncpu.

## Clock cycle

![Clock diagram](clock-diagram.png)

The clock cycle is divided in two phases: Φ1 (control phase) and Φ2 (data phase).

* During Φ1, the data bus contains the contents of `[CS:PC]`: only the device mapped to the memory section containing address `CS:PC` is allowed to write to the data bus, regardless of control signals.

* During Φ2, bus control is determined by the control signals of the current microinstruction.

Each phase has a clock pulse entirely within it, and components may synchronize at the rising edge or the falling edge of each clock pulse.

From Φ and the base clock, 4 rising edges are derived:
* **C1** (Φ1), _control_: the next instruction or microinstruction is loaded and the control signals are stabilized.
* **C2** (Φ1), _fetch_: if required by the current microinstruction, data is moved from `[CS:PC]` to the data latch register via the data bus.
* **C3** (Φ2), _data_: data is transferred between the activated components via the data bus.
* **C4** (Φ2), _increment_: counters such as `CS:PC`, `SP` are incremented/decremented as specified by the current microinstruction.


# Pipeline

The implementation uses a very simple pipeline to spare a few clock cycles.


1. At the beginning of the first clock cycle of any instruction (i.e. when Φ1 goes high), the following conditions are required: 

    * `Data latch (DL)` must contain the **value of the instruction about to be executed**.
    * `CS:PC` must contain the the address of the byte **after the opcode about to be executed**.

*Every instruction (including break, jump and call instructions) must pass to the next with this configuration*.

2. During the execution of the instruction:

    1. `Data latch (DL)` will be loaded with the value pointed to by `CS:PC` (i.e. either the next opcode or the next byte to be fetched by the instruction), then
    2. `CS:PC` will be incremented to point to the byte that will be fetched during the next clock cycle.