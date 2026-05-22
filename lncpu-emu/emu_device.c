//
// Created by loryn on 9/6/2025.
//

#ifdef _WIN32
    #include <conio.h>
    #include <windows.h>

#else
    #include <unistd.h>
    #include <termios.h>
    #include <fcntl.h>
    #include <errno.h>
#endif
#include "emu_device.h"

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#define EMU_TTY_SIGNATURE "LNDI\x01\x08\x00\x20\x00\x00\x00\x00\x00\x00\x00\xA5"

struct emu_tty_data {
    uint16_t wptr, rptr;
    uint8_t buffer[EMU_TTY_BUFFER_SIZE];
#ifdef _WIN32
    DWORD orig_mode;
    DWORD direct_mode;
    HANDLE g_conin;
#else 
    struct termios orig_termios;
#endif
};

void emu_tty_init(struct lncpu_vm *vm, struct emu_device *device) {
    device->data = malloc(sizeof(struct emu_tty_data));
    struct emu_tty_data *data = device->data;
    data->wptr = 0;
    data->rptr = 0;
    device->irq_req = false;
#ifdef _WIN32
    data->g_conin = CreateFileW(L"CONIN$", GENERIC_READ|GENERIC_WRITE,
                      FILE_SHARE_READ|FILE_SHARE_WRITE, NULL,
                      OPEN_EXISTING, 0, NULL);
    DWORD m=0;
    GetConsoleMode(data->g_conin, &m);
    data->orig_mode = m;
    m &= ~(ENABLE_LINE_INPUT | ENABLE_ECHO_INPUT);
    data->direct_mode = m;
    SetConsoleMode(data->g_conin, m);
#else
    emu_tty_resume(vm, device, data);
#endif
}

#ifdef _WIN32
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
#endif

void emu_tty_pause(struct lncpu_vm *vm, struct emu_device *device, void *dev_data) {
    struct emu_tty_data *data = device->data;
    #ifdef _WIN32
    SetConsoleMode(data->g_conin, data->orig_mode);
    #else
    tcsetattr(STDIN_FILENO, TCSANOW, &data->orig_termios);
    int flags = fcntl(STDIN_FILENO, F_GETFL, 0);
    fcntl(STDIN_FILENO, F_SETFL, flags & ~O_NONBLOCK);
    fflush(stdout);
    tcdrain(STDOUT_FILENO);
    #endif
}

void emu_tty_resume(struct lncpu_vm *vm, struct emu_device *device, void *dev_data) {
    struct emu_tty_data *data = device->data;
    #ifdef _WIN32
    SetConsoleMode(data->g_conin, data->direct_mode);
    #else
    tcgetattr(STDIN_FILENO, &data->orig_termios);
    struct termios newt = data->orig_termios;
    newt.c_lflag &= ~(ICANON | ECHO);
    newt.c_cc[VMIN] = 0;
    newt.c_cc[VTIME] = 0;
    tcsetattr(STDIN_FILENO, TCSANOW, &newt);
    setvbuf(stdin, NULL, _IONBF, 0);

    int flags = fcntl(STDIN_FILENO, F_GETFL, 0);
    fcntl(STDIN_FILENO, F_SETFL, flags | O_NONBLOCK);
    #endif
}

void emu_tty_step(struct lncpu_vm *vm, struct emu_device *device, void *dev_data) {
    int c;
#ifdef _WIN32
    if ((c = tty_try_char(((struct emu_tty_data *)dev_data)->g_conin)) != -1) {
        struct emu_tty_data *data = (struct emu_tty_data *)dev_data;
        data->buffer[data->wptr] = (uint8_t)c;
        data->wptr = (data->wptr + 1) % EMU_TTY_BUFFER_SIZE;
        device->irq_req = true;
    }
#else
    unsigned char ch;
    ssize_t r = read(STDIN_FILENO, &ch, 1);
    if (r == 1) {
        struct emu_tty_data *data = (struct emu_tty_data *)dev_data;
        data->buffer[data->wptr] = ch;
        data->wptr = (data->wptr + 1) % EMU_TTY_BUFFER_SIZE;
        device->irq_req = true;
    } else if (r == -1) {
        if (errno != EAGAIN && errno != EWOULDBLOCK) {
            // non-recoverable read error - ignore for now
        }
    }
#endif
}

uint8_t emu_tty_addr_read(struct lncpu_vm *vm, struct emu_device *device, void *dev_data,
    uint16_t addr) {
    if (addr - device->start == 0) {
        // return whether data is available
        struct emu_tty_data *data = device->data;
        uint16_t available = (data->wptr + EMU_TTY_BUFFER_SIZE - data->rptr) % EMU_TTY_BUFFER_SIZE;
        return available > 0 ? 1 : 0;
    }else if (addr - device->start == 1) {
        // return data
        struct emu_tty_data *data = device->data;
        uint8_t d = data->buffer[data->rptr];
        data->rptr = (data->rptr + 1) % EMU_TTY_BUFFER_SIZE;
        uint16_t available = (data->wptr + EMU_TTY_BUFFER_SIZE - data->rptr) % EMU_TTY_BUFFER_SIZE;
        if (available == 0) {
            device->irq_req = false;
        }
        return d;
    }else if (addr - device->start >= 0x1ff0 && addr - device->start <= 0x1fff) {
        // signature
        return EMU_TTY_SIGNATURE[addr - device->start - 0x1ff0];
    }
    return 0;
}

void putch(int c){
    #ifdef _WIN32
    char ch = (char)c;
    HANDLE h = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD written;
    WriteConsoleA(h, &ch, 1, &written, NULL);
    #else
    char ch = (char)c;
    ssize_t r = write(STDOUT_FILENO, &ch, 1);
    (void)r;
    tcdrain(STDOUT_FILENO);
    #endif
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