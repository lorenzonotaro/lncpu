

#include "config/cmdline.h"


int main(int argc, const char **argv) {
    struct emu_cmdline_params cmdline_params;
    parse_args(&cmdline_params, argc, argv);

    return run_emu(cmdline_params);
}
