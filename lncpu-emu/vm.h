//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_VM_H
#define LNCPU_EMU_VM_H

#include <stdbool.h>
#include <stdint.h>
#include "config/cmdline.h"

#define FLAGS_C 0x01
#define FLAGS_Z 0x02
#define FLAGS_N 0x03
#define FLAGS_I 0x04

struct lncpu_vm {

    uint8_t ra, rb, rc, rd;
    uint8_t ds, ss, sp, bp;

    uint8_t flags;

    uint16_t cspc;
    uint8_t addr_space[0x10000];
};

bool init_vm(struct lncpu_vm *vm, const struct emu_cmdline_params *cmdline_params);


#endif //LNCPU_EMU_VM_H
