package com.lnc.cc.codegen;

import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.assembler.parser.argument.Composite;
import com.lnc.common.frontend.TokenType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.lnc.assembler.parser.LnasmParser.SUBLABEL_INITIATOR;

/**
 * Tracks and emits software extension routines required by code generation.
 */
public class SoftwareExtensionsManager {

    private static final SectionInfo EXT_SECTION = new SectionInfo("LNCEXT", -1, LinkTarget.ROM, LinkMode.PAGE_FIT, false, false, false);

    private final Set<ExtensionRequest> requiredExtensions = new LinkedHashSet<>();

    public String requireAddWordByte(Register dstWord, Register srcByte) {
        return require(ExtensionKind.ADD_WORD_BYTE, dstWord, srcByte);
    }

    public String requireAddWordWord(Register dstWord, Register srcWord) {
        return require(ExtensionKind.ADD_WORD_WORD, dstWord, srcWord);
    }

    public String requireSubWordByte(Register dstWord, Register srcByte) {
        return require(ExtensionKind.SUB_WORD_BYTE, dstWord, srcByte);
    }

    public String requireSubWordWord(Register dstWord, Register srcWord) {
        return require(ExtensionKind.SUB_WORD_WORD, dstWord, srcWord);
    }

    public String requireIncWord(Register dstWord) {
        return require(ExtensionKind.INC_WORD, dstWord, null);
    }

    public String requireIncWord(Composite splitInner) {
        return require(ExtensionKind.INC_WORD, Register.valueOf(splitInner.high.toString().toUpperCase() + splitInner.low.toString().toUpperCase()), null);
    }

    public String requireDecWord(Composite splitInner) {
        return require(ExtensionKind.DEC_WORD, Register.valueOf(splitInner.high.toString().toUpperCase() + splitInner.low.toString().toUpperCase()), null);
    }

    public String requireDecWord(Register dstWord) {
        return require(ExtensionKind.DEC_WORD, dstWord, null);
    }

    public CompilerOutput emitOutput() {
        if (requiredExtensions.isEmpty()) {
            return null;
        }

        CompilerOutput output = new CompilerOutput(EXT_SECTION);

        List<ExtensionRequest> ordered = new ArrayList<>(requiredExtensions);
        ordered.sort(Comparator
                .comparing((ExtensionRequest req) -> req.kind)
                .thenComparing(req -> req.dstWord.ordinal())
                .thenComparing(req -> req.src == null ? -1 : req.src.ordinal()));

        for (var request : ordered) {
            emitRoutine(output, request);
        }

        return output;
    }

    private String require(ExtensionKind kind, Register dstWord, Register src) {
        validateRequest(kind, dstWord, src);
        ExtensionRequest request = new ExtensionRequest(kind, dstWord, src);
        requiredExtensions.add(request);
        return request.symbolName();
    }

    private void validateRequest(ExtensionKind kind, Register dstWord, Register src) {
        if (dstWord == null || !dstWord.isCompound()) {
            throw new IllegalArgumentException("Destination must be a word register");
        }

        switch (kind) {
            case ADD_WORD_BYTE, SUB_WORD_BYTE -> {
                if (src == null || src.isCompound()) {
                    throw new IllegalArgumentException(kind + " requires a byte source register");
                }
            }
            case ADD_WORD_WORD, SUB_WORD_WORD -> {
                if (src == null || !src.isCompound()) {
                    throw new IllegalArgumentException(kind + " requires a word source register");
                }
            }
            case INC_WORD, DEC_WORD -> {
                if (src != null) {
                    throw new IllegalArgumentException(kind + " does not use a source register");
                }
            }
        }
    }

    private void emitRoutine(CompilerOutput output, ExtensionRequest request) {
        output.addLabel(request.symbolName());

        switch (request.kind) {
            case ADD_WORD_BYTE -> emitAddWordByte(output, request.dstWord, request.src);
            case ADD_WORD_WORD -> emitAddWordWord(output, request.dstWord, request.src);
            case SUB_WORD_BYTE -> emitSubWordByte(output, request.dstWord, request.src);
            case SUB_WORD_WORD -> emitSubWordWord(output, request.dstWord, request.src);
            case INC_WORD -> emitIncWord(output, request.dstWord);
            case DEC_WORD -> emitDecWord(output, request.dstWord);
        }
    }

    private void emitAddWordByte(CompilerOutput output, Register dstWord, Register srcByte) {
        var low = reg(low(dstWord));
        var high = reg(high(dstWord));
        var src = reg(srcByte);
        String carryLabel = routineLabel("carry");

        emit(output, TokenType.ADD, low, src);
        emit(output, TokenType.JC, CodeGenUtils.labelRef(carryLabel));
        emit(output, TokenType.RET);
        output.addLabel(carryLabel);
        emit(output, TokenType.INC, high);
        emit(output, TokenType.RET);
    }

    private void emitAddWordWord(CompilerOutput output, Register dstWord, Register srcWord) {
        var dstLow = reg(low(dstWord));
        var dstHigh = reg(high(dstWord));
        var srcLow = reg(low(srcWord));
        var srcHigh = reg(high(srcWord));
        String carryLabel = routineLabel("carry");

        emit(output, TokenType.ADD, dstLow, srcLow);
        emit(output, TokenType.JC, CodeGenUtils.labelRef(carryLabel));
        emit(output, TokenType.ADD, dstHigh, srcHigh);
        emit(output, TokenType.RET);
        output.addLabel(carryLabel);
        emit(output, TokenType.ADD, dstHigh, srcHigh);
        emit(output, TokenType.INC, dstHigh);
        emit(output, TokenType.RET);
    }

    private void emitSubWordByte(CompilerOutput output, Register dstWord, Register srcByte) {
        var low = reg(low(dstWord));
        var high = reg(high(dstWord));
        var src = reg(srcByte);
        String borrowLabel = routineLabel("borrow");

        emit(output, TokenType.SUB, low, src);
        emit(output, TokenType.JC, CodeGenUtils.labelRef(borrowLabel));
        emit(output, TokenType.RET);
        output.addLabel(borrowLabel);
        emit(output, TokenType.DEC, high);
        emit(output, TokenType.RET);
    }

    private void emitSubWordWord(CompilerOutput output, Register dstWord, Register srcWord) {
        var dstLow = reg(low(dstWord));
        var dstHigh = reg(high(dstWord));
        var srcLow = reg(low(srcWord));
        var srcHigh = reg(high(srcWord));
        String borrowLabel = routineLabel("borrow");

        emit(output, TokenType.SUB, dstLow, srcLow);
        emit(output, TokenType.JC, CodeGenUtils.labelRef(borrowLabel));
        emit(output, TokenType.SUB, dstHigh, srcHigh);
        emit(output, TokenType.RET);
        output.addLabel(borrowLabel);
        emit(output, TokenType.SUB, dstHigh, srcHigh);
        emit(output, TokenType.DEC, dstHigh);
        emit(output, TokenType.RET);
    }

    private void emitIncWord(CompilerOutput output, Register dstWord) {
        var low = reg(low(dstWord));
        var high = reg(high(dstWord));
        String carryLabel = routineLabel("carry");

        emit(output, TokenType.ADD, low, CodeGenUtils.immByte(1));
        emit(output, TokenType.JC, CodeGenUtils.labelRef(carryLabel));
        emit(output, TokenType.RET);
        output.addLabel(carryLabel);
        emit(output, TokenType.INC, high);
        emit(output, TokenType.RET);
    }

    private void emitDecWord(CompilerOutput output, Register dstWord) {
        var low = reg(low(dstWord));
        var high = reg(high(dstWord));
        String borrowLabel = routineLabel("borrow");

        emit(output, TokenType.SUB, low, CodeGenUtils.immByte(1));
        emit(output, TokenType.JC, CodeGenUtils.labelRef(borrowLabel));
        emit(output, TokenType.RET);
        output.addLabel(borrowLabel);
        emit(output, TokenType.DEC, high);
        emit(output, TokenType.RET);
    }

    private Argument reg(Register register) {
        return CodeGenUtils.reg(register);
    }

    private void emit(CompilerOutput output, TokenType opcode, Argument... args) {
        output.append(CodeGenUtils.instr(opcode, args));
    }

    private Register high(Register wordRegister) {
        return wordRegister.getComponents()[0];
    }

    private Register low(Register wordRegister) {
        return wordRegister.getComponents()[1];
    }

    private static String routineLabel(String sublabelName) {
        return SUBLABEL_INITIATOR + sublabelName;
    }

    private static String normalizeWord(Register register) {
        return register.getRegName().replace(":", "").toLowerCase(Locale.ROOT);
    }

    private static String normalize(Register register) {
        return register.getRegName().replace(":", "").toLowerCase(Locale.ROOT);
    }

    private enum ExtensionKind {
        ADD_WORD_BYTE,
        ADD_WORD_WORD,
        SUB_WORD_BYTE,
        SUB_WORD_WORD,
        INC_WORD,
        DEC_WORD
    }

    private record ExtensionRequest(ExtensionKind kind, Register dstWord, Register src) {

        private String symbolName() {
            String dst = normalizeWord(dstWord);
            return switch (kind) {
                case ADD_WORD_BYTE -> "add_" + dst + "_" + normalize(src);
                case ADD_WORD_WORD -> "add_" + dst + "_" + normalizeWord(src);
                case SUB_WORD_BYTE -> "sub_" + dst + "_" + normalize(src);
                case SUB_WORD_WORD -> "sub_" + dst + "_" + normalizeWord(src);
                case INC_WORD -> "inc_" + dst;
                case DEC_WORD -> "dec_" + dst;
            };
        }
    }
}
