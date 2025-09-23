//
// Created by loryn on 9/6/2025.
//

#include "emu.h"

#include <ctype.h>
#include <io.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <windows.h>

#include "utlist.h"
#include "vm.h"
#include "config/cmdline.h"

static struct emulator *emulator;

static void set_emu(struct emulator *emu) {
    emulator = emu;
}

struct emulator *get_emu(void) {
    return emulator;
}


char line_buffer[1024];
void help(void) {
    printf("Commands:\n");
    printf("c, continue - Continue execution\n");
    printf("s, step - Step one instruction\n");
    printf("o, stepover - Step over call instruction (not implemented) \n"),
    printf("r, register" " - Print the current register values\n");
    printf("XXXX (hex), XXXX-XXXX (hex) - Print memory dump from address XXXX to XXXX\n");
    printf("h, help - Print this help message\n");
    printf("q, quit - Quit the emulator\n");
    printf("b, break <address> - Set a breakpoint at the given address\n");
}

void reg_dump(struct lncpu_vm *vm, FILE *f) {
    fprintf(f, "\nRA    RB    RC    RD\n");
    fprintf(f, "%02x    %02x    %02x    %02x\n\n" , vm->ra, vm->rb, vm->rc, vm->rd);
    fprintf(f, "DS    SS    SP    BP\n");
    fprintf(f, "%02x    %02x    %02x    %02x\n\n" , vm->ds, vm->ss, vm->sp, vm->bp);
    fprintf(f,"CSPC        FLAGS (INZC)\n");
    fprintf(f, "%04x        %02x    (%c%c%c%c)\n\n" , vm->cspc, vm->flags,
           (vm->flags & FLAGS_I) ? 'I' : '-',
           (vm->flags & FLAGS_N) ? 'N' : '-',
           (vm->flags & FLAGS_Z) ? 'Z' : '-',
           (vm->flags & FLAGS_C) ? 'C' : '-');
}

void memdump(struct lncpu_vm * vm, uint16_t start, uint16_t end) {
    // floor to last 16-byte memory alignment
    uint16_t i = start & ~0xF;
    puts("\n");
    while (i <= end) {
        printf("%04x: ", i);
        for (int j = 0; j < 16; j++) {
            if (i >= start && i <= end) {
                printf("%02x ", vm->addr_space[i]);
            } else {
                printf("   ");
            }
            i += 1;
        }
        printf("\n");
    }
}

void memdump_bin(struct lncpu_vm *vm, const char *filename) {
    FILE *f = fopen(filename, "wb");
    if (f == NULL) {
        fprintf(stderr, "Failed to open file %s\n", filename);
        return;
    }
    fwrite(vm->addr_space, 1, 0x10000, f);
    fclose(f);
}

void pause(struct emulator *emu) {

    if (emu->status != EMU_STATUS_TERMINATED) {
        printf("==== LNCPU paused at 0x%04x ====\n\n", emu->vm.cspc);
    }

    emu->status = EMU_STATUS_PAUSED;

    bool loop = true;
    do {
        printf("> ");

        if (!fgets(line_buffer, sizeof(line_buffer), stdin)) {
            continue;
        }

        char *ptr = strtok(line_buffer, "\n");

        ptr = strtok(ptr, " ");

        if (!ptr) {
            continue;
        }

        if (strcmp(ptr, "c") == 0 || strcmp(ptr, "continue") == 0) {
            emu->status = EMU_STATUS_RUNNING;
            loop = false;
        } else if (strcmp(ptr, "s") == 0 || strcmp(ptr, "step") == 0) {
            emu->status = EMU_STATUS_STEPPING;
            loop = false;
        } else if (strcmp(ptr, "o") == 0 || strcmp(ptr, "stepover") == 0) {
            printf("Stepping over is not yet implemented. Stepping.");
            emu->status = EMU_STATUS_STEPPING;
        }else if (strcmp(ptr, "h") == 0 || strcmp(ptr, "help") == 0) {
            help();
        } else if (strcmp(ptr, "q") == 0 || strcmp(ptr, "quit") == 0) {
            emu->status = EMU_STATUS_TERMINATED;
            loop = false;
        } else if (strcmp(ptr, "r") == 0 || strcmp(ptr, "register") == 0) {
            reg_dump(&emu->vm, stdout);
        }else if (strlen(ptr) == 4 && isxdigit(ptr[0]) && isxdigit(ptr[1]) && isxdigit(ptr[2]) && isxdigit(ptr[3])) {
            uint16_t start = (uint16_t) strtol(ptr, NULL, 16);
            memdump(&emu->vm, start, start);
        }else if (strlen(ptr) == 9 && isxdigit(ptr[0]) && isxdigit(ptr[1]) && isxdigit(ptr[2]) && isxdigit(ptr[3])
                  && ptr[4] == '-' && isxdigit(ptr[5]) && isxdigit(ptr[6]) && isxdigit(ptr[7]) && isxdigit(ptr[8])) {
            char *end_ptr;
            uint16_t start = (uint16_t) strtol(ptr, &end_ptr, 16);
            if (*end_ptr != '-') {
                printf("Invalid memory range\n");
                continue;
            }
            end_ptr++;
            uint16_t end = (uint16_t) strtol(end_ptr, NULL, 16);
            if (end < start) {
                printf("Invalid memory range\n");
                continue;
            }
            memdump(&emu->vm, start, end);
        }else if (strcmp(ptr, "b") == 0 || strcmp(ptr, "break") == 0) {
            char *addr_str = strtok(NULL, " ");
            char *end_ptr;
            if (addr_str) {
                uint16_t addr = (uint16_t) strtol(addr_str, &end_ptr, 16);
                if (end_ptr == addr_str || *end_ptr != '\0') {
                    printf("Invalid address\n");
                    continue;
                }
                struct bp *bp = malloc(sizeof(struct bp));
                bp->addr = addr;
                LL_APPEND(emu->bp_list, bp);
                printf("Breakpoint set at 0x%04x\n", addr);
            } else {
                printf("Usage: b <address>\n");
            }
        } else {
            printf("Unknown command. Type 'h' or 'help' for a list of commands.\n");
        }
    }while (loop);

}

bool bp_hit(const struct emulator *emu) {
    struct bp *bp;
    LL_FOREACH(emu->bp_list, bp) {
        if (bp->addr == emu->vm.cspc) {
            return true;
        }
    }
    return false;
}

int run_emu(const struct emu_cmdline_params *cmdline_params) {
    struct emulator emu = {
        .status = EMU_STATUS_RUNNING,
        .bp_list = NULL
    };

    set_emu(&emu);

    struct lncpu_vm *vm = &emu.vm;

    if (!vm_init(vm, cmdline_params)) {
        fprintf(stderr, "Vm init failed. Exiting.\n");
        return 1;
    }

    if (cmdline_params->pause_on_start) {

        for (int i = 0; i < vm->emu_device_count; i++) {
            if (vm->emu_devices[i].pause) {
                vm->emu_devices[i].pause(vm, &vm->emu_devices[i], vm->emu_devices[i].data);
            }
        }

        pause(&emu);

        for (int i = 0; i < vm->emu_device_count; i++) {
            if (vm->emu_devices[i].resume) {
                vm->emu_devices[i].resume(vm, &vm->emu_devices[i], vm->emu_devices[i].data);
            }
        }
    }
    
    while (!vm->halted && emu.status != EMU_STATUS_TERMINATED) {

        for (int i = 0; i < vm->emu_device_count; i++) {
            if (vm->emu_devices[i].step) {
                vm->emu_devices[i].step(vm, &vm->emu_devices[i], vm->emu_devices[i].data);
            }
        }

        vm_step(vm);

        if (emu.status == EMU_STATUS_PAUSED || emu.status == EMU_STATUS_STEPPING || bp_hit(&emu)) {
            for (int i = 0; i < vm->emu_device_count; i++) {
                if (vm->emu_devices[i].pause) {
                    vm->emu_devices[i].pause(vm, &vm->emu_devices[i], vm->emu_devices[i].data);
                }
            }
            pause(&emu);

            for (int i = 0; i < vm->emu_device_count; i++) {
                if (vm->emu_devices[i].resume) {
                    vm->emu_devices[i].resume(vm, &vm->emu_devices[i], vm->emu_devices[i].data);
                }
            }
        }
    }

    emu.status = EMU_STATUS_TERMINATED;

    if (vm->halted && !cmdline_params->no_pause_on_halt){
        printf("LNCPU has halted. Type 'c' or 'continue' to exit. \n");
        pause(&emu);
    }

    if (cmdline_params->dump_status != NULL) {
        FILE *f = fopen(cmdline_params->dump_status, "w");

        if (!f) {
            fprintf(stderr, "Failed to open file %s\n", cmdline_params->dump_status);

        }else {
            reg_dump(vm, f);
        }
    }

    if (cmdline_params->dump_address_space != NULL) {
        memdump_bin(vm, cmdline_params->dump_address_space);
    }

    // delete breakpoints
    struct bp *bp = emu.bp_list;
    while (bp != NULL) {
        struct bp *next = bp->next;
        free(bp);
        bp = next;
    }

    vm_destroy(vm);
    set_emu(NULL);

    return 0;
}
