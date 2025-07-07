package com.lnc.cc;

import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.argument.BinaryOp;
import com.lnc.cc.codegen.CodeGenUtils;
import com.lnc.cc.codegen.CompilerOutput;
import com.lnc.common.frontend.TokenType;

import java.util.ArrayList;
import java.util.List;

public class CodeSnippets {

    private static final SectionInfo START_SECTIONINFO = new SectionInfo("_START", 0, LinkTarget.ROM, LinkMode.FIXED, false, false, false);

    public static final CompilerOutput STANDALONE_START_CODE_OUTPUT;

    static {

        /*
            ; setup stack
            mov     0x21,       SS
            mov     0x0,        SP

            ; setup data page
            mov (LNCDATA >> 8)::byte, DS

            ; call main
            lcall main
            hlt

        */

        var o = new CompilerOutput(START_SECTIONINFO);
        o.addLabel("_START");

        o.append(CodeGenUtils.instr(TokenType.MOV,
                CodeGenUtils.immByte(0x21),
                CodeGenUtils.reg(TokenType.SS)));

        o.append(CodeGenUtils.instr(TokenType.MOV,
                CodeGenUtils.immByte(0),
                CodeGenUtils.reg(TokenType.SP)));

        o.append(CodeGenUtils.instr(TokenType.MOV,
                CodeGenUtils.cast(
                        CodeGenUtils.bin(
                        CodeGenUtils.labelRef("LNCDATA"),
                        CodeGenUtils.immByte(8),
                        TokenType.SHR
                ), "byte"),
                CodeGenUtils.reg(TokenType.DS)));

        o.append(CodeGenUtils.instr(TokenType.LCALL,
                CodeGenUtils.labelRef("main")));

        o.append(CodeGenUtils.instr(TokenType.HLT));

        STANDALONE_START_CODE_OUTPUT = o;
    }
}
