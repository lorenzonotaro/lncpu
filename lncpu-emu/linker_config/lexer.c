//
// Created by loryn on 9/6/2025.
//

#include "token.h"
#include "lexer.h"
#include "location.h"
#include <ctype.h>
#include <stdio.h>
#include <string.h>

#if defined(_WIN32) || defined(_WIN64)
    #define strncasecmp(x,y,z) _strnicmp(x,y,z)
#endif

static Location make_loc(const Lexer *lx, size_t start_idx) {
    Location loc = { lx->file, lx->line, 1 };
    loc.column = (start_idx >= lx->line_start_idx) ? (start_idx - lx->line_start_idx + 1) : 1;
    return loc;
}

static int peekc(const Lexer *lx) {
    return (lx->idx < lx->length) ? (unsigned char)lx->source[lx->idx] : EOF;
}
static int peekc2(const Lexer *lx, size_t off) {
    size_t i = lx->idx + off;
    return (i < lx->length) ? (unsigned char)lx->source[i] : EOF;
}
static int getc_advance(Lexer *lx) {
    if (lx->idx >= lx->length) return EOF;
    int c = (unsigned char)lx->source[lx->idx++];
    if (c == '\n') {
        lx->line++;
        lx->line_start_idx = lx->idx;
    }
    return c;
}

static bool is_ident_start(int c) { return isalpha(c) || c=='_' ; }
static bool is_ident_cont(int c)  { return isalnum(c) || c=='_' ; }

static TokenType keyword_type(const char *s, size_t n, bool cs) {
#define KW(_t,lit) if(((n)==strlen(lit)) && (cs? (strncmp(s,lit,(n))==0) : strncasecmp(s,lit,(n))==0)) return (_t)
    KW(T_KW_SECTIONS, "SECTIONS");
    KW(T_KW_TRUE,    "true");
    KW(T_KW_FALSE,   "false");
#undef KW
    return T_IDENTIFIER;
}

static Token make_token(Lexer *lx, TokenType t, const char *lex, size_t len, size_t start_idx, long long num) {
    Token tk;
    tk.type   = t;
    tk.lexeme = lex;
    tk.len    = len;
    tk.number = num;
    tk.loc    = make_loc(lx, start_idx);
    return tk;
}

static void skip_ws_and_comments(Lexer *lx) {
    for (;;) {
        int c = peekc(lx);
        if (c == ' ' || c == '\t' || c == '\r' || c == '\n') { getc_advance(lx); continue; }
        if (lx->cfg.single_line_comments == CMT_C_STYLE_SINGLE && c == '/' && peekc2(lx,1) == '/') {
            while ((c = getc_advance(lx)) != EOF && c != '\n') {}
            continue;
        }
        if (lx->cfg.single_line_comments == CMT_ASM_STYLE_SINGLE && c == ';') {
            while ((c = getc_advance(lx)) != EOF && c != '\n') {}
            continue;
        }
        break;
    }
}

static long long parse_number(const char *p, size_t n, bool *ok) {
    /* decimal or 0x... */
    *ok = false;
    if (n >= 3 && p[0] == '0' && (p[1] == 'x' || p[1]=='X')) {
        long long v = 0;
        for (size_t i=2;i<n;i++){
            int c = p[i];
            int d = (c>='0'&&c<='9')? c-'0' :
                    (c>='a'&&c<='f')? c-'a'+10 :
                    (c>='A'&&c<='F')? c-'A'+10 : -1;
            if (d < 0) return 0;
            v = (v<<4) + d;
        }
        *ok = true; return v;
    } else {
        long long v = 0; int sgn=1; size_t i=0;
        if (n>0 && (p[0]=='+'||p[0]=='-')) { sgn = (p[0]=='-')?-1:1; i=1; }
        for (; i<n; i++){
            int c=p[i]; if (!(c>='0'&&c<='9')) return 0;
            v = v*10 + (c-'0');
        }
        *ok = true; return sgn*v;
    }
}

void lexer_init(Lexer *lx, const char *src, size_t len, const char *file, LexerConfig cfg) {
    lx->source = src;
    lx->length = len;
    lx->file   = file;
    lx->idx    = 0;
    lx->line   = 1;
    lx->line_start_idx = 0;
    lx->cfg    = cfg;
}

Token lexer_next(Lexer *lx) {
    skip_ws_and_comments(lx);
    size_t start = lx->idx;
    int c = peekc(lx);
    if (c == EOF) {
        return make_token(lx, T_EOF, lx->source+lx->idx, 0, lx->idx, 0);
    }

    /* punctuation */
    if (c==':') { getc_advance(lx); return make_token(lx, T_COLON    , lx->source+start, 1, start, 0); }
    if (c==',') { getc_advance(lx); return make_token(lx, T_COMMA    , lx->source+start, 1, start, 0); }
    if (c==';') { getc_advance(lx); return make_token(lx, T_SEMICOLON, lx->source+start, 1, start, 0); }
    if (c=='=') { getc_advance(lx); return make_token(lx, T_EQUAL    , lx->source+start, 1, start, 0); }
    if (c=='[') { getc_advance(lx); return make_token(lx, T_LBRACKET , lx->source+start, 1, start, 0); }
    if (c==']') { getc_advance(lx); return make_token(lx, T_RBRACKET , lx->source+start, 1, start, 0); }


    /* identifier / keyword */
    if (is_ident_start(c)) {
        getc_advance(lx);
        while (is_ident_cont(peekc(lx))) getc_advance(lx);
        size_t len = lx->idx - start;
        TokenType tt = keyword_type(lx->source+start, len, lx->cfg.case_sensitive);
        return make_token(lx, tt, lx->source+start, len, start, 0);
    }

    /* number */
    if (isdigit(c) || c=='+' || c=='-') {
        getc_advance(lx);
        while (isxdigit(peekc(lx)) || peekc(lx)=='x' || peekc(lx)=='X') getc_advance(lx);
        size_t len = lx->idx - start;
        bool ok=false;
        long long v = parse_number(lx->source+start, len, &ok);
        return make_token(lx, ok?T_NUMBER:T_IDENTIFIER, lx->source+start, len, start, v);
    }

    /* fallback: treat as single-char identifier */
    getc_advance(lx);
    return make_token(lx, T_IDENTIFIER, lx->source+start, 1, start, 0);
}