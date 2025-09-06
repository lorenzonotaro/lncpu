#include "linker_config_parser.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdarg.h>

#include "lexer.h"

typedef struct {
    Parser p;
    char  *err; /* owned if set */
    bool   case_sensitive;
} LCx;

/* ---- small utils ---- */
static char* dup_slice(const char *s, size_t n) {
    char *r = (char*)malloc(n+1);
    if (!r) return NULL;
    memcpy(r,s,n); r[n]='\0';
    return r;
}

static void set_error(LCx *S, const Token *tk, const char *fmt, ...) {
    if (S->err) return;
    char buf[512];
    va_list ap; va_start(ap, fmt);
    vsnprintf(buf, sizeof(buf), fmt, ap);
    va_end(ap);
    char loc[128];
    snprintf(loc, sizeof(loc), "%s:%zu:%zu: ",
             tk->loc.file?tk->loc.file:"<input>", tk->loc.line, tk->loc.column);
    size_t L = strlen(loc), M = strlen(buf);
    S->err = (char*)malloc(L+M+1);
    if (!S->err) return;
    memcpy(S->err, loc, L);
    memcpy(S->err+L, buf, M);
    S->err[L+M] = 0;
}

static const Token* need(LCx *S, TokenType t, const char *what) {
    const Token *tk = parser_consume(&S->p, t, what);
    if (!tk) set_error(S, parser_peek(&S->p), "expected %s", what);
    return tk;
}

static int icmp(const char *s, size_t n, const char *lit, bool cs) {
    size_t L = strlen(lit);
    if (L != n) return 1;
    return cs ? strncmp(s, lit, n) : strncasecmp(s, lit, n);
}

/* ---- productions ---- */

static bool parse_props(LCx *S, SectionInfo *si) {
    /* key '=' value (',' key '=' value)* */
    for (;;) {
        const Token *k = parser_peek(&S->p);
        if (parser_check(&S->p, T_SEMICOLON) || parser_check(&S->p, T_RBRACKET)) break;

        if (parser_match_ident(&S->p, "start")) {
            if (!need(S, T_EQUAL, "'=' after start")) return false;
            const Token *v = need(S, T_NUMBER, "number");
            if (!v) return false;
            si->start = (int)v->number;
        }
        else if (parser_match_ident(&S->p, "target")) {
            if (!need(S, T_EQUAL, "'=' after target")) return false;
            const Token *v = need(S, T_IDENTIFIER, "target name");
            if (!v) return false;
            si->target = link_target_from_name(v->lexeme, v->len, S->case_sensitive);
        }
        else if (parser_match_ident(&S->p, "mode")) {
            if (!need(S, T_EQUAL, "'=' after mode")) return false;
            const Token *v = need(S, T_IDENTIFIER, "mode name");
            if (!v) return false;
            if (icmp(v->lexeme, v->len, "fixed", S->case_sensitive)==0)       si->mode = LM_FIXED;
            else if (icmp(v->lexeme, v->len, "page_fit", S->case_sensitive)==0) si->mode = LM_PAGE_FIT;
            else if (icmp(v->lexeme, v->len, "page_align", S->case_sensitive)==0) si->mode = LM_PAGE_ALIGN;
            else if (icmp(v->lexeme, v->len, "fit", S->case_sensitive)==0) si->mode = LM_FIT;
            else {
                set_error(S, v, "unsupported mode '%.*s'", (int)v->len, v->lexeme);
                return false;
            }
        }
        else if (parser_match_ident(&S->p, "datapage")) {
            if (parser_match(&S->p, T_EQUAL)) {
                if (parser_match(&S->p, T_KW_TRUE)) {
                    si->datapage = true;
                }else if (parser_match(&S->p, T_KW_FALSE)) {
                    si->datapage = false;
                }
            }else {
                si->datapage = true;
            }
        }else if (parser_match_ident(&S->p, "virtual")) {
            if (parser_match(&S->p, T_EQUAL)) {
                if (parser_match(&S->p, T_KW_TRUE)) {
                    si->virtual = true;
                }else if (parser_match(&S->p, T_KW_FALSE)) {
                    si->virtual = false;
                }
            }else {
                si->virtual = true;
            }
        }
        else if (parser_match_ident(&S->p, "multi")) {
            if (parser_match(&S->p, T_EQUAL)) {
                if (parser_match(&S->p, T_KW_TRUE)) {
                    si->multi = true;
                }else if (parser_match(&S->p, T_KW_FALSE)) {
                    si->multi = false;
                }
            }else {
                si->multi = true;
            }
        }
        else {
            set_error(S, k, "unexpected token '%.*s' in properties", (int)k->len, k->lexeme);
            return false;
        }

        (void)parser_match(&S->p, T_COMMA); /* optional comma between props */
    }
    return true;
}

static bool parse_entry(LCx *S, LinkerConfig *out_cfg) {
    /* <IDENT> ':' <props> ';' */
    const Token *name = need(S, T_IDENTIFIER, "section name");
    if (!name) return false;
    if (!need(S, T_COLON, "':' after section name")) return false;

    SectionInfo si;
    memset(&si, 0, sizeof(si));
    si.name  = dup_slice(name->lexeme, name->len);
    si.loc_name = name->loc;
    si.mode  = LM_UNSPEC;
    si.target = LT_VIRTUAL; /* default if not provided; you can change to ROM if you prefer */

    if (!parse_props(S, &si)) { free(si.name); return false; }

    if (!need(S, T_SEMICOLON, "';' after section entry")) { free(si.name); return false; }

    /* validation:
       - if mode=fixed, 'start' must be set and within target (unless target is __VIRTUAL__) */
    if (si.mode == LM_FIXED) {
        const LinkTargetInfo *ti = link_target_info(si.target);
        if (si.start < ti->start || si.start > ti->end) {
            set_error(S, name, "start 0x%04x not in target range [0x%04x..0x%04x]",
                      si.start, ti->start, ti->end);
            free(si.name); return false;
        }
    }

    if (!linker_config_push(out_cfg, si)) { free(si.name); set_error(S, name, "out of memory"); return false; }
    return true;
}


static Token* lex_all(const char *src, size_t len, const char *file, LexerConfig cfg, size_t *out_count) {
    Lexer lx; lexer_init(&lx, src, len, file, cfg);
    size_t cap = 128, n=0;
    Token *arr = (Token*)malloc(cap*sizeof(*arr));
    if (!arr) return NULL;
    for (;;) {
        if (n==cap) { cap*=2; Token *t=(Token*)realloc(arr, cap*sizeof(*t)); if(!t){ free(arr); return NULL; } arr=t; }
        Token tk = lexer_next(&lx);
        arr[n++] = tk;
        if (tk.type == T_EOF) break;
    }
    *out_count = n;
    return arr;
}


bool parse_linker_config_tokens(const Token *tokens, size_t count, LinkerConfig *out_cfg, char **out_err) {
    *out_err = NULL;
    LCx S = {0};
    parser_init(&S.p, tokens, count);
    S.case_sensitive = false;

    linker_config_init(out_cfg);

    /* SECTIONS '[' { entry } ']' */
    if (!need(&S, T_KW_SECTIONS, "'SECTIONS'")) { *out_err = S.err; return false; }
    if (!need(&S, T_LBRACKET, "'['"))           { *out_err = S.err; return false; }

    while (!parser_check(&S.p, T_RBRACKET) && !parser_is_at_end(&S.p)) {
        if (!parse_entry(&S, out_cfg)) { *out_err = S.err ? S.err : strdup("parse error"); return false; }
    }

    if (!need(&S, T_RBRACKET, "']'")) { *out_err = S.err; return false; }
    return true;
}

bool parse_linker_config_from_source(const char *src, size_t len, const char *file, LinkerConfig *out_cfg, char **out_err) {
    LexerConfig lcfg = {
        .case_sensitive = false,
        .single_line_comments = CMT_C_STYLE_SINGLE
    };
    size_t n=0;
    Token *toks = lex_all(src, len, file, lcfg, &n);
    if (!toks) { *out_err = strdup("out of memory"); return false; }
    bool ok = parse_linker_config_tokens(toks, n, out_cfg, out_err);
    free(toks);
    return ok;
}
