//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_CMDLINE_H
#define LNCPU_EMU_CMDLINE_H

#include "argparse.h"

struct emu_cmdline_params {
    /* Debugger options */
    int pause_on_start; /* Pause before start. */

    /* Emulator options */
    const char *linker_config_file; /* Linker config file. */
    const char *emu_tty_section; /* Section for emulating tty. */

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

int parse_args(struct emu_cmdline_params *params, int argc, const char **argv);

#endif //LNCPU_EMU_CMDLINE_H