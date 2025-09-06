//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_LOCATION_H
#define LNCPU_EMU_LOCATION_H

#include <stddef.h>

typedef struct {
    const char *file;   /* not owned */
    size_t line;        /* 1-based */
    size_t column;      /* 1-based utf-8 byte column */
} Location;

#endif //LNCPU_EMU_LOCATION_H