//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_LINK_TARGET_H
#define LNCPU_EMU_LINK_TARGET_H
#include <stdbool.h>
#include <stddef.h>

typedef enum {
    LT_ROM = 0,
    LT_RAM,
    LT_D0, LT_D1, LT_D2, LT_D3, LT_D4, LT_D5,
    LT_VIRTUAL /* __VIRTUAL__ */
} LinkTarget;

typedef struct {
    int start;  /* inclusive */
    int end;    /* inclusive */
    bool read_only;
} LinkTargetInfo;

const LinkTargetInfo* link_target_info(LinkTarget t);
bool  link_target_contains(LinkTarget t, int address);
LinkTarget link_target_from_name(const char *s, size_t len, bool case_sensitive);
LinkTarget link_target_from_address(int address);

#endif //LNCPU_EMU_LINK_TARGET_H