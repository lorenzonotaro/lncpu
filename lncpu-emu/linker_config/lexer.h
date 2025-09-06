//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_LEXER_H
#define LNCPU_EMU_LEXER_H

#include <stdbool.h>
#include <stddef.h>

/* Comment modes (kept generic for reuse) */
typedef enum {
    CMT_DISABLED = 0,
    CMT_C_STYLE_SINGLE,  /* // ... */
    CMT_ASM_STYLE_SINGLE /* ; ... */
} SingleLineCommentMode;

typedef struct {
    /* keyword table is fixed for LinkerConfig, but the API is generic */
    bool case_sensitive;
    SingleLineCommentMode single_line_comments;
} LexerConfig;

typedef struct {
    const char *source;     /* not owned */
    const char *file;       /* not owned */
    size_t length;
    size_t idx;
    size_t line;
    size_t line_start_idx;
    LexerConfig cfg;
} Lexer;

void lexer_init(Lexer *lx, const char *src, size_t len, const char *file, LexerConfig cfg);

/* Produce next token (stateless “iterator” style). */
Token lexer_next(Lexer *lx);

#endif //LNCPU_EMU_LEXER_H