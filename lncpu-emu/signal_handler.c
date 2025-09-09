//
// Created by loryn on 9/9/2025.
//

#include "signal_handler.h"

#include "emu.h"
#include <signal.h>
#include <stdlib.h>

#include "vm.h"

static void *signal_handler_installed = 0;

void signal_handler(int signal) {
    if (signal == SIGINT) {
        struct emulator *emu = get_emu();
        if (emu) {
            if (emu->vm.halted || emu->status == EMU_STATUS_TERMINATED || emu->status == EMU_STATUS_PAUSED) {
                exit(0);
            }else {
                emu->status = EMU_STATUS_PAUSED;
            }
        }else {
            exit(0);
        }
    }
}

void setup_signal_handler() {
    signal_handler_installed = signal(SIGINT, signal_handler);
}