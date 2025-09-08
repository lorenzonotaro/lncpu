

#include "config/cmdline.h"
#include "emu.h"

#include <windows.h>
#include <conio.h>
#include <io.h>
#include <stdio.h>

void diag(void) {
    DWORD mode = 0;
    HANDLE hIn = GetStdHandle(STD_INPUT_HANDLE);

    printf("_isatty(stdin) = %d\n", _isatty(_fileno(stdin)));
    printf("GetConsoleMode(STDIN) = %d\n", GetConsoleMode(hIn, &mode)); // 0 = no console
    printf("STDIN handle = %p, type = %lu\n", hIn, GetFileType(hIn));

    puts("Press keys (Esc to quit)...");
    for (;;) {
        if (_kbhit()) {
            int c = _getch();
            printf("got %d '%c'\n", c, (c>=32 && c<127)?c:'.');
            if (c == 27) break;
        } else {
            Sleep(50);
        }
    }
}

int main(int argc, const char **argv) {
    setvbuf(stdin, NULL, _IONBF, 0);
    //diag();
    struct emu_cmdline_params cmdline_params;

    cmdline_init(&cmdline_params);

    if (!parse_args(&cmdline_params, argc, argv)) {
        return 1;
    }

    return run_emu(&cmdline_params);
}
