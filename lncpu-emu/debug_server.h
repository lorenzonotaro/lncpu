#ifndef LNCPU_EMU_DEBUG_SERVER_H
#define LNCPU_EMU_DEBUG_SERVER_H

#include <stdbool.h>

struct emulator;

int run_debug_server(struct emulator *emu, bool stop_on_entry);

#endif
