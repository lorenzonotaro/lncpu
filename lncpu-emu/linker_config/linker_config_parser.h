//
// Created by loryn on 9/6/2025.
//

#ifndef LNCPU_EMU_LINKER_CONFIG_PARSER_H
#define LNCPU_EMU_LINKER_CONFIG_PARSER_H

#include <stdbool.h>
#include "parser.h"
#include "linker_config.h"

/* High-level parse entrypoints */
bool parse_linker_config_from_source(const char *src, size_t len, const char *file, LinkerConfig *out_cfg, char **out_err);
bool parse_linker_config_tokens(const Token *tokens, size_t count, LinkerConfig *out_cfg, char **out_err);


#endif //LNCPU_EMU_LINKER_CONFIG_PARSER_H