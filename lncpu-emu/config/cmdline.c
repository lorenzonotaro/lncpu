#include "argparse.h"
#include "cmdline.h"

#include <stdbool.h>
#include <stdio.h>

int validate_args(const struct emu_cmdline_params * params) {
    if (!params->linker_config_file && params->emu_tty_section) {
        fprintf(stderr, "emu-tty requires linker-config\n");
        return 1;
    }
    return 0;
}

void cmdline_init(struct emu_cmdline_params *params) {
    params->pause = false;
    params->linker_config_file = NULL;
    params->emu_tty_section = NULL;
    params->rom_file = NULL;
    params->ram_file = NULL;
    params->d0_file = NULL;
    params->d1_file = NULL;
    params->d2_file = NULL;
    params->d3_file = NULL;
    params->d4_file = NULL;
    params->d5_file = NULL;
}

int parse_args(struct emu_cmdline_params *params, int argc, const char **argv) {
    struct argparse_option options[] = {
        OPT_HELP(),
        OPT_GROUP("Debugger options"),
        OPT_BOOLEAN('p', "pause", &params->pause, "pause before start", NULL, 0, 0),
        OPT_GROUP("Emulator options"),
        OPT_STRING('l', "linker-config", &params->linker_config_file, "linker config file", NULL, 0, 0),
        OPT_STRING('t', "emu-tty", &params->emu_tty_section, "section for emulating tty (requires section config)",
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
