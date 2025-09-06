cmake_minimum_required(VERSION 4.0)
project(lncpu_emu C)

set(CMAKE_C_STANDARD 17)

add_executable(lncpu_emu main.c
        argparse.h
        argparse.c
        cmdline.h
        linker_config/location.h
        linker_config/token.h
        linker_config/lexer.h
        linker_config/parser.h
        linker_config/link_target.h
        linker_config/linker_config.h
        linker_config/linker_config_parser.h
        linker_config/lexer.c
        linker_config/parser.c
        linker_config/link_target.c
        linker_config/linker_config_parser.c
        linker_config/linker_config.c)
