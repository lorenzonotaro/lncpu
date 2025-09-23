cmake_minimum_required(VERSION 4.0)
project(lncpu_emu C)

set(CMAKE_C_STANDARD 17)

add_executable(lncpu_emu main.c
        config/argparse.h
        config/argparse.c
        config/cmdline.h
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
        linker_config/linker_config.c
        config/cmdline.c
        emu.c
        emu.h
        vm.h
        vm.c
        emu_device.h
        emu_device.c
        opcodes.h
        signal_handler.h
        signal_handler.c
        utlist.h)

target_compile_options(lncpu_emu PRIVATE -lconio)