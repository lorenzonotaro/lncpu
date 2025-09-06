//
// Created by loryn on 9/6/2025.
//

#include "vm.h"

#include <stdio.h>
#include "linker_config/link_target.h"
#include "linker_config/linker_config.h"
#include "linker_config/linker_config_parser.h"

static bool init_emu_devices(struct lncpu_vm *vm, const struct emu_cmdline_params * cmdline_params) {
    if (cmdline_params->linker_config_file) {
        LinkerConfig lc;
        FILE *f = fopen(cmdline_params->linker_config_file, "r");
        char *cfg_contents = NULL;
        if (f) {
            fseek(f, 0, SEEK_END);
            const size_t size = ftell(f);
            fseek(f, 0, SEEK_SET);
            fread(cfg_contents, 1, size, f);
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
    }
    return true;
}

bool try_load_device(LinkTarget lt, const char * file, uint8_t * addr_space) {
    FILE *f = fopen(file, "r");

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
    fread(addr_space + target_info->start, 1, size, f);

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

bool init_vm(struct lncpu_vm *vm, const struct emu_cmdline_params *cmdline_params) {
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

    if (!init_addr_space(vm->addr_space, cmdline_params)) {
        return false;
    }

    return init_emu_devices(vm, cmdline_params);
}
