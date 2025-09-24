//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_CMDLINE_H
#define LNCPU_EMU_CMDLINE_H

#include <stdbool.h>

#include "argparse.h"

struct emu_cmdline_params {
    /* Debugger options */
    int pause_on_start; /* Pause before start. */
    const char *no_pause_on_halt; /* Don't pause on halt. */
    const char *dump_status; /* Dump status on exit. */
    const char *dump_address_space; /* Dump address space on exit. */

    /* Emulator options */
    const char *emu_tty_device; /* Device (D0-D5) for emulating tty. */

    /* Data options */
    const char *rom_file;
    const char *ram_file;
    const char *d0_file;
    const char *d1_file;
    const char *d2_file;
    const char *d3_file;
    const char *d4_file;
    const char *d5_file;
};

void cmdline_init(struct emu_cmdline_params *params);

bool parse_args(struct emu_cmdline_params *params, int argc, const char **argv);

#endif //LNCPU_EMU_CMDLINE_H