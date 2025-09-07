//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_VM_H
#define LNCPU_EMU_VM_H

#include <stdbool.h>
#include <stdint.h>
#include "config/cmdline.h"
#include "emu_device.h"

#define FLAGS_C ((uint8_t)0x01)
#define FLAGS_Z ((uint8_t)0x02)
#define FLAGS_N ((uint8_t)0x04)
#define FLAGS_I ((uint8_t)0x08)

struct lncpu_vm {

    uint8_t ra, rb, rc, rd;
    uint8_t ds, ss, sp, bp;

    uint8_t flags;

    uint16_t cspc;
    uint8_t addr_space[0x10000];

    struct emu_device emu_devices[6];

    size_t emu_device_count;

    bool halted;
};

bool vm_init(struct lncpu_vm *vm, const struct emu_cmdline_params *cmdline_params);

void vm_step(struct lncpu_vm *vm);

void vm_destroy(struct lncpu_vm *vm);

#endif //LNCPU_EMU_VM_H
