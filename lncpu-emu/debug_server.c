#include "debug_server.h"

#include <ctype.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
typedef SOCKET lndbg_socket;
#define LNDBG_INVALID_SOCKET INVALID_SOCKET
#define lndbg_close closesocket
#else
#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <unistd.h>
typedef int lndbg_socket;
#define LNDBG_INVALID_SOCKET (-1)
#define lndbg_close close
#endif

#include "emu.h"
#include "opcodes.h"
#include "utlist.h"
#include "vm.h"

#define LNDBG_FRAME_CAPACITY 131200

struct lndbg_call_frame {
    uint16_t return_pc;
    uint8_t caller_ss;
    uint8_t caller_sp;
};

struct lndbg_server {
    lndbg_socket listener;
    lndbg_socket client;
    char *frames;
    size_t frames_used;
    bool disconnected;
    bool devices_paused;
    bool skip_breakpoint_once;
    const char *pending_stop_reason;
    struct lndbg_call_frame *call_frames;
    size_t call_frame_count;
    size_t call_frame_capacity;
    size_t step_out_depth;
    bool call_frames_valid;
};

static bool socket_would_block(void) {
#ifdef _WIN32
    int error = WSAGetLastError();
    return error == WSAEWOULDBLOCK || error == WSAEINTR;
#else
    return errno == EAGAIN || errno == EWOULDBLOCK || errno == EINTR;
#endif
}

static bool set_nonblocking(lndbg_socket socket_fd) {
#ifdef _WIN32
    u_long enabled = 1;
    return ioctlsocket(socket_fd, FIONBIO, &enabled) == 0;
#else
    int flags = fcntl(socket_fd, F_GETFL, 0);
    return flags >= 0 && fcntl(socket_fd, F_SETFL, flags | O_NONBLOCK) == 0;
#endif
}

static bool wait_readable(lndbg_socket socket_fd) {
    fd_set read_set;
    FD_ZERO(&read_set);
    FD_SET(socket_fd, &read_set);
#ifdef _WIN32
    return select(0, &read_set, NULL, NULL, NULL) > 0;
#else
    return select(socket_fd + 1, &read_set, NULL, NULL, NULL) > 0;
#endif
}

static bool wait_writable(lndbg_socket socket_fd) {
    fd_set write_set;
    FD_ZERO(&write_set);
    FD_SET(socket_fd, &write_set);
#ifdef _WIN32
    return select(0, NULL, &write_set, NULL, NULL) > 0;
#else
    return select(socket_fd + 1, NULL, &write_set, NULL, NULL) > 0;
#endif
}

static bool send_all(struct lndbg_server *server, const char *data, size_t length) {
    size_t sent = 0;
    while (sent < length) {
        int chunk = send(server->client, data + sent, (int)(length - sent), 0);
        if (chunk > 0) {
            sent += (size_t)chunk;
            continue;
        }
        if (chunk < 0 && socket_would_block() && wait_writable(server->client)) {
            continue;
        }
        server->disconnected = true;
        return false;
    }
    return true;
}

static bool send_format(struct lndbg_server *server, const char *format, ...) {
    char stack_buffer[512];
    va_list args;
    va_start(args, format);
    int required = vsnprintf(stack_buffer, sizeof(stack_buffer), format, args);
    va_end(args);
    if (required < 0) return false;
    if ((size_t)required < sizeof(stack_buffer)) {
        return send_all(server, stack_buffer, (size_t)required);
    }

    char *buffer = malloc((size_t)required + 1);
    if (!buffer) return false;
    va_start(args, format);
    vsnprintf(buffer, (size_t)required + 1, format, args);
    va_end(args);
    bool result = send_all(server, buffer, (size_t)required);
    free(buffer);
    return result;
}

static void send_event(struct lndbg_server *server, const char *reason, uint16_t pc) {
    send_format(server, "! stopped %s %04x\n", reason, pc);
}

static void pause_devices(struct emulator *emu, struct lndbg_server *server) {
    if (server->devices_paused) return;
    for (size_t i = 0; i < emu->vm.emu_device_count; i++) {
        if (emu->vm.emu_devices[i].pause) {
            emu->vm.emu_devices[i].pause(&emu->vm, &emu->vm.emu_devices[i], emu->vm.emu_devices[i].data);
        }
    }
    server->devices_paused = true;
}

static void resume_devices(struct emulator *emu, struct lndbg_server *server) {
    if (!server->devices_paused) return;
    for (size_t i = 0; i < emu->vm.emu_device_count; i++) {
        if (emu->vm.emu_devices[i].resume) {
            emu->vm.emu_devices[i].resume(&emu->vm, &emu->vm.emu_devices[i], emu->vm.emu_devices[i].data);
        }
    }
    server->devices_paused = false;
}

static void step_devices(struct emulator *emu) {
    for (size_t i = 0; i < emu->vm.emu_device_count; i++) {
        if (emu->vm.emu_devices[i].step) {
            emu->vm.emu_devices[i].step(&emu->vm, &emu->vm.emu_devices[i], emu->vm.emu_devices[i].data);
        }
    }
}

static bool parse_hex(const char *text, unsigned long maximum, unsigned long *value) {
    if (!text || !*text) return false;
    if (text[0] == '0' && (text[1] == 'x' || text[1] == 'X')) text += 2;
    if (!*text) return false;
    unsigned long parsed = 0;
    for (; *text; text++) {
        unsigned digit;
        if (*text >= '0' && *text <= '9') digit = (unsigned)(*text - '0');
        else if (*text >= 'a' && *text <= 'f') digit = (unsigned)(*text - 'a' + 10);
        else if (*text >= 'A' && *text <= 'F') digit = (unsigned)(*text - 'A' + 10);
        else return false;
        if (parsed > (maximum - digit) / 16) return false;
        parsed = parsed * 16 + digit;
    }
    *value = parsed;
    return true;
}

static bool parse_length(const char *text, unsigned long *value) {
    if (!text || !*text) return false;
    int base = 10;
    if (text[0] == '0' && (text[1] == 'x' || text[1] == 'X')) {
        base = 16;
        text += 2;
    }
    if (!*text) return false;
    char *end = NULL;
    unsigned long parsed = strtoul(text, &end, base);
    if (!end || *end != '\0' || parsed > 0x10000UL) return false;
    *value = parsed;
    return true;
}

static bool breakpoint_hit(const struct emulator *emu) {
    struct bp *breakpoint;
    LL_FOREACH(emu->bp_list, breakpoint) {
        if (breakpoint->addr == emu->vm.cspc) return true;
    }
    return false;
}

static void breakpoint_set(struct emulator *emu, uint16_t address) {
    struct bp *breakpoint;
    LL_FOREACH(emu->bp_list, breakpoint) {
        if (breakpoint->addr == address) return;
    }
    breakpoint = malloc(sizeof(*breakpoint));
    if (!breakpoint) return;
    breakpoint->addr = address;
    breakpoint->next = NULL;
    LL_APPEND(emu->bp_list, breakpoint);
}

static void breakpoint_clear(struct emulator *emu, uint16_t address) {
    struct bp *breakpoint;
    struct bp *temporary;
    LL_FOREACH_SAFE(emu->bp_list, breakpoint, temporary) {
        if (breakpoint->addr == address) {
            LL_DELETE(emu->bp_list, breakpoint);
            free(breakpoint);
            return;
        }
    }
}

static void breakpoint_clear_all(struct emulator *emu) {
    struct bp *breakpoint = emu->bp_list;
    while (breakpoint) {
        struct bp *next = breakpoint->next;
        free(breakpoint);
        breakpoint = next;
    }
    emu->bp_list = NULL;
}

static void uppercase(char *text) {
    for (; *text; text++) *text = (char)toupper((unsigned char)*text);
}

static bool command_allowed_while_running(const char *command) {
    return strcmp(command, "hello") == 0 || strcmp(command, "bp") == 0 ||
           strcmp(command, "pause") == 0 || strcmp(command, "quit") == 0;
}

static void invalidate_call_frames(struct lndbg_server *server) {
    server->call_frame_count = 0;
    server->call_frames_valid = false;
}

static bool append_call_frame(struct lndbg_server *server, uint16_t return_pc,
                              uint8_t caller_ss, uint8_t caller_sp) {
    if (!server->call_frames_valid) {
        server->call_frame_count = 0;
        server->call_frames_valid = true;
    }
    if (server->call_frame_count == server->call_frame_capacity) {
        size_t capacity = server->call_frame_capacity == 0 ? 16 : server->call_frame_capacity * 2;
        struct lndbg_call_frame *frames = realloc(server->call_frames, capacity * sizeof(*frames));
        if (!frames) {
            invalidate_call_frames(server);
            return false;
        }
        server->call_frames = frames;
        server->call_frame_capacity = capacity;
    }
    server->call_frames[server->call_frame_count++] =
        (struct lndbg_call_frame){return_pc, caller_ss, caller_sp};
    return true;
}

static void track_call_stack(struct lndbg_server *server, const struct lncpu_vm *vm,
                             uint8_t opcode, uint16_t instruction_pc,
                             uint8_t initial_ss, uint8_t initial_sp, uint8_t return_discard) {
    if (opcode == OP_LCALL_DCST || opcode == OP_LCALL_RCRD) {
        uint8_t length = opcode == OP_LCALL_DCST ? 3 : 1;
        if (vm->ss == initial_ss && vm->sp == (uint8_t)(initial_sp + 2)) {
            append_call_frame(server, (uint16_t)(instruction_pc + length), initial_ss, initial_sp);
        }
        return;
    }
    if (opcode != OP_RET && opcode != OP_RET_CST) return;
    if (!server->call_frames_valid || server->call_frame_count == 0) {
        invalidate_call_frames(server);
        return;
    }
    struct lndbg_call_frame frame = server->call_frames[server->call_frame_count - 1];
    uint8_t expected_sp = frame.caller_sp;
    if (opcode == OP_RET_CST) expected_sp = (uint8_t)(expected_sp - return_discard);
    if (vm->cspc != frame.return_pc || vm->ss != frame.caller_ss || vm->sp != expected_sp) {
        invalidate_call_frames(server);
        return;
    }
    server->call_frame_count--;
}

static void reply_error(struct lndbg_server *server, const char *sequence, const char *message) {
    send_format(server, "%s err %s\n", sequence, message);
}

static void handle_regs(struct lndbg_server *server, const char *sequence, const struct lncpu_vm *vm) {
    send_format(server,
        "%s ok RA=%02x RB=%02x RC=%02x RD=%02x DS=%02x SS=%02x SP=%02x BP=%02x FLAGS=%02x CSPC=%04x INSTRUCTIONS=%llx CYCLES=%llx\n",
        sequence, vm->ra, vm->rb, vm->rc, vm->rd, vm->ds, vm->ss, vm->sp, vm->bp,
        vm->flags, vm->cspc, (unsigned long long)vm->instr_count, (unsigned long long)vm->cycle_count);
}

static bool set_register(struct lncpu_vm *vm, char *name, unsigned long value) {
    uppercase(name);
    if (strcmp(name, "RA") == 0 && value <= UINT8_MAX) vm->ra = (uint8_t)value;
    else if (strcmp(name, "RB") == 0 && value <= UINT8_MAX) vm->rb = (uint8_t)value;
    else if (strcmp(name, "RC") == 0 && value <= UINT8_MAX) vm->rc = (uint8_t)value;
    else if (strcmp(name, "RD") == 0 && value <= UINT8_MAX) vm->rd = (uint8_t)value;
    else if (strcmp(name, "DS") == 0 && value <= UINT8_MAX) vm->ds = (uint8_t)value;
    else if (strcmp(name, "SS") == 0 && value <= UINT8_MAX) vm->ss = (uint8_t)value;
    else if (strcmp(name, "SP") == 0 && value <= UINT8_MAX) vm->sp = (uint8_t)value;
    else if (strcmp(name, "BP") == 0 && value <= UINT8_MAX) vm->bp = (uint8_t)value;
    else if (strcmp(name, "FLAGS") == 0 && value <= UINT8_MAX) vm->flags = (uint8_t)value;
    else if ((strcmp(name, "CSPC") == 0 || strcmp(name, "CS_PC") == 0) && value <= UINT16_MAX) vm->cspc = (uint16_t)value;
    else return false;
    return true;
}

static void handle_readmem(struct lndbg_server *server, const char *sequence,
                           const struct lncpu_vm *vm, const char *address_text, const char *length_text) {
    unsigned long address;
    unsigned long length;
    if (!parse_hex(address_text, UINT16_MAX, &address) || !parse_length(length_text, &length) ||
        address + length > 0x10000UL) {
        reply_error(server, sequence, "invalid memory range");
        return;
    }
    size_t prefix_length = strlen(sequence) + 4;
    size_t response_length = prefix_length + (size_t)length * 2 + 1;
    char *response = malloc(response_length + 1);
    if (!response) {
        reply_error(server, sequence, "out of memory");
        return;
    }
    int written = sprintf(response, "%s ok ", sequence);
    char *output = response + written;
    static const char digits[] = "0123456789abcdef";
    for (unsigned long i = 0; i < length; i++) {
        uint8_t byte = vm->addr_space[address + i];
        *output++ = digits[byte >> 4];
        *output++ = digits[byte & 0x0f];
    }
    *output++ = '\n';
    *output = '\0';
    send_all(server, response, (size_t)(output - response));
    free(response);
}

static void handle_writemem(struct lndbg_server *server, const char *sequence,
                            struct lncpu_vm *vm, const char *address_text, const char *bytes) {
    unsigned long address;
    size_t hex_length = bytes ? strlen(bytes) : 0;
    if (!parse_hex(address_text, UINT16_MAX, &address) || !bytes || (hex_length & 1) != 0 ||
        address + hex_length / 2 > 0x10000UL) {
        reply_error(server, sequence, "invalid memory write");
        return;
    }
    for (size_t i = 0; i < hex_length; i += 2) {
        char pair[3] = {bytes[i], bytes[i + 1], '\0'};
        unsigned long value;
        if (!parse_hex(pair, UINT8_MAX, &value)) {
            reply_error(server, sequence, "invalid hex bytes");
            return;
        }
    }
    for (size_t i = 0; i < hex_length; i += 2) {
        char pair[3] = {bytes[i], bytes[i + 1], '\0'};
        unsigned long value;
        parse_hex(pair, UINT8_MAX, &value);
        vm->addr_space[address + i / 2] = (uint8_t)value;
    }
    send_format(server, "%s ok\n", sequence);
}

static void handle_command(struct lndbg_server *server, struct emulator *emu, char *line) {
    char *sequence = strtok(line, " \t");
    char *command = strtok(NULL, " \t");
    if (!sequence || !command) {
        send_format(server, "0 err invalid request\n");
        return;
    }
    for (const char *cursor = sequence; *cursor; cursor++) {
        if (!isdigit((unsigned char)*cursor)) {
            send_format(server, "0 err invalid sequence\n");
            return;
        }
    }
    if ((emu->status == EMU_STATUS_RUNNING || emu->status == EMU_STATUS_STEPPING_OVER ||
         emu->status == EMU_STATUS_STEPPING_OUT ||
         emu->status == EMU_STATUS_STEPPING) && !command_allowed_while_running(command)) {
        reply_error(server, sequence, "target is running");
        return;
    }

    if (strcmp(command, "hello") == 0) {
        send_format(server, "%s ok LNDBG 1\n", sequence);
    } else if (strcmp(command, "bp") == 0) {
        char *operation = strtok(NULL, " \t");
        char *address_text = strtok(NULL, " \t");
        unsigned long address;
        if (!operation) {
            reply_error(server, sequence, "usage: bp set|clear|clearall [address]");
        } else if (strcmp(operation, "clearall") == 0 && !address_text) {
            breakpoint_clear_all(emu);
            send_format(server, "%s ok\n", sequence);
        } else if ((!address_text || !parse_hex(address_text, UINT16_MAX, &address)) || strtok(NULL, " \t")) {
            reply_error(server, sequence, "invalid breakpoint address");
        } else if (strcmp(operation, "set") == 0) {
            breakpoint_set(emu, (uint16_t)address);
            send_format(server, "%s ok\n", sequence);
        } else if (strcmp(operation, "clear") == 0) {
            breakpoint_clear(emu, (uint16_t)address);
            send_format(server, "%s ok\n", sequence);
        } else {
            reply_error(server, sequence, "invalid breakpoint operation");
        }
    } else if (strcmp(command, "continue") == 0) {
        if (strtok(NULL, " \t")) reply_error(server, sequence, "continue takes no arguments");
        else {
            server->skip_breakpoint_once = breakpoint_hit(emu);
            emu->status = EMU_STATUS_RUNNING;
            send_format(server, "%s ok\n", sequence);
        }
    } else if (strcmp(command, "step") == 0) {
        if (strtok(NULL, " \t")) reply_error(server, sequence, "step takes no arguments");
        else {
            server->skip_breakpoint_once = breakpoint_hit(emu);
            emu->status = EMU_STATUS_STEPPING;
            send_format(server, "%s ok\n", sequence);
        }
    } else if (strcmp(command, "stepover") == 0) {
        if (strtok(NULL, " \t")) reply_error(server, sequence, "stepover takes no arguments");
        else {
            uint8_t length = vm_next_call_instr_length(&emu->vm);
            server->skip_breakpoint_once = breakpoint_hit(emu);
            if (length == 0) emu->status = EMU_STATUS_STEPPING;
            else {
                emu->status = EMU_STATUS_STEPPING_OVER;
                emu->step_over_target_sssp = (uint16_t)((uint16_t)emu->vm.ss << 8) | emu->vm.sp;
                emu->step_over_target_addr = (uint16_t)(emu->vm.cspc + length);
            }
            send_format(server, "%s ok\n", sequence);
        }
    } else if (strcmp(command, "stepout") == 0) {
        if (strtok(NULL, " \t")) reply_error(server, sequence, "stepout takes no arguments");
        else if (!server->call_frames_valid || server->call_frame_count == 0) {
            reply_error(server, sequence, "no representable return frame");
        } else {
            server->skip_breakpoint_once = breakpoint_hit(emu);
            server->step_out_depth = server->call_frame_count;
            emu->status = EMU_STATUS_STEPPING_OUT;
            send_format(server, "%s ok\n", sequence);
        }
    } else if (strcmp(command, "pause") == 0) {
        if (strtok(NULL, " \t")) reply_error(server, sequence, "pause takes no arguments");
        else {
            bool was_running = emu->status != EMU_STATUS_PAUSED;
            emu->status = EMU_STATUS_PAUSED;
            if (was_running) server->pending_stop_reason = "pause";
            send_format(server, "%s ok\n", sequence);
        }
    } else if (strcmp(command, "regs") == 0) {
        if (strtok(NULL, " \t")) reply_error(server, sequence, "regs takes no arguments");
        else handle_regs(server, sequence, &emu->vm);
    } else if (strcmp(command, "setreg") == 0) {
        char *name = strtok(NULL, " \t");
        char *value_text = strtok(NULL, " \t");
        unsigned long value;
        if (!name || !value_text || strtok(NULL, " \t") || !parse_hex(value_text, UINT16_MAX, &value) ||
            !set_register(&emu->vm, name, value)) {
            reply_error(server, sequence, "invalid or immutable register");
        } else {
            if (strcmp(name, "SS") == 0 || strcmp(name, "SP") == 0 ||
                strcmp(name, "CSPC") == 0 || strcmp(name, "CS_PC") == 0) {
                invalidate_call_frames(server);
            }
            send_format(server, "%s ok\n", sequence);
        }
    } else if (strcmp(command, "readmem") == 0) {
        char *address = strtok(NULL, " \t");
        char *length = strtok(NULL, " \t");
        if (!address || !length || strtok(NULL, " \t")) reply_error(server, sequence, "usage: readmem address length");
        else handle_readmem(server, sequence, &emu->vm, address, length);
    } else if (strcmp(command, "writemem") == 0) {
        char *address = strtok(NULL, " \t");
        char *bytes = strtok(NULL, " \t");
        if (!address || !bytes || strtok(NULL, " \t")) reply_error(server, sequence, "usage: writemem address hexbytes");
        else handle_writemem(server, sequence, &emu->vm, address, bytes);
    } else if (strcmp(command, "quit") == 0) {
        if (strtok(NULL, " \t")) reply_error(server, sequence, "quit takes no arguments");
        else {
            send_format(server, "%s ok\n", sequence);
            send_format(server, "! exited 0\n");
            emu->status = EMU_STATUS_TERMINATED;
        }
    } else {
        reply_error(server, sequence, "unknown command");
    }
}

static void process_frames(struct lndbg_server *server, struct emulator *emu) {
    for (;;) {
        char *newline = memchr(server->frames, '\n', server->frames_used);
        if (!newline) return;
        size_t line_length = (size_t)(newline - server->frames);
        if (line_length > 0 && server->frames[line_length - 1] == '\r') line_length--;
        server->frames[line_length] = '\0';
        if (line_length == 0) send_format(server, "0 err invalid request\n");
        else handle_command(server, emu, server->frames);
        size_t consumed = (size_t)(newline - server->frames) + 1;
        memmove(server->frames, server->frames + consumed, server->frames_used - consumed);
        server->frames_used -= consumed;
        if (server->disconnected || emu->status == EMU_STATUS_TERMINATED) return;
    }
}

static void receive_available(struct lndbg_server *server, struct emulator *emu, bool block) {
    if (block && !wait_readable(server->client)) {
        server->disconnected = true;
        return;
    }
    for (;;) {
        if (server->frames_used == LNDBG_FRAME_CAPACITY) {
            send_format(server, "0 err frame too long\n");
            server->disconnected = true;
            return;
        }
        int received = recv(server->client, server->frames + server->frames_used,
                            (int)(LNDBG_FRAME_CAPACITY - server->frames_used), 0);
        if (received > 0) {
            server->frames_used += (size_t)received;
            process_frames(server, emu);
            if (server->disconnected || emu->status == EMU_STATUS_TERMINATED) return;
            continue;
        }
        if (received == 0) {
            server->disconnected = true;
            return;
        }
        if (socket_would_block()) return;
        server->disconnected = true;
        return;
    }
}

static bool open_server(struct lndbg_server *server) {
#ifdef _WIN32
    WSADATA winsock_data;
    if (WSAStartup(MAKEWORD(2, 2), &winsock_data) != 0) return false;
#endif
    server->listener = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (server->listener == LNDBG_INVALID_SOCKET) return false;
    struct sockaddr_in address;
    memset(&address, 0, sizeof(address));
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    address.sin_port = htons(0);
    if (bind(server->listener, (struct sockaddr *)&address, sizeof(address)) != 0 ||
        listen(server->listener, 1) != 0) return false;
    socklen_t address_length = (socklen_t)sizeof(address);
    if (getsockname(server->listener, (struct sockaddr *)&address, &address_length) != 0) return false;
    printf("LNDBG-LISTEN %u\n", (unsigned)ntohs(address.sin_port));
    fflush(stdout);
    server->client = accept(server->listener, NULL, NULL);
    if (server->client == LNDBG_INVALID_SOCKET || !set_nonblocking(server->client)) return false;
    return true;
}

static void close_server(struct lndbg_server *server) {
    if (server->client != LNDBG_INVALID_SOCKET) lndbg_close(server->client);
    if (server->listener != LNDBG_INVALID_SOCKET) lndbg_close(server->listener);
    free(server->frames);
    free(server->call_frames);
#ifdef _WIN32
    WSACleanup();
#endif
}

int run_debug_server(struct emulator *emu, bool stop_on_entry) {
    struct lndbg_server server;
    memset(&server, 0, sizeof(server));
    server.listener = LNDBG_INVALID_SOCKET;
    server.client = LNDBG_INVALID_SOCKET;
    server.call_frames_valid = true;
    server.frames = malloc(LNDBG_FRAME_CAPACITY);
    if (!server.frames || !open_server(&server)) {
        fprintf(stderr, "Failed to start LNDBG server.\n");
        close_server(&server);
        return 1;
    }

    if (stop_on_entry) {
        emu->status = EMU_STATUS_PAUSED;
        pause_devices(emu, &server);
        send_event(&server, "entry", emu->vm.cspc);
    } else {
        emu->status = EMU_STATUS_RUNNING;
    }

    while (!server.disconnected && emu->status != EMU_STATUS_TERMINATED) {
        if (emu->status == EMU_STATUS_PAUSED) {
            receive_available(&server, emu, true);
            if (!server.disconnected && server.pending_stop_reason) {
                pause_devices(emu, &server);
                send_event(&server, server.pending_stop_reason, emu->vm.cspc);
                server.pending_stop_reason = NULL;
            }
            continue;
        }

        receive_available(&server, emu, false);
        if (server.disconnected || emu->status == EMU_STATUS_TERMINATED) break;
        if (emu->status == EMU_STATUS_PAUSED) {
            pause_devices(emu, &server);
            send_event(&server, server.pending_stop_reason ? server.pending_stop_reason : "pause", emu->vm.cspc);
            server.pending_stop_reason = NULL;
            continue;
        }

        bool skip_breakpoint = server.skip_breakpoint_once;
        server.skip_breakpoint_once = false;
        if (!skip_breakpoint && breakpoint_hit(emu)) {
            emu->status = EMU_STATUS_PAUSED;
            pause_devices(emu, &server);
            send_event(&server, "breakpoint", emu->vm.cspc);
            continue;
        }

        resume_devices(emu, &server);
        step_devices(emu);
        uint16_t instruction_pc = emu->vm.cspc;
        uint8_t opcode = emu->vm.addr_space[instruction_pc];
        uint8_t initial_ss = emu->vm.ss;
        uint8_t initial_sp = emu->vm.sp;
        uint8_t return_discard = opcode == OP_RET_CST ? emu->vm.addr_space[(uint16_t)(instruction_pc + 1)] : 0;
        vm_step(&emu->vm);
        track_call_stack(&server, &emu->vm, opcode, instruction_pc, initial_ss, initial_sp, return_discard);

        if (emu->vm.halted) {
            emu->status = EMU_STATUS_TERMINATED;
            pause_devices(emu, &server);
            send_event(&server, "halt", emu->vm.cspc);
            send_format(&server, "! exited 0\n");
        } else if (emu->status == EMU_STATUS_STEPPING) {
            emu->status = EMU_STATUS_PAUSED;
            pause_devices(emu, &server);
            send_event(&server, "step", emu->vm.cspc);
        } else if (emu->status == EMU_STATUS_STEPPING_OVER &&
                   emu->vm.cspc == emu->step_over_target_addr &&
                   ((uint16_t)((uint16_t)emu->vm.ss << 8) | emu->vm.sp) == emu->step_over_target_sssp) {
            emu->status = EMU_STATUS_PAUSED;
            pause_devices(emu, &server);
            send_event(&server, "step", emu->vm.cspc);
        } else if (emu->status == EMU_STATUS_STEPPING_OUT && server.call_frames_valid &&
                   server.call_frame_count < server.step_out_depth) {
            emu->status = EMU_STATUS_PAUSED;
            pause_devices(emu, &server);
            send_event(&server, "step", emu->vm.cspc);
        }
    }

    close_server(&server);
    emu->status = EMU_STATUS_TERMINATED;
    return 0;
}
