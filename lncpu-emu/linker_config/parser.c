//
// Created by loryn on 9/6/2025.
//

#include "parser.h"

#include <string.h>

void parser_init(Parser *p, const Token *tokens, size_t count) {
    p->tokens = tokens;
    p->count  = count;
    p->index  = 0;
}

bool parser_is_at_end(const Parser *p) {
    return p->index >= p->count || p->tokens[p->index].type == T_EOF;
}

const Token* parser_peek(const Parser *p) {
    if (p->index >= p->count) return &p->tokens[p->count-1];
    return &p->tokens[p->index];
}

const Token* parser_previous(const Parser *p) {
    if (p->index==0) return &p->tokens[0];
    return &p->tokens[p->index-1];
}

const Token* parser_advance(Parser *p) {
    if (!parser_is_at_end(p)) p->index++;
    return parser_previous(p);
}

bool parser_check(const Parser *p, TokenType t) {
    if (parser_is_at_end(p)) return false;
    return parser_peek(p)->type == t;
}

bool parser_match(Parser *p, TokenType t) {
    if (parser_check(p,t)) { parser_advance(p); return true; }
    return false;
}

bool parser_match_ident(Parser *p, const char *lexeme) {
    if (parser_check(p,T_IDENTIFIER)) {
        const Token *t = parser_peek(p);
        if (strncmp(t->lexeme, lexeme, t->len)==0) {
            parser_advance(p);
            return true;
        }
    }
    return false;
}

const Token* parser_consume(Parser *p, TokenType t, const char *msg) {
    if (parser_check(p,t)) return parser_advance(p);
    (void)msg; /* message can be used by caller to format error */
    return NULL;
}
