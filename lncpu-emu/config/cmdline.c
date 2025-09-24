#include "argparse.h"
#include "cmdline.h"

#include <ctype.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>

bool validate_args(const struct emu_cmdline_params * params) {
    if (params->emu_tty_device && !(strlen(params->emu_tty_device) == 2 && (params->emu_tty_device[0] == 'd' || params->emu_tty_device[0] == 'D') && isdigit(params->emu_tty_device[1]))) {
        fprintf(stderr, "Invalid emu-tty device: %s\n", params->emu_tty_device);
        return false;
    }
    return true;
}

void cmdline_init(struct emu_cmdline_params *params) {
    params->pause_on_start = false;
    params->no_pause_on_halt = false;
    params->dump_status = NULL;
    params->dump_address_space = NULL;
    params->emu_tty_device = NULL;
    params->rom_file = NULL;
    params->ram_file = NULL;
    params->d0_file = NULL;
    params->d1_file = NULL;
    params->d2_file = NULL;
    params->d3_file = NULL;
    params->d4_file = NULL;
    params->d5_file = NULL;
}

bool parse_args(struct emu_cmdline_params *params, int argc, const char **argv) {
    struct argparse_option options[] = {
        OPT_HELP(),
        OPT_GROUP("Debugger options"),
        OPT_BOOLEAN('p', "pause", &params->pause_on_start, "pause before start", NULL, 0, 0),
        OPT_BOOLEAN(0, "nopauseonhalt", &params->no_pause_on_halt, "don't pause on halt", NULL, 0, 0),
        OPT_STRING(0, "dumpstatus", &params->dump_status, "dump status on exit"),
        OPT_STRING(0, "dumpaddrspace", &params->dump_address_space, "dump address space on exit"),
        OPT_GROUP("Emulator options"),
        OPT_STRING('t', "emu-tty", &params->emu_tty_device, "Device (D0-D5) for emulating tty",
                   NULL, 0, 0),
        OPT_GROUP("Data options"),
        OPT_STRING(0, "rom", &params->rom_file, "ROM file to load", NULL, 0, 0),
        OPT_STRING(0, "ram", &params->ram_file, "RAM file to load", NULL, 0, 0),
        OPT_STRING(0, "d0", &params->d0_file, "D0 file to load", NULL, 0, 0),
        OPT_STRING(0, "d1", &params->d1_file, "D1 file to load", NULL, 0, 0),
        OPT_STRING(0, "d2", &params->d2_file, "D2 file to load", NULL, 0, 0),
        OPT_STRING(0, "d3", &params->d3_file, "D3 file to load", NULL, 0, 0),
        OPT_STRING(0, "d4", &params->d4_file, "D4 file to load", NULL, 0, 0),
        OPT_STRING(0, "d5", &params->d5_file, "D5 file to load", NULL, 0, 0),
        OPT_END()
    };

    const char * const usages[] = {
        "lncpu-emu [options]",
        NULL
    };

    struct argparse argparse;
    argparse_init(&argparse, options, usages, 0);
    argparse_describe(&argparse,
                      "LNCpu Emulator",
                      "A simple lncpu emulator.");

    argparse_parse(&argparse, argc, argv);

    return validate_args(params);
}
