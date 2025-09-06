//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_TOKEN_H
#define LNCPU_EMU_TOKEN_H

#include <stddef.h>
#include "location.h"

typedef enum {
    T_EOF = 0,
    T_IDENTIFIER,
    T_NUMBER, /* decimal or 0x... */

    /* keywords (case-insensitive) */
    T_KW_SECTIONS,
    T_KW_TRUE,
    T_KW_FALSE,

    /* punctuation */
    T_COLON, /* : */
    T_COMMA, /* , */
    T_SEMICOLON, /* ; */
    T_EQUAL, /* = */
    T_LBRACKET, /* [ */
    T_RBRACKET, /* ] */
} TokenType;

typedef struct {
    TokenType type;
    const char *lexeme; /* view into source, not owned */
    size_t len;
    long long number; /* valid if NUMBER */
    Location loc;
} Token;

#endif //LNCPU_EMU_TOKEN_H
