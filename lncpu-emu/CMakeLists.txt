cmake_minimum_required(VERSION 3.25)
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
        debug_server.c
        debug_server.h
        emu_expect.c
        emu_expect.h
        vm.h
        vm.c
        emu_device.h
        emu_device.c
        opcodes.h
        signal_handler.h
        signal_handler.c
        utlist.h)

if(WIN32)
    target_link_libraries(lncpu_emu PRIVATE ws2_32)
endif()

include(CTest)
if(BUILD_TESTING)
    find_package(Python3 COMPONENTS Interpreter REQUIRED)
    add_test(NAME lndbg_socket
            COMMAND ${Python3_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/test_debug_server.py $<TARGET_FILE:lncpu_emu>)
    set_tests_properties(lndbg_socket PROPERTIES TIMEOUT 15)
endif()
