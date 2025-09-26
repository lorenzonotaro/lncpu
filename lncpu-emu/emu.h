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
    EMU_STATUS_STEPPING_OVER = 0x3,
    EMU_STATUS_TERMINATED = 0x4,
};

struct bp;

struct bp {
    uint16_t addr;
    struct bp *next;
};

struct emulator {
    struct lncpu_vm vm;
    enum emu_status status;
    uint16_t step_over_target_addr;
    uint16_t step_over_target_sssp;

    struct bp *bp_list;
};

struct emulator *get_emu(void);

int run_emu(const struct emu_cmdline_params *cmdline_params);

#endif //LNCPU_EMU_EMU_H