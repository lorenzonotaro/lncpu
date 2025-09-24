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

#define EMU_TTY_SIGNATURE "LNDI\x01\x08\x00\x20\x00\x00\x00\x00\x00\x00\x00\xA5"

struct emu_tty_data {
    uint8_t wptr, rptr;
    uint8_t buffer[EMU_TTY_BUFFER_SIZE];
    DWORD orig_mode;
    DWORD direct_mode;
    HANDLE g_conin;
};

void emu_tty_init(struct lncpu_vm *vm, struct emu_device *device) {
    device->data = malloc(sizeof(struct emu_tty_data));
    struct emu_tty_data *data = device->data;
    data->wptr = 0;
    data->rptr = 0;
    device->irq_req = false;

    data->g_conin = CreateFileW(L"CONIN$", GENERIC_READ|GENERIC_WRITE,
                      FILE_SHARE_READ|FILE_SHARE_WRITE, NULL,
                      OPEN_EXISTING, 0, NULL);
    DWORD m=0;
    GetConsoleMode(data->g_conin, &m);
    data->orig_mode = m;
    m &= ~(ENABLE_LINE_INPUT | ENABLE_ECHO_INPUT);
    data->direct_mode = m;
    SetConsoleMode(data->g_conin, m);
}

int tty_try_char(HANDLE handle) { // -1 if none
    INPUT_RECORD rec; DWORD n=0;
    while (PeekConsoleInputW(handle, &rec, 1, &n) && n) {
        ReadConsoleInputW(handle, &rec, 1, &n);
        if (rec.EventType == KEY_EVENT && rec.Event.KeyEvent.bKeyDown) {
            WCHAR w = rec.Event.KeyEvent.uChar.UnicodeChar;
            if (w == '\r') w = '\n';
            if (w) return (unsigned char)w;
        }
    }
    return -1;
}

void emu_tty_pause(struct lncpu_vm *vm, struct emu_device *device, void *dev_data) {
    struct emu_tty_data *data = device->data;
    SetConsoleMode(data->g_conin, data->orig_mode);
}

void emu_tty_resume(struct lncpu_vm *vm, struct emu_device *device, void *dev_data) {
    struct emu_tty_data *data = device->data;
    SetConsoleMode(data->g_conin, data->direct_mode);
}

void emu_tty_step(struct lncpu_vm *vm, struct emu_device *device, void *dev_data) {
    int c;
    if ((c = tty_try_char(((struct emu_tty_data *)dev_data)->g_conin)) != -1) {
        struct emu_tty_data *data = device->data;
        data->buffer[data->wptr++] = c;
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
    }else if (addr - device->start >= 0x1ff0 && addr - device->start <= 0x1fff) {
        // signature
        return EMU_TTY_SIGNATURE[addr - device->start - 0x1ff0];
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
        if (value == '\b') {
            putch('\b');
            putch(' ');
            putch('\b');
        }else {
            putch(value);
        }
    }
}

void emu_tty_destroy(struct lncpu_vm *vm, struct emu_device *device, void *dev_data) {
    free(device->data);
}