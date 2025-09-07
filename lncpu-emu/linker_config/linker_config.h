//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_LINKER_CONFIG_H
#define LNCPU_EMU_LINKER_CONFIG_H


#include <stddef.h>
#include <stdbool.h>
#include "link_target.h"
#include "location.h"

#include <stddef.h>
#include <stdbool.h>
#include "link_target.h"
#include "location.h"

typedef enum {
    LM_UNSPEC = 0,
    LM_FIXED,
    LM_PAGE_FIT,
    LM_PAGE_ALIGN,
    LM_FIT
} LinkMode;

typedef struct {
    char       *name;      /* owned */
    int         start;     /* 0..0xFFFF, optional if mode=page_fit */
    LinkTarget  target;    /* ROM, RAM, D0..D5, __VIRTUAL__ */
    LinkMode    mode;      /* fixed | page_fit */
    Location    loc_name;  /* for diagnostics */
    bool datapage;         /* 0=false, 1=true, default=0 */
    bool virtual;          /* 0=false, 1=true, default=0 */
    bool multi;            /* 0=false, 1=true, default=0 */
} SectionInfo;

typedef struct {
    SectionInfo *sections; /* owned array */
    size_t       count;
    size_t       cap;
} LinkerConfig;

void linker_config_init(LinkerConfig *cfg);
void linker_config_free(LinkerConfig *cfg);
const SectionInfo *linker_config_get_section(LinkerConfig *lc, const char *name);
bool linker_config_push(LinkerConfig *cfg, SectionInfo si);

#endif //LNCPU_EMU_LINKER_CONFIG_H