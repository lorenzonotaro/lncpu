#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "linker_config/linker_config.h"
#include "linker_config/linker_config_parser.h"


int main(void) {
    const char *cfg_text =
"SECTIONS[\n"
"    START:      target=ROM, start=0x0;\n"
"    INDEV0:     target=D0,  start=0x4000;\n"
"    STDIO:      target=ROM, mode=page_fit;\n"
"    IOUTILS:    target=ROM, mode=page_fit;\n"
"    NUMUTILS:   target=ROM, mode=page_fit;\n"
"\tDEV_KEYBOARD:\tmode = fixed, start = 0x4000;\n"
"\tDEV_TTY:\t\tmode = fixed, start = 0x6000;\n"
"    INPUT_BUFFER:\tmode = fixed, start = 0x3f00;\n"
"]\n";

    LinkerConfig cfg;
    char *err = NULL;

    if (!parse_linker_config_from_source(cfg_text, strlen(cfg_text), "test.cfg", &cfg, &err)) {
        fprintf(stderr, "Error: %s\n", err ? err : "(unknown)");
        free(err);
        return 1;
    }

    for (size_t i=0; i<cfg.count; ++i) {
        SectionInfo *s = &cfg.sections[i];
        printf("Section %s: start=0x%04x target=%d loc=%s:%llu\n",
               s->name, s->start, (int)s->target, s->loc_name.file, s->loc_name.line);
    }

    linker_config_free(&cfg);
    return 0;
}
