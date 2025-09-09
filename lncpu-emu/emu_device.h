//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_EMU_DEVICE_H
#define LNCPU_EMU_EMU_DEVICE_H
#include <stdbool.h>
#include <stdint.h>


struct lncpu_vm;
struct emu_device;

typedef void (*emu_device_init_func)(struct lncpu_vm * vm, struct emu_device *device);
typedef void (*emu_device_step_func)(struct lncpu_vm * vm, struct emu_device *device, void * dev_data);
typedef void (*emu_device_pause_func)(struct lncpu_vm * vm, struct emu_device *device, void * dev_data);
typedef void (*emu_device_resume_func)(struct lncpu_vm * vm, struct emu_device *device, void * dev_data);
typedef uint8_t (*emu_device_addr_read_func)(struct lncpu_vm * vm, struct emu_device *device, void * dev_data, uint16_t addr);
typedef void (*emu_device_addr_write_func)(struct lncpu_vm * vm, struct emu_device *device, void * dev_data, uint16_t addr, uint8_t value);
typedef void (*emu_device_destroy_func)(struct lncpu_vm * vm, struct emu_device *device, void * dev_data);

struct emu_device {
    uint16_t start;
    uint16_t end;
    emu_device_init_func init;
    emu_device_step_func step;
    emu_device_pause_func pause;
    emu_device_resume_func resume;
    emu_device_addr_read_func addr_read;
    emu_device_addr_write_func addr_write;
    emu_device_destroy_func destroy;
    void * data;
    bool irq_req;
};

/* EMU TTY
 * Behavior:
 * 0x0: avail on read, reset buffer on write
 * 0x1: get next char on read, ignored on write
 * 0x2: write character
 * IRQ when data is available to read
 */
#define EMU_TTY_BUFFER_SIZE 256
void emu_tty_init(struct lncpu_vm * vm, struct emu_device *device);
void emu_tty_step(struct lncpu_vm * vm, struct emu_device *device, void * dev_data);
void emu_tty_pause(struct lncpu_vm * vm, struct emu_device *device, void * dev_data);
void emu_tty_resume(struct lncpu_vm * vm, struct emu_device *device, void * dev_data);
uint8_t emu_tty_addr_read(struct lncpu_vm * vm, struct emu_device *device, void * dev_data, uint16_t addr);
void emu_tty_addr_write(struct lncpu_vm * vm, struct emu_device *device, void * dev_data, uint16_t addr, uint8_t value);
void emu_tty_destroy(struct lncpu_vm * vm, struct emu_device *device, void * dev_data);

#endif //LNCPU_EMU_EMU_DEVICE_H
