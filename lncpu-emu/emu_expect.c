#include "emu_expect.h"

#include <ctype.h>
#include <errno.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static char *trim(char *s) {
    while (isspace((unsigned char)*s)) {
        s++;
    }

    char *end = s + strlen(s);
    while (end > s && isspace((unsigned char)*(end - 1))) {
        *(--end) = '\0';
    }

    return s;
}

static void strip_comment(char *line) {
    char *hash = strchr(line, '#');
    char *semi = strchr(line, ';');
    char *slash = strstr(line, "//");
    char *cut = NULL;

    if (hash) cut = hash;
    if (semi && (!cut || semi < cut)) cut = semi;
    if (slash && (!cut || slash < cut)) cut = slash;

    if (cut) {
        *cut = '\0';
    }
}

static bool parse_number(const char *text, unsigned long *out) {
    char *end = NULL;
    errno = 0;

    if (text[0] == '0' && (text[1] == 'b' || text[1] == 'B')) {
        unsigned long value = 0;
        const char *p = text + 2;
        if (*p == '\0') {
            return false;
        }
        while (*p) {
            if (*p != '0' && *p != '1') {
                return false;
            }
            value = (value << 1) | (unsigned long)(*p - '0');
            p++;
        }
        *out = value;
        return true;
    }

    unsigned long value = strtoul(text, &end, 0);
    if (errno != 0 || end == text || *trim(end) != '\0') {
        return false;
    }

    *out = value;
    return true;
}

static void upper(char *s) {
    while (*s) {
        *s = (char)toupper((unsigned char)*s);
        s++;
    }
}

static bool register_value(struct lncpu_vm *vm, char *name, unsigned long *value, unsigned long *mask) {
    upper(name);

    *mask = 0xff;
    if (strcmp(name, "RA") == 0) *value = vm->ra;
    else if (strcmp(name, "RB") == 0) *value = vm->rb;
    else if (strcmp(name, "RC") == 0) *value = vm->rc;
    else if (strcmp(name, "RD") == 0) *value = vm->rd;
    else if (strcmp(name, "DS") == 0) *value = vm->ds;
    else if (strcmp(name, "SS") == 0) *value = vm->ss;
    else if (strcmp(name, "SP") == 0) *value = vm->sp;
    else if (strcmp(name, "BP") == 0) *value = vm->bp;
    else if (strcmp(name, "FLAGS") == 0) *value = vm->flags;
    else if (strcmp(name, "HALTED") == 0) *value = vm->halted ? 1 : 0;
    else if (strcmp(name, "CSPC") == 0 || strcmp(name, "CS_PC") == 0 || strcmp(name, "CS:PC") == 0) {
        *value = vm->cspc;
        *mask = 0xffff;
    } else {
        return false;
    }

    return true;
}

int check_expectations(struct lncpu_vm *vm, const char *filename) {
    FILE *file = fopen(filename, "r");
    if (!file) {
        fprintf(stderr, "expect: unable to open %s\n", filename);
        return 2;
    }

    char buffer[512];
    unsigned int line_number = 0;
    unsigned int checked = 0;
    unsigned int failed = 0;

    while (fgets(buffer, sizeof(buffer), file)) {
        line_number++;
        strip_comment(buffer);
        char *line = trim(buffer);
        if (*line == '\0') {
            continue;
        }

        char *equals = strchr(line, '=');
        if (!equals) {
            fprintf(stderr, "%s:%u: expected '<register|[address]> = <value>'\n", filename, line_number);
            fclose(file);
            return 2;
        }

        *equals = '\0';
        char *left = trim(line);
        char *right = trim(equals + 1);
        unsigned long expected;
        if (!parse_number(right, &expected)) {
            fprintf(stderr, "%s:%u: invalid value '%s'\n", filename, line_number, right);
            fclose(file);
            return 2;
        }

        unsigned long actual;
        unsigned long mask;
        if (left[0] == '[') {
            size_t len = strlen(left);
            if (len < 3 || left[len - 1] != ']') {
                fprintf(stderr, "%s:%u: invalid memory expression '%s'\n", filename, line_number, left);
                fclose(file);
                return 2;
            }

            left[len - 1] = '\0';
            char *addr_text = trim(left + 1);
            unsigned long address;
            if (!parse_number(addr_text, &address) || address > 0xffff) {
                fprintf(stderr, "%s:%u: invalid address '%s'\n", filename, line_number, addr_text);
                fclose(file);
                return 2;
            }

            actual = vm_read_byte(vm, (uint16_t)address);
            mask = 0xff;
            checked++;
            if ((actual & mask) != (expected & mask)) {
                fprintf(stderr, "FAIL %s:%u: [0x%04lx] expected 0x%02lx, got 0x%02lx\n",
                        filename, line_number, address, expected & mask, actual & mask);
                failed++;
            }
        } else {
            char reg_name[64];
            snprintf(reg_name, sizeof(reg_name), "%s", left);
            if (!register_value(vm, reg_name, &actual, &mask)) {
                fprintf(stderr, "%s:%u: unknown register '%s'\n", filename, line_number, left);
                fclose(file);
                return 2;
            }

            checked++;
            if ((actual & mask) != (expected & mask)) {
                fprintf(stderr, "FAIL %s:%u: %s expected 0x%0*lx, got 0x%0*lx\n",
                        filename, line_number, reg_name, mask == 0xffff ? 4 : 2, expected & mask,
                        mask == 0xffff ? 4 : 2, actual & mask);
                failed++;
            }
        }
    }

    fclose(file);

    if (failed) {
        fprintf(stderr, "expect: %u/%u condition(s) failed\n", failed, checked);
        return 1;
    }

    printf("expect: %u condition(s) passed\n", checked);
    return 0;
}
