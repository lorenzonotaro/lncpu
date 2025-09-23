//
// Created by loryn on 9/6/2025.
//

#include "vm.h"
#include "emu_device.h"
#include <stdio.h>
#include <stdlib.h>

#include "opcodes.h"
#include "linker_config/link_target.h"
#include "linker_config/linker_config.h"
#include "linker_config/linker_config_parser.h"

static bool init_emu_devices(struct lncpu_vm *vm, const struct emu_cmdline_params * cmdline_params) {
    if (cmdline_params->linker_config_file) {
        LinkerConfig lc;
        FILE *f = fopen(cmdline_params->linker_config_file, "r");
        if (f) {
            fseek(f, 0, SEEK_END);
            const size_t size = ftell(f);
            char *cfg_contents = malloc(size + 1);

            fseek(f, 0, SEEK_SET);
            fread(cfg_contents, 1, size, f);;
            fclose(f);

            char *err = NULL;
            if (!parse_linker_config_from_source(cfg_contents, size, cmdline_params->linker_config_file, &lc, &err)) {
                fprintf(stderr, "error parsing linker config: %s\n", err);
                return false;
            }
        }else {
            fprintf(stderr, "error opening linker config file: %s\n", cmdline_params->linker_config_file);
            return false;
        }

        if (cmdline_params->emu_tty_section) {

            const SectionInfo *section = linker_config_get_section(&lc, cmdline_params->emu_tty_section);

            if (!section) {
                fprintf(stderr, "emu-tty section not found: %s\n", cmdline_params->emu_tty_section);
                return false;
            }
            vm->emu_devices[0].start = section->start;
            vm->emu_devices[0].end = section->start + 2;
            vm->emu_devices[0].init = emu_tty_init;
            vm->emu_devices[0].step = emu_tty_step;
            vm->emu_devices[0].pause = emu_tty_pause;
            vm->emu_devices[0].resume = emu_tty_resume;
            vm->emu_devices[0].addr_read = emu_tty_addr_read;
            vm->emu_devices[0].addr_write = emu_tty_addr_write;
            vm->emu_devices[0].destroy = emu_tty_destroy;

            // initialize the emulated device
            vm->emu_devices[0].init(vm, &vm->emu_devices[0]);

            vm->emu_device_count++;
        }
    }



    return true;
}

bool try_load_device(LinkTarget lt, const char * file, uint8_t * addr_space) {
    FILE *f = fopen(file, "rb");

    if (!f) {
        fprintf(stderr, "error opening file: %s\n", file);
        return false;
    }

    size_t size = 0;
    fseek(f, 0, SEEK_END);
    size = ftell(f);
    fseek(f, 0, SEEK_SET);

    if (size > 0x2000) {
        printf("file %s is too large: truncating to 8192 bytes\n", file);
        size = 0x2000;
    }

    const LinkTargetInfo *target_info = link_target_info(lt);
    // load into address space
    int read = fread(addr_space + target_info->start, 1, size, f);

    if (read != size) {
        fprintf(stderr, "error reading file: %d, %d\n", read, ferror(f));
        fclose(f);
        return false;
    }

    fclose(f);

    return true;
}

int init_addr_space(uint8_t * addr_space, const struct emu_cmdline_params * cmdline_params) {

    if (cmdline_params->rom_file && !try_load_device(LT_ROM, cmdline_params->rom_file, addr_space)) {
        return false;
    }

    if (cmdline_params->ram_file && !try_load_device(LT_RAM, cmdline_params->ram_file, addr_space)) {
        return false;
    }

    if (cmdline_params->d0_file && !try_load_device(LT_D0, cmdline_params->d0_file, addr_space)) {
        return false;
    }

    if (cmdline_params->d1_file && !try_load_device(LT_D1, cmdline_params->d1_file, addr_space)) {
        return false;
    }

    if (cmdline_params->d2_file && !try_load_device(LT_D2, cmdline_params->d2_file, addr_space)) {
        return false;
    }

    if (cmdline_params->d3_file && !try_load_device(LT_D3, cmdline_params->d3_file, addr_space)) {
        return false;
    }

    if (cmdline_params->d4_file && !try_load_device(LT_D4, cmdline_params->d4_file, addr_space)) {
        return false;
    }

    if (cmdline_params->d5_file && !try_load_device(LT_D5, cmdline_params->d5_file, addr_space)) {
        return false;
    }

    return true;
}

bool emu_device_intercept_read(struct lncpu_vm *vm, uint16_t addr, uint8_t *value) {
    for (size_t i = 0; i < vm->emu_device_count; i++) {
        if (addr >= vm->emu_devices[i].start && addr <= vm->emu_devices[i].end) {
            emu_device_addr_read_func read = vm->emu_devices[i].addr_read;
            if (read) {
                *value = vm->emu_devices[i].addr_read(vm, &vm->emu_devices[i], vm->emu_devices[i].data, addr);
            }
            return true;
        }
    }
    return false;
}

uint8_t read_byte(struct lncpu_vm *vm, uint16_t addr) {
    uint8_t value;
    if (!emu_device_intercept_read(vm, addr, &value)) {
        value = vm->addr_space[addr];
    }
    return value;
}

uint8_t fetch_byte(struct lncpu_vm *vm) {
    return read_byte(vm, vm->cspc++);
}

bool emu_device_intercept_write(struct lncpu_vm *vm, uint16_t addr, uint8_t value) {
    for (size_t i = 0; i < vm->emu_device_count; i++) {
        if (addr >= vm->emu_devices[i].start && addr <= vm->emu_devices[i].end) {
            emu_device_addr_write_func write = vm->emu_devices[i].addr_write;
            if (write) {
                vm->emu_devices[i].addr_write(vm, &vm->emu_devices[i], vm->emu_devices[i].data, addr, value);
            }
            return true;
        }
    }
    return false;
}

void write_byte(struct lncpu_vm *vm, uint16_t addr, uint8_t value) {
    if (!emu_device_intercept_write(vm, addr, value)) {
        vm->addr_space[addr] = value;
    }
}

bool vm_init(struct lncpu_vm *vm, const struct emu_cmdline_params *cmdline_params) {
    vm->ra = 0;
    vm->rb = 0;
    vm->rc = 0;
    vm->rd = 0;
    vm->ds = 0;
    vm->ss = 0;
    vm->sp = 0;
    vm->bp = 0;
    vm->flags = 0;
    vm->cspc = 0;
    vm->emu_device_count = 0;
    vm->halted = 0;

    if (!init_addr_space(vm->addr_space, cmdline_params)) {
        return false;
    }

    return init_emu_devices(vm, cmdline_params);
}

void push(struct lncpu_vm *vm, uint8_t value) {
    uint16_t addr = ((uint16_t)vm->ss << 8) | vm->sp;
    write_byte(vm, addr, value);
    vm->sp = (uint8_t)(vm->sp + 1);
}

void pop(struct lncpu_vm *vm, uint8_t *dest) {
    vm->sp = (uint8_t)(vm->sp - 1);
    uint16_t addr = ((uint16_t)vm->ss << 8) | vm->sp;
    *dest = read_byte(vm, addr);
}

int irq_req(struct lncpu_vm * vm) {
    for (size_t i = 0; i < vm->emu_device_count; i++) {
        if (vm->emu_devices[i].irq_req) {
            return true;
        }
    }
    return false;
}

void set_flags(struct lncpu_vm * vm, uint16_t result, const bool set_carry) {
    vm->flags = (vm->flags & FLAGS_I) | // I flag untouched
        (set_carry && result > 0xFF ? FLAGS_C : 0) | // result carried over
            ((result & 0xFF) == 0 ? FLAGS_Z : 0) | // result zero
            (result & 0b10000000 ? FLAGS_N : 0); // result negative (bit 7 set)
}

void vm_step(struct lncpu_vm *vm) {

    if (vm->halted) return;

    enum Opcode opcode;

    uint8_t temp1, temp2, temp3;
    uint16_t temp16;

    if (!(vm->flags & FLAGS_I) && irq_req(vm)) {
        // handle interrupt
        opcode = OP__BRK_;
    }else {
        opcode = fetch_byte(vm);
    }

    switch (opcode) {
        case OP_NOP:
            break;
        case OP_HLT:
            vm->halted = true;
            break;
        case OP__BRK_:
        case OP_INT:
            push(vm, (vm->cspc >> 8) & 0xFF);
            push(vm, vm->cspc & 0xFF);
            push(vm, vm->flags);
            vm->cspc = 0x1F00;
            vm->flags |= FLAGS_I;
            break;
        case OP_MOV_CST_RA:
            vm->ra = fetch_byte(vm);
            break;
        case OP_MOV_CST_RB:
            vm->rb = fetch_byte(vm);
            break;
        case OP_MOV_CST_RC:
            vm->rc = fetch_byte(vm);
            break;
        case OP_MOV_CST_RD:
            vm->rd = fetch_byte(vm);
            break;
        case OP_MOV_CST_SP:
            vm->sp = fetch_byte(vm);
            break;
        case OP_MOV_CST_SS:
            vm->ss = fetch_byte(vm);
            break;
        case OP_MOV_CST_DS:
            vm->ds = fetch_byte(vm);
            break;
        case OP_MOV_CST_BP:
            vm->bp = fetch_byte(vm);
            break;
        case OP_MOV_SS_RD:
            vm->rd = vm->ss;
            break;
        case OP_MOV_SP_RD:
            vm->rd = vm->sp;
            break;
        case OP_MOV_SP_BP:
            vm->bp = vm->sp;
            break;
        case OP_MOV_BP_SP:
            vm->sp = vm->bp;
            break;
        case OP_MOV_DS_RD:
            vm->rd = vm->ds;
            break;
        case OP_MOV_RA_RB:
            vm->rb = vm->ra;
            break;
        case OP_MOV_RA_RC:
            vm->rc = vm->ra;
            break;
        case OP_MOV_RA_RD:
            vm->rd = vm->ra;
            break;
        case OP_MOV_RB_RA:
            vm->ra = vm->rb;
            break;
        case OP_MOV_RB_RC:
            vm->rc = vm->rb;
            break;
        case OP_MOV_RB_RD:
            vm->rd = vm->rb;
            break;
        case OP_MOV_RC_RA:
            vm->ra = vm->rc;
            break;
        case OP_MOV_RC_RB:
            vm->rb = vm->rc;
            break;
        case OP_MOV_RC_RD:
            vm->rd = vm->rc;
            break;
        case OP_MOV_RD_RA:
            vm->ra = vm->rd;
            break;
        case OP_MOV_RD_RB:
            vm->rb = vm->rd;
            break;
        case OP_MOV_RD_RC:
            vm->rc = vm->rd;
            break;
        case OP_MOV_RD_SS:
            vm->ss = vm->rd;
            break;
        case OP_MOV_RD_SP:
            vm->sp = vm->rd;
            break;
        case OP_MOV_RD_DS:
            vm->ds = vm->rd;
            break;
        case OP_MOV_IBPOFFSET_RA:
            temp1 = vm->bp + fetch_byte(vm);
            vm->ra = read_byte(vm, ((uint16_t) vm->ss << 8 | temp1));
            break;
        case OP_MOV_IBPOFFSET_RB:
            temp1 = vm->bp + fetch_byte(vm);
            vm->rb = read_byte(vm, ((uint16_t) vm->ss << 8 | temp1));
            break;
        case OP_MOV_IBPOFFSET_RC:
            temp1 = vm->bp + fetch_byte(vm);
            vm->rc = read_byte(vm, ((uint16_t) vm->ss << 8 | temp1));
            break;
        case OP_MOV_IBPOFFSET_RD:
            temp1 = vm->bp + fetch_byte(vm);
            vm->rd = read_byte(vm, ((uint16_t) vm->ss << 8 | temp1));
            break;
        case OP_MOV_RA_IBPOFFSET:
            temp1 = vm->bp + fetch_byte(vm);
            write_byte(vm, ((uint16_t) vm->ss << 8 | temp1), vm->ra);
            break;
        case OP_MOV_RB_IBPOFFSET:
            temp1 = vm->bp + fetch_byte(vm);
            write_byte(vm, ((uint16_t) vm->ss << 8 | temp1), vm->rb);
            break;
        case OP_MOV_RC_IBPOFFSET:
            temp1 = vm->bp + fetch_byte(vm);
            write_byte(vm, ((uint16_t) vm->ss << 8 | temp1), vm->rc);
            break;
        case OP_MOV_RD_IBPOFFSET:
            temp1 = vm->bp + fetch_byte(vm);
            write_byte(vm, ((uint16_t) vm->ss << 8 | temp1), vm->rd);
            break;
        case OP_MOV_CST_IBPOFFSET:
            temp1 = fetch_byte(vm);
            write_byte(vm, ((uint16_t) vm->ss << 8 | temp1),  fetch_byte(vm));
            break;
        case OP_MOV_RA_DATAP:
            write_byte(vm, ((uint16_t) vm->ds) << 8 | fetch_byte(vm), vm->ra);
            break;
        case OP_MOV_RB_DATAP:
            write_byte(vm, ((uint16_t) vm->ds) << 8 | fetch_byte(vm), vm->rb);
            break;
        case OP_MOV_RC_DATAP:
            write_byte(vm, (uint16_t) vm->ds << 8 | fetch_byte(vm), vm->rc);
            break;
        case OP_MOV_RD_DATAP:
            write_byte(vm, ((uint16_t) vm->ds) << 8 | fetch_byte(vm), vm->rd);
            break;
        case OP_MOV_DATAP_RA:
            vm->ra = read_byte(vm, ((uint16_t) vm->ds) << 8 | fetch_byte(vm));
            break;
        case OP_MOV_DATAP_RB:
            vm->rb = read_byte(vm, ((uint16_t) vm->ds) << 8 | fetch_byte(vm));
            break;
        case OP_MOV_DATAP_RC:
            vm->rc = read_byte(vm, ((uint16_t) vm->ds) << 8 | fetch_byte(vm));
            break;
        case OP_MOV_DATAP_RD:
            vm->rd = read_byte(vm, ((uint16_t) vm->ds) << 8 | fetch_byte(vm));
            break;
        case OP_MOV_CST_DATAP:
            temp1 = fetch_byte(vm);
            write_byte(vm, ((uint16_t) vm->ds << 8) + temp1, fetch_byte(vm));
            break;
        case OP_MOV_RA_IRCRD:
            write_byte(vm, (uint16_t) vm->rc << 8 | vm->rd, vm->ra);
            break;
        case OP_MOV_RB_IRCRD:
            write_byte(vm, (uint16_t) vm->rc << 8 | vm->rd, vm->rb);
            break;
        case OP_MOV_RC_IRCRD:
            write_byte(vm, (uint16_t) vm->rc << 8 | vm->rd, vm->rc);
            break;
        case OP_MOV_RD_IRCRD:
            write_byte(vm, (uint16_t) vm->rc << 8 | vm->rd, vm->rd);
            break;
        case OP_MOV_IRCRD_RA:
            vm->ra = read_byte(vm, (uint16_t) vm->rc << 8 | vm->rd);
            break;
        case OP_MOV_IRCRD_RB:
            vm->rb = read_byte(vm, (uint16_t) vm->rc << 8 | vm->rd);
            break;
        case OP_MOV_IRCRD_RC:
            vm->rc = read_byte(vm, (uint16_t) vm->rc << 8 | vm->rd);
            break;
        case OP_MOV_IRCRD_RD:
            vm->rd = read_byte(vm, (uint16_t) vm->rc << 8 | vm->rd);
            break;
        case OP_MOV_CST_IRCRD:
            write_byte(vm, (uint16_t) vm->rc << 8 | vm->rd, fetch_byte(vm));
            break;
        case OP_MOV_RA_ABS:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            write_byte(vm, (uint16_t) temp1 << 8 | temp2, vm->ra);
            break;
        case OP_MOV_RB_ABS:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            write_byte(vm, (uint16_t) temp1 << 8 | temp2, vm->rb);
            break;
        case OP_MOV_RC_ABS:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            write_byte(vm, (uint16_t) temp1 << 8 | temp2, vm->rc);
            break;
        case OP_MOV_RD_ABS:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            write_byte(vm, (uint16_t) temp1 << 8 | temp2, vm->rd);
            break;
        case OP_MOV_ABS_RA:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            vm->ra = read_byte(vm, (uint16_t) temp1 << 8 | temp2);
            break;
        case OP_MOV_ABS_RB:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            vm->rb = read_byte(vm, (uint16_t) temp1 << 8 | temp2);
            break;
        case OP_MOV_ABS_RC:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            vm->rc = read_byte(vm, (uint16_t) temp1 << 8 | temp2);
            break;
        case OP_MOV_ABS_RD:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            vm->rd = read_byte(vm, (uint16_t) temp1 << 8 | temp2);
            break;
        case OP_MOV_CST_ABS:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            write_byte(vm, (uint16_t) temp1 << 8 | temp2, fetch_byte(vm));
            break;
        case OP_MOV_DATAP_DATAP:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            write_byte(vm, (uint16_t) vm->ds << 8 | temp2, read_byte(vm, (uint16_t) vm->ds << 8 | temp1));
            break;
        case OP_MOV_IRD_RA:
            vm->ra = read_byte(vm, (uint16_t) vm->ds << 8 | vm->rd);
            break;
        case OP_MOV_IRD_RB:
            vm->rb = read_byte(vm, (uint16_t) vm->ds << 8 | vm->rd);
            break;
        case OP_MOV_IRD_RC:
            vm->rc = read_byte(vm, (uint16_t) vm->ds << 8 | vm->rd);
            break;
        case OP_MOV_IRD_RD:
            vm->rd = read_byte(vm, (uint16_t) vm->ds << 8 | vm->rd);
            break;
        case OP_MOV_RA_IRD:
            write_byte(vm, (uint16_t) vm->ds << 8 | vm->rd, vm->ra);
            break;
        case OP_MOV_RB_IRD:
            write_byte(vm, (uint16_t) vm->ds << 8 | vm->rd, vm->rb);
            break;
        case OP_MOV_RC_IRD:
            write_byte(vm, (uint16_t) vm->ds << 8 | vm->rd, vm->rc);
            break;
        case OP_MOV_RD_IRD:
            write_byte(vm, (uint16_t) vm->ds << 8 | vm->rd, vm->rd);
            break;
        case OP_MOV_CST_IRD:
            write_byte(vm, (uint16_t) vm->ds << 8 | vm->rd, fetch_byte(vm));
            break;
        case OP_PUSH_RA:
            push(vm, vm->ra);
            break;
        case OP_PUSH_RB:
            push(vm, vm->rb);
            break;
        case OP_PUSH_RC:
            push(vm, vm->rc);
            break;
        case OP_PUSH_RD:
            push(vm, vm->rd);
            break;
        case OP_PUSH_DATAP:
            push(vm, read_byte(vm, (uint16_t) vm->ds << 8 | fetch_byte(vm)));
            break;
        case OP_PUSH_IRD:
            push(vm, read_byte(vm, (uint16_t) vm->ds << 8 | vm->rd));
            break;
        case OP_PUSH_IRCRD:
            push(vm, read_byte(vm, (uint16_t) vm->rc << 8 | vm->rd));
            break;
        case OP_PUSH_ABS:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            push(vm, read_byte(vm, (uint16_t) temp1 << 8 | temp2));
            break;
        case OP_PUSH_CST:
            push(vm, fetch_byte(vm));
            break;
        case OP_PUSH_BP:
            push(vm, vm->bp);
            break;
        case OP_POP_RA:
            pop(vm, &vm->ra);
            break;
        case OP_POP_RB:
            pop(vm, &vm->rb);
            break;
        case OP_POP_RC:
            pop(vm, &vm->rc);
            break;
        case OP_POP_RD:
            pop(vm, &vm->rd);
            break;
        case OP_POP_DATAP:
            pop(vm, &temp1);
            write_byte(vm, (uint16_t) vm->ds << 8 | fetch_byte(vm), temp1);
            break;
        case OP_POP_IRD:
            pop(vm, &temp1);
            write_byte(vm, (uint16_t) vm->ds << 8 | vm->rd, temp1);
            break;
        case OP_POP_IRCRD:
            pop(vm, &temp1);
            write_byte(vm, (uint16_t) vm->rc << 8 | vm->rd, temp1);
            break;
        case OP_POP_ABS:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            pop(vm, &temp3);
            write_byte(vm, (uint16_t) temp1 << 8 | temp2, temp3);
            break;
        case OP_POP_BP:
            pop(vm, &vm->bp);
            break;
        case OP_ADD_RA_RA:
            temp16 = (uint16_t) vm->ra + vm->ra;
            set_flags(vm, temp16, true);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_ADD_RA_RB:
            temp16 = (uint16_t) vm->ra + vm->rb;
            set_flags(vm, temp16, true);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_ADD_RA_RC:
            temp16 = (uint16_t) vm->ra + vm->rc;
            set_flags(vm, temp16, true);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_ADD_RA_RD:
            temp16 = (uint16_t) vm->ra + vm->rd;
            set_flags(vm, temp16, true);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_ADD_RB_RA:
            temp16 = (uint16_t) vm->rb + vm->ra;
            set_flags(vm, temp16, true);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_ADD_RB_RB:
            temp16 = (uint16_t) vm->rb + vm->rb;
            set_flags(vm, temp16, true);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_ADD_RB_RC:
            temp16 = (uint16_t) vm->rb + vm->rc;
            set_flags(vm, temp16, true);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_ADD_RB_RD:
            temp16 = (uint16_t) vm->rb + vm->rd;
            set_flags(vm, temp16, true);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_ADD_RC_RA:
            temp16 = (uint16_t) vm->rc + vm->ra;
            set_flags(vm, temp16, true);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_ADD_RC_RB:
            temp16 = (uint16_t) vm->rc + vm->rb;
            set_flags(vm, temp16, true);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_ADD_RC_RC:
            temp16 = (uint16_t) vm->rc + vm->rc;
            set_flags(vm, temp16, true);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_ADD_RC_RD:
            temp16 = (uint16_t) vm->rc + vm->rd;
            set_flags(vm, temp16, true);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_ADD_RD_RA:
            temp16 = (uint16_t) vm->rd + vm->ra;
            set_flags(vm, temp16, true);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_ADD_RD_RB:
            temp16 = (uint16_t) vm->rd + vm->rb;
            set_flags(vm, temp16, true);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_ADD_RD_RC:
            temp16 = (uint16_t) vm->rd + vm->rc;
            set_flags(vm, temp16, true);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_ADD_RD_RD:
            temp16 = (uint16_t) vm->rd + vm->rd;
            set_flags(vm, temp16, true);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_ADD_RA_CST:
            temp16 = (uint16_t) vm->ra + fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_ADD_RB_CST:
            temp16 = (uint16_t) vm->rb + fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_ADD_RC_CST:
            temp16 = (uint16_t) vm->rc + fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_ADD_RD_CST:
            temp16 = (uint16_t) vm->rd + fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_ADD_SP_CST:
            temp16 = (uint16_t) vm->sp + (int8_t) fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->sp = (uint8_t) temp16;
            break;
        case OP_ADD_BP_CST:
            temp16 = (uint16_t) vm->bp + (int8_t) fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->bp = (uint8_t) temp16;
            break;
        case OP_SUB_RA_RA:
            temp16 = (uint16_t) vm->ra - vm->ra;
            set_flags(vm, temp16, true);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_SUB_RA_RB:
            temp16 = (uint16_t) vm->ra - vm->rb;
            set_flags(vm, temp16, true);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_SUB_RA_RC:
            temp16 = (uint16_t) vm->ra - vm->rc;
            set_flags(vm, temp16, true);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_SUB_RA_RD:
            temp16 = (uint16_t) vm->ra - vm->rd;
            set_flags(vm, temp16, true);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_SUB_RB_RA:
            temp16 = (uint16_t) vm->rb - vm->ra;
            set_flags(vm, temp16, true);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_SUB_RB_RB:
            temp16 = (uint16_t) vm->rb - vm->rb;
            set_flags(vm, temp16, true);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_SUB_RB_RC:
            temp16 = (uint16_t) vm->rb - vm->rc;
            set_flags(vm, temp16, true);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_SUB_RB_RD:
            temp16 = (uint16_t) vm->rb - vm->rd;
            set_flags(vm, temp16, true);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_SUB_RC_RA:
            temp16 = (uint16_t) vm->rc - vm->ra;
            set_flags(vm, temp16, true);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_SUB_RC_RB:
            temp16 = (uint16_t) vm->rc - vm->rb;
            set_flags(vm, temp16, true);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_SUB_RC_RC:
            temp16 = (uint16_t) vm->rc - vm->rc;
            set_flags(vm, temp16, true);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_SUB_RC_RD:
            temp16 = (uint16_t) vm->rc - vm->rd;
            set_flags(vm, temp16, true);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_SUB_RD_RA:
            temp16 = (uint16_t) vm->rd - vm->ra;
            set_flags(vm, temp16, true);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_SUB_RD_RB:
            temp16 = (uint16_t) vm->rd - vm->rb;
            set_flags(vm, temp16, true);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_SUB_RD_RC:
            temp16 = (uint16_t) vm->rd - vm->rc;
            set_flags(vm, temp16, true);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_SUB_RD_RD:
            temp16 = (uint16_t) vm->rd - vm->rd;
            set_flags(vm, temp16, true);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_SUB_RA_CST:
            temp16 = (uint16_t) vm->ra - fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_SUB_RB_CST:
            temp16 = (uint16_t) vm->rb - fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_SUB_RC_CST:
            temp16 = (uint16_t) vm->rc - fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_SUB_RD_CST:
            temp16 = (uint16_t) vm->rd - fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_SUB_SP_CST:
            temp16 = (uint16_t) vm->sp - fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->sp = (uint8_t) temp16;
            break;
        case OP_SUB_BP_CST:
            temp16 = (uint16_t) vm->bp - fetch_byte(vm);
            set_flags(vm, temp16, true);
            vm->bp = (uint8_t) temp16;
            break;
        case OP_CMP_RA_RA:
            temp16 = (uint16_t) vm->ra - vm->ra;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RA_RB:
            temp16 = (uint16_t) vm->ra - vm->rb;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RA_RC:
            temp16 = (uint16_t) vm->ra - vm->rc;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RA_RD:
            temp16 = (uint16_t) vm->ra - vm->rd;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RB_RA:
            temp16 = (uint16_t) vm->rb - vm->ra;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RB_RB:
            temp16 = (uint16_t) vm->rb - vm->rb;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RB_RC:
            temp16 = (uint16_t) vm->rb - vm->rc;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RB_RD:
            temp16 = (uint16_t) vm->rb - vm->rd;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RC_RA:
            temp16 = (uint16_t) vm->rc - vm->ra;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RC_RB:
            temp16 = (uint16_t) vm->rc - vm->rb;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RC_RC:
            temp16 = (uint16_t) vm->rc - vm->rc;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RC_RD:
            temp16 = (uint16_t) vm->rc - vm->rd;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RD_RA:
            temp16 = (uint16_t) vm->rd - vm->ra;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RD_RB:
            temp16 = (uint16_t) vm->rd - vm->rb;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RD_RC:
            temp16 = (uint16_t) vm->rd - vm->rc;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RD_RD:
            temp16 = (uint16_t) vm->rd - vm->rd;
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RA_CST:
            temp16 = (uint16_t) vm->ra - fetch_byte(vm);
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RB_CST:
            temp16 = (uint16_t) vm->rb - fetch_byte(vm);
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RC_CST:
            temp16 = (uint16_t) vm->rc - fetch_byte(vm);
            set_flags(vm, temp16, true);
            break;
        case OP_CMP_RD_CST:
            temp16 = (uint16_t) vm->rd - fetch_byte(vm);
            set_flags(vm, temp16, true);
            break;
        case OP_OR_RA_RA:
            temp16 = (uint16_t) vm->ra | vm->ra;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_OR_RA_RB:
            temp16 = (uint16_t) vm->ra | vm->rb;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_OR_RA_RC:
            temp16 = (uint16_t) vm->ra | vm->rc;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_OR_RA_RD:
            temp16 = (uint16_t) vm->ra | vm->rd;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_OR_RB_RA:
            temp16 = (uint16_t) vm->rb | vm->ra;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_OR_RB_RB:
            temp16 = (uint16_t) vm->rb | vm->rb;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_OR_RB_RC:
            temp16 = (uint16_t) vm->rb | vm->rc;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_OR_RB_RD:
            temp16 = (uint16_t) vm->rb | vm->rd;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_OR_RC_RA:
            temp16 = (uint16_t) vm->rc | vm->ra;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_OR_RC_RB:
            temp16 = (uint16_t) vm->rc | vm->rb;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_OR_RC_RC:
            temp16 = (uint16_t) vm->rc | vm->rc;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_OR_RC_RD:
            temp16 = (uint16_t) vm->rc | vm->rd;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_OR_RD_RA:
            temp16 = (uint16_t) vm->rd | vm->ra;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_OR_RD_RB:
            temp16 = (uint16_t) vm->rd | vm->rb;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_OR_RD_RC:
            temp16 = (uint16_t) vm->rd | vm->rc;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_OR_RD_RD:
            temp16 = (uint16_t) vm->rd | vm->rd;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_OR_RA_CST:
            temp16 = (uint16_t) vm->ra | fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_OR_RB_CST:
            temp16 = (uint16_t) vm->rb | fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_OR_RC_CST:
            temp16 = (uint16_t) vm->rc | fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_OR_RD_CST:
            temp16 = (uint16_t) vm->rd | fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_AND_RA_RA:
            temp16 = (uint16_t) vm->ra & vm->ra;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_AND_RA_RB:
            temp16 = (uint16_t) vm->ra & vm->rb;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_AND_RA_RC:
            temp16 = (uint16_t) vm->ra & vm->rc;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_AND_RA_RD:
            temp16 = (uint16_t) vm->ra & vm->rd;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_AND_RB_RA:
            temp16 = (uint16_t) vm->rb & vm->ra;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_AND_RB_RB:
            temp16 = (uint16_t) vm->rb & vm->rb;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_AND_RB_RC:
            temp16 = (uint16_t) vm->rb & vm->rc;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_AND_RB_RD:
            temp16 = (uint16_t) vm->rb & vm->rd;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_AND_RC_RA:
            temp16 = (uint16_t) vm->rc & vm->ra;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_AND_RC_RB:
            temp16 = (uint16_t) vm->rc & vm->rb;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_AND_RC_RC:
            temp16 = (uint16_t) vm->rc & vm->rc;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_AND_RC_RD:
            temp16 = (uint16_t) vm->rc & vm->rd;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_AND_RD_RA:
            temp16 = (uint16_t) vm->rd & vm->ra;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_AND_RD_RB:
            temp16 = (uint16_t) vm->rd & vm->rb;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_AND_RD_RC:
            temp16 = (uint16_t) vm->rd & vm->rc;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_AND_RD_RD:
            temp16 = (uint16_t) vm->rd & vm->rd;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_AND_RA_CST:
            temp16 = (uint16_t) vm->ra & fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_AND_RB_CST:
            temp16 = (uint16_t) vm->rb & fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_AND_RC_CST:
            temp16 = (uint16_t) vm->rc & fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_AND_RD_CST:
            temp16 = (uint16_t) vm->rd & fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_XOR_RA_RA:
            temp16 = (uint16_t) vm->ra ^ vm->ra;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_XOR_RA_RB:
            temp16 = (uint16_t) vm->ra ^ vm->rb;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_XOR_RA_RC:
            temp16 = (uint16_t) vm->ra ^ vm->rc;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_XOR_RA_RD:
            temp16 = (uint16_t) vm->ra ^ vm->rd;
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_XOR_RB_RA:
            temp16 = (uint16_t) vm->rb ^ vm->ra;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_XOR_RB_RB:
            temp16 = (uint16_t) vm->rb ^ vm->rb;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_XOR_RB_RC:
            temp16 = (uint16_t) vm->rb ^ vm->rc;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_XOR_RB_RD:
            temp16 = (uint16_t) vm->rb ^ vm->rd;
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_XOR_RC_RA:
            temp16 = (uint16_t) vm->rc ^ vm->ra;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_XOR_RC_RB:
            temp16 = (uint16_t) vm->rc ^ vm->rb;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_XOR_RC_RC:
            temp16 = (uint16_t) vm->rc ^ vm->rc;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_XOR_RC_RD:
            temp16 = (uint16_t) vm->rc ^ vm->rd;
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_XOR_RD_RA:
            temp16 = (uint16_t) vm->rd ^ vm->ra;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_XOR_RD_RB:
            temp16 = (uint16_t) vm->rd ^ vm->rb;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_XOR_RD_RC:
            temp16 = (uint16_t) vm->rd ^ vm->rc;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_XOR_RD_RD:
            temp16 = (uint16_t) vm->rd ^ vm->rd;
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_XOR_RA_CST:
            temp16 = (uint16_t) vm->ra ^ fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->ra = (uint8_t) temp16;
            break;
        case OP_XOR_RB_CST:
            temp16 = (uint16_t) vm->rb ^ fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->rb = (uint8_t) temp16;
            break;
        case OP_XOR_RC_CST:
            temp16 = (uint16_t) vm->rc ^ fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->rc = (uint8_t) temp16;
            break;
        case OP_XOR_RD_CST:
            temp16 = (uint16_t) vm->rd ^ fetch_byte(vm);
            set_flags(vm, temp16, false);
            vm->rd = (uint8_t) temp16;
            break;
        case OP_NOT_RA:
            vm->ra = ~vm->ra;
            set_flags(vm, vm->ra, false);
            break;
        case OP_NOT_RB:
            vm->rb = ~vm->rb;
            set_flags(vm, vm->rb, false);
            break;
        case OP_NOT_RC:
            vm->rc = ~vm->rc;
            set_flags(vm, vm->rc, false);
            break;
        case OP_NOT_RD:
            vm->rd = ~vm->rd;
            set_flags(vm, vm->rd, false);
            break;
        case OP_INC_RA:
            temp1 = (uint16_t) vm->ra + 1;
            set_flags(vm, temp1, true);
            vm->ra = temp1;
            break;
        case OP_INC_RB:
            temp1 = (uint16_t) vm->rb + 1;
            set_flags(vm, temp1, true);
            vm->rb = temp1;
            break;
        case OP_INC_RC:
            temp1 = (uint16_t) vm->rc + 1;
            set_flags(vm, temp1, true);
            vm->rc = temp1;
            break;
        case OP_INC_RD:
            temp1 = (uint16_t) vm->rd + 1;
            set_flags(vm, temp1, true);
            vm->rd = temp1;
            break;
        case OP_DEC_RA:
            temp1 = (uint16_t) vm->ra - 1;
            set_flags(vm, temp1, true);
            vm->ra = temp1;
            break;
        case OP_DEC_RB:
            temp1 = (uint16_t) vm->rb - 1;
            set_flags(vm, temp1, true);
            vm->rb = temp1;
            break;
        case OP_DEC_RC:
            temp1 = (uint16_t) vm->rc - 1;
            set_flags(vm, temp1, true);
            vm->rc = temp1;
            break;
        case OP_DEC_RD:
            temp1 = (uint16_t) vm->rd - 1;
            set_flags(vm, temp1, true);
            vm->rd = temp1;
            break;
        case OP_SHL_RA:
            vm->ra <<= 1;
            // in the current implementation, shifting RA does NOT set FLAGS
            break;
        case OP_SHR_RA:
            vm->ra >>= 1;
            break;
        case OP_JC_CST:
            temp1 = fetch_byte(vm);
            if (vm->flags & FLAGS_C) {
                vm->cspc = (vm->cspc & 0xFF00) | temp1;
            }
            break;
        case OP_JN_CST:
            temp1 = fetch_byte(vm);
            if (vm->flags & FLAGS_N) {
                vm->cspc = (vm->cspc & 0xFF00) | temp1;
            }
            break;
        case OP_JZ_CST:
            temp1 = fetch_byte(vm);
            if (vm->flags & FLAGS_Z) {
                vm->cspc = (vm->cspc & 0xFF00) | temp1;
            }
            break;
        case OP_GOTO_CST:
            temp1 = fetch_byte(vm);
            vm->cspc = (vm->cspc & 0xFF00) | temp1;
            break;
        case OP_LJC_DCST:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            if (vm->flags & FLAGS_C) {
                vm->cspc = ((uint16_t) temp1 << 8) | temp2;
            }
            break;
        case OP_LJN_DCST:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            if (vm->flags & FLAGS_N) {
                vm->cspc = ((uint16_t) temp1 << 8) | temp2;
            }
            break;
        case OP_LJZ_DCST:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            if (vm->flags & FLAGS_Z) {
                vm->cspc = ((uint16_t) temp1 << 8) | temp2;
            }
            break;
        case OP_LGOTO_DCST:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);
            vm->cspc = ((uint16_t) temp1 << 8) | temp2;
            break;
        case OP_LGOTO_RCRD:
            vm->cspc = ((uint16_t) vm->rc << 8) | vm->rd;
            break;
        case OP_LCALL_DCST:
            temp1 = fetch_byte(vm);
            temp2 = fetch_byte(vm);

            push(vm, vm->cspc >> 8);
            push(vm, vm->cspc & 0xFF);

            vm->cspc = ((uint16_t) temp1 << 8) | temp2;
            break;
        case OP_LCALL_RCRD:
            push(vm, vm->cspc >> 8);
            push(vm, vm->cspc & 0xFF);
            vm->cspc = ((uint16_t) vm->rc << 8) | vm->rd;
            break;
        case OP_RET:
            pop(vm, &temp2);
            pop(vm, &temp1);
            vm->cspc = ((uint16_t) temp1 << 8) | (uint16_t) temp2;
            break;
        case OP_RET_CST:
            pop(vm, &temp2);
            pop(vm, &temp1);
            temp16 = ((uint16_t) temp1 << 8) | temp2; // temp1 = return cspc
            temp2 = fetch_byte(vm); // temp 2: stack places to discard
            vm->sp -= temp2;
            vm->cspc = temp16;
            break;
        case OP_IRET:
            pop(vm, &vm->flags);
            pop(vm, &temp2);
            pop(vm, &temp1);
            vm->cspc = ((uint16_t) temp1 << 8) | temp2;
            break;
        case OP_CID:
            // set bit 3 of FLAGS to 0
            vm->flags &= ~FLAGS_I;
            break;
        case OP_SID:
            // set bit 3 of FLAGS to 1
            vm->flags |= FLAGS_I;
            break;
        case OP_CLC:
            // set bit 0 of FLAGS to 0
            vm->flags &= ~FLAGS_C;
            break;
        case OP_SEC:
            vm->flags |= FLAGS_C;
            break;
    }
}

void vm_destroy(struct lncpu_vm *vm) {
    // destroy emulated devices

    for (size_t i = 0; i < vm->emu_device_count; i++) {
        emu_device_destroy_func destroy = vm->emu_devices[i].destroy;

        if (destroy)
            destroy(vm, &vm->emu_devices[i], vm->emu_devices[i].data);
    }
}
