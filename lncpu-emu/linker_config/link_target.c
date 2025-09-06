//
// Created by loryn on 9/6/2025.
//

#include "link_target.h"

#include <stdbool.h>
#include <string.h>
#include <strings.h> /* strcasecmp */

static const LinkTargetInfo TBL[] = {
    [LT_ROM]     = { 0x0000, 0x1fff, true  },
    [LT_RAM]     = { 0x2000, 0x3fff, false },
    [LT_D0]      = { 0x4000, 0x5fff, false },
    [LT_D1]      = { 0x6000, 0x7fff, false },
    [LT_D2]      = { 0x8000, 0x9fff, false },
    [LT_D3]      = { 0xa000, 0xbfff, false },
    [LT_D4]      = { 0xc000, 0xdfff, false },
    [LT_D5]      = { 0xe000, 0xffff, false },
    [LT_VIRTUAL] = { 0x0000, 0xffff, false },
};

const LinkTargetInfo* link_target_info(LinkTarget t) {
    return &TBL[t];
}

bool link_target_contains(LinkTarget t, int address) {
    const LinkTargetInfo *i = &TBL[t];
    return address >= i->start && address <= i->end;
}

static LinkTarget from_name_cs(const char *s, size_t n) {
    #define IS(lit) (strlen(lit)==(n) && strncmp(s,lit,(n))==0)
    if (IS("ROM")) return LT_ROM;
    if (IS("RAM")) return LT_RAM;
    if (IS("D0"))  return LT_D0;
    if (IS("D1"))  return LT_D1;
    if (IS("D2"))  return LT_D2;
    if (IS("D3"))  return LT_D3;
    if (IS("D4"))  return LT_D4;
    if (IS("D5"))  return LT_D5;
    if (IS("__VIRTUAL__")) return LT_VIRTUAL;
    #undef IS
    return LT_VIRTUAL; /* fallback */
}

static LinkTarget from_name_ci(const char *s, size_t n) {
    #define IS(lit) (strlen(lit)==(n) && strncasecmp(s,lit,(n))==0)
    if (IS("ROM")) return LT_ROM;
    if (IS("RAM")) return LT_RAM;
    if (IS("D0"))  return LT_D0;
    if (IS("D1"))  return LT_D1;
    if (IS("D2"))  return LT_D2;
    if (IS("D3"))  return LT_D3;
    if (IS("D4"))  return LT_D4;
    if (IS("D5"))  return LT_D5;
    if (IS("__VIRTUAL__")) return LT_VIRTUAL;
    #undef IS
    return LT_VIRTUAL;
}

LinkTarget link_target_from_name(const char *s, size_t len, bool case_sensitive) {
    return case_sensitive ? from_name_cs(s,len) : from_name_ci(s,len);
}

LinkTarget link_target_from_address(int address) {
    for (int t=LT_ROM; t<=LT_VIRTUAL; ++t) {
        if (link_target_contains((LinkTarget)t, address)) return (LinkTarget)t;
    }
    return LT_VIRTUAL;
}
