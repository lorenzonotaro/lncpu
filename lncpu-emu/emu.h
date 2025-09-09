//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_EMU_H
#define LNCPU_EMU_EMU_H

#include "config/cmdline.h"
#include "vm.h"

enum emu_status {
    EMU_STATUS_RUNNING = 0x0,
    EMU_STATUS_PAUSED = 0x1,
    EMU_STATUS_STEPPING = 0x2,
    EMU_STATUS_TERMINATED = 0x3,
};

struct emulator {
    struct lncpu_vm vm;
    enum emu_status status;
};

struct emulator *get_emu(void);

int run_emu(struct emu_cmdline_params *cmdline_params);

#endif //LNCPU_EMU_EMU_H