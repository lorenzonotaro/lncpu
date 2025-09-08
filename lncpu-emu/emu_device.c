//
// Created by loryn on 9/6/2025.
//

#include <conio.h>
#include "emu_device.h"

#include <bemapiset.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <windows.h>

struct emu_tty_data {
    uint8_t wptr, rptr;
    uint8_t buffer[EMU_TTY_BUFFER_SIZE];
    HANDLE g_conin;
};

void emu_tty_init(struct lncpu_vm *vm, struct emu_device *device) {
    device->data = malloc(sizeof(struct emu_tty_data));
    struct emu_tty_data *data = device->data;
    data->wptr = 0;
    device->irq_req = false;

    data->g_conin = CreateFileW(L"CONIN$", GENERIC_READ|GENERIC_WRITE,
                      FILE_SHARE_READ|FILE_SHARE_WRITE, NULL,
                      OPEN_EXISTING, 0, NULL);
    DWORD m=0; GetConsoleMode(data->g_conin, &m);
    m &= ~(ENABLE_LINE_INPUT | ENABLE_ECHO_INPUT);
    SetConsoleMode(data->g_conin, m);
}

int tty_try_char(HANDLE handle) { // -1 if none
    INPUT_RECORD rec; DWORD n=0;
    while (PeekConsoleInputW(handle, &rec, 1, &n) && n) {
        ReadConsoleInputW(handle, &rec, 1, &n);
        if (rec.EventType == KEY_EVENT && rec.Event.KeyEvent.bKeyDown) {
            WCHAR w = rec.Event.KeyEvent.uChar.UnicodeChar;
            if (w) return (unsigned char)w;
        }
    }
    return -1;
}

void emu_tty_step(struct lncpu_vm *vm, struct emu_device *device, void *dev_data) {
    int c;
    if ((c = tty_try_char(((struct emu_tty_data *)dev_data)->g_conin)) != -1) {
        printf("%c", c);
        struct emu_tty_data *data = device->data;
        data->buffer[data->rptr++] = c;
        device->irq_req = true;
    }
}

uint8_t emu_tty_addr_read(struct lncpu_vm *vm, struct emu_device *device, void *dev_data,
    uint16_t addr) {
    if (addr - device->start == 0) {
        // return whether data is available
        struct emu_tty_data *data = device->data;
        return data->wptr - data->rptr > 0 ? 1 : 0;
    }else if (addr - device->start == 1) {
        // return data
        struct emu_tty_data *data = device->data;
        uint8_t d = data->buffer[data->rptr++];
        if (data->rptr == data->wptr) {
            device->irq_req = false;
        }
        return d;
    }
    return 0;
}

void emu_tty_addr_write(struct lncpu_vm *vm, struct emu_device *device, void *dev_data,
    uint16_t addr, uint8_t value) {

    if (addr - device->start == 0) {
        // reset input buffer
        struct emu_tty_data *data = device->data;
        data->rptr = 0;
        data->wptr = 0;
    }
    else if (addr - device->start == 2) {
        // write char
        putch(value);
    }
}

void emu_tty_destroy(struct lncpu_vm *vm, struct emu_device *device, void *dev_data) {
    free(device->data);
}