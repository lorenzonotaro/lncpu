

#include "config/cmdline.h"
#include "emu.h"

#include "signal_handler.h"

int main(int argc, const char **argv) {
    struct emu_cmdline_params cmdline_params;

    cmdline_init(&cmdline_params);

    if (!parse_args(&cmdline_params, argc, argv)) {
        return 1;
    }

    setup_signal_handler();

    return run_emu(&cmdline_params);
}
