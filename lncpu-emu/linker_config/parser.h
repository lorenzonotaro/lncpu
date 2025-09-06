//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_PARSER_H
#define LNCPU_EMU_PARSER_H

#include <stdbool.h>
#include <stddef.h>

#include "token.h"

typedef struct {
    const Token *tokens;  /* array */
    size_t count;
    size_t index;
} Parser;

/* Common parser helpers */
void   parser_init(Parser *p, const Token *tokens, size_t count);
bool   parser_is_at_end(const Parser *p);
const  Token* parser_peek(const Parser *p);
const  Token* parser_previous(const Parser *p);
const  Token* parser_advance(Parser *p);
bool   parser_check(const Parser *p, TokenType t);
bool   parser_match(Parser *p, TokenType t);
bool    parser_match_ident(Parser *p, const char *lexeme);
const  Token* parser_consume(Parser *p, TokenType t, const char *msg);

#endif //LNCPU_EMU_PARSER_H