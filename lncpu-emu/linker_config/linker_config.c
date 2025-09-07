//
// Created by loryn on 9/6/2025.
//

#include "linker_config.h"
#include <stdlib.h>
#include <string.h>

void linker_config_init(LinkerConfig *cfg) {
    cfg->sections = NULL;
    cfg->count = 0;
    cfg->cap = 0;
}

static void free_section(SectionInfo *s) {
    if (s->name) free(s->name);
}

void linker_config_free(LinkerConfig *cfg) {
    if (!cfg) return;
    for (size_t i=0; i<cfg->count; ++i) free_section(&cfg->sections[i]);
    free(cfg->sections);
    cfg->sections = NULL;
    cfg->count = cfg->cap = 0;
}

const SectionInfo * linker_config_get_section(LinkerConfig *lc, const char *name) {
    for (size_t i=0; i<lc->count; ++i) {
        if (strcmp(lc->sections[i].name, name) == 0) return &lc->sections[i];
    }
    return NULL;
}

bool linker_config_push(LinkerConfig *cfg, SectionInfo si) {
    if (cfg->count == cfg->cap) {
        size_t ncap = cfg->cap ? cfg->cap*2 : 8;
        SectionInfo *n = (SectionInfo*)realloc(cfg->sections, ncap*sizeof(*n));
        if (!n) return false;
        cfg->sections = n;
        cfg->cap = ncap;
    }
    cfg->sections[cfg->count++] = si;
    return true;
}
