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

    public String requireCmpWordByte(Register leftReg, Register rightReg, TokenType targetFlag) {
        ExtensionKind extensionKind = switch (targetFlag){
            case JZ -> ExtensionKind.CMP_Z_WORD_BYTE;
            case JN -> ExtensionKind.CMP_N_WORD_BYTE;
            case JC -> ExtensionKind.CMP_C_WORD_BYTE;
            default -> throw new IllegalArgumentException("Invalid target flag: " + targetFlag);
        };
        return require(extensionKind, leftReg, rightReg);
    }
    public String requireCmpWordWord(Register leftReg, Register rightReg, TokenType targetFlag) {
        ExtensionKind extensionKind = switch (targetFlag){
            case JZ -> ExtensionKind.CMP_Z_WORD_WORD;
            case JN -> ExtensionKind.CMP_N_WORD_WORD;
            case JC -> ExtensionKind.CMP_C_WORD_WORD;
            default -> throw new IllegalArgumentException("Invalid target flag: " + targetFlag);
        };
        return require(extensionKind, leftReg, rightReg);
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
                .thenComparing(req -> req.a.ordinal())
                .thenComparing(req -> req.b == null ? -1 : req.b.ordinal()));

        for (var request : ordered) {
            emitRoutine(output, request);
        }

        return output;
    }

    private String require(ExtensionKind kind, Register a, Register b) {
        validateRequest(kind, a, b);
        ExtensionRequest request = new ExtensionRequest(kind, a, b);
        requiredExtensions.add(request);
        return request.symbolName();
    }

    private void validateRequest(ExtensionKind kind, Register dstWord, Register src) {
        if (dstWord == null || !dstWord.isCompound()) {
            throw new IllegalArgumentException("Destination must be a word register");
        }

        switch (kind) {
            case ADD_WORD_BYTE, SUB_WORD_BYTE, CMP_C_WORD_BYTE, CMP_Z_WORD_BYTE, CMP_N_WORD_BYTE -> {
                if (src == null || src.isCompound()) {
                    throw new IllegalArgumentException(kind + " requires a byte source register");
                }
            }
            case ADD_WORD_WORD, SUB_WORD_WORD, CMP_C_WORD_WORD, CMP_Z_WORD_WORD,  CMP_N_WORD_WORD -> {
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
            case ADD_WORD_BYTE -> emitAddWordByte(output, request.a, request.b);
            case ADD_WORD_WORD -> emitAddWordWord(output, request.a, request.b);
            case SUB_WORD_BYTE -> emitSubWordByte(output, request.a, request.b);
            case SUB_WORD_WORD -> emitSubWordWord(output, request.a, request.b);
            case INC_WORD -> emitIncWord(output, request.a);
            case DEC_WORD -> emitDecWord(output, request.a);
            case CMP_C_WORD_BYTE -> emitCmpCWordByte(output, request.a, request.b);
            case CMP_C_WORD_WORD -> emitCmpCWordWord(output, request.a, request.b);
            case CMP_Z_WORD_BYTE -> emitCmpZWordByte(output, request.a, request.b);
            case CMP_Z_WORD_WORD -> emitCmpZWordWord(output, request.a, request.b);
            case CMP_N_WORD_BYTE -> emitCmpNWordByte(output, request.a, request.b);
            case CMP_N_WORD_WORD -> emitCmpNWordWord(output, request.a, request.b);
        }
    }

    private void emitCmpNWordWord(CompilerOutput output, Register a, Register b) {
        var aLow = reg(low(a));
        var aHigh = reg(high(a));
        var bLow = reg(low(b));
        var bHigh = reg(high(b));

        var chkLow = routineLabel("chklow");
        var carryHigh = routineLabel("carryhigh");

        emit(output, TokenType.CMP, aHigh, bHigh);
        emit(output, TokenType.JN, CodeGenUtils.labelRef(carryHigh));
        emit(output, TokenType.JZ, CodeGenUtils.labelRef(chkLow));
        emit(output, TokenType.CLC);
        emit(output, TokenType.RET);
        output.addLabel(chkLow);
        emit(output, TokenType.CMP, aLow, bLow);
        emit(output, TokenType.JN, CodeGenUtils.labelRef(carryHigh));
        emit(output, TokenType.CLC);
        emit(output, TokenType.RET);
        output.addLabel(carryHigh);
        emit(output, TokenType.SEC);
        emit(output, TokenType.RET);
    }

    private void emitCmpNWordByte(CompilerOutput output, Register a, Register b) {
        var aLow = reg(low(a));
        var aHigh = reg(high(a));
        var src = reg(b);

        var chkLow = routineLabel("chklow");
        var carryHigh = routineLabel("carryhigh");

        emit(output, TokenType.CMP, aHigh, CodeGenUtils.immByte(0));
        emit(output, TokenType.JZ, CodeGenUtils.labelRef(chkLow));
        emit(output, TokenType.CLC);
        emit(output, TokenType.RET);
        output.addLabel(chkLow);
        emit(output, TokenType.CMP, aLow, src);
        emit(output, TokenType.JN, CodeGenUtils.labelRef(carryHigh));
        emit(output, TokenType.CLC);
        emit(output, TokenType.RET);
        output.addLabel(carryHigh);
        emit(output, TokenType.SEC);
        emit(output, TokenType.RET);
    }

    private void emitCmpZWordWord(CompilerOutput output, Register a, Register b) {
        var aLow = reg(low(a));
        var aHigh = reg(high(a));
        var bLow = reg(low(a));
        var bHigh = reg(high(a));

        var chkLow = routineLabel("chklow");
        var carryHigh = routineLabel("carryhigh");

        emit(output, TokenType.CMP, aHigh, bHigh);
        emit(output, TokenType.JZ, CodeGenUtils.labelRef(chkLow));
        emit(output, TokenType.CLC);
        emit(output, TokenType.RET);
        output.addLabel(chkLow);
        emit(output, TokenType.CMP, aLow, bLow);
        emit(output, TokenType.JZ, CodeGenUtils.labelRef(carryHigh));
        emit(output, TokenType.CLC);
        emit(output, TokenType.RET);
        output.addLabel(carryHigh);
        emit(output, TokenType.SEC);
        emit(output, TokenType.RET);
    }

    private void emitCmpZWordByte(CompilerOutput output, Register a, Register b) {
        var aLow = reg(low(a));
        var aHigh = reg(high(a));
        var src = reg(b);

        var chklow = routineLabel("chklow");
        var carryHigh = routineLabel("carryhigh");

        emit(output, TokenType.AND, aHigh, aHigh);
        emit(output, TokenType.JZ, CodeGenUtils.labelRef(chklow));
        emit(output, TokenType.CLC);
        emit(output, TokenType.RET);
        output.addLabel(chklow);
        //check low
        emit(output, TokenType.CMP, aLow, src);
        emit(output, TokenType.JZ, CodeGenUtils.labelRef(carryHigh));
        emit(output, TokenType.CLC);
        emit(output, TokenType.RET);
        //carry high
        output.addLabel(carryHigh);
        emit(output, TokenType.SEC);
        emit(output, TokenType.RET);
    }

    private void emitCmpCWordWord(CompilerOutput output, Register a, Register b) {
        var aLow = reg(low(a));
        var bLow = reg(low(b));
        var aHigh = reg(high(a));
        var bHigh = reg(high(b));

        var carry = routineLabel("carry");

        emit(output, TokenType.CMP, aHigh, bHigh);
        emit(output, TokenType.JC, CodeGenUtils.labelRef(carry));
        emit(output, TokenType.CMP, aLow, bLow);
        emit(output, TokenType.RET);
        output.addLabel(carry);
        emit(output, TokenType.SEC);
        emit(output, TokenType.RET);
    }

    private void emitCmpCWordByte(CompilerOutput output, Register a, Register b) {
        var low = reg(low(a));
        var high = reg(high(a));
        var src = reg(b);

        var checkLow = routineLabel("chklow");

        emit(output, TokenType.AND, high, high);
        emit(output, TokenType.JZ, CodeGenUtils.labelRef(checkLow));
        emit(output, TokenType.CLC);
        emit(output, TokenType.RET);
        output.addLabel(checkLow);
        emit(output, TokenType.CMP, low, src);
        emit(output, TokenType.RET);
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
        DEC_WORD,
        CMP_C_WORD_BYTE,
        CMP_C_WORD_WORD,
        CMP_Z_WORD_BYTE,
        CMP_Z_WORD_WORD,
        CMP_N_WORD_BYTE,
        CMP_N_WORD_WORD
    }

    private record ExtensionRequest(ExtensionKind kind, Register a, Register b) {

        private String symbolName() {
            String normA = normalizeWord(a);
            return switch (kind) {
                case ADD_WORD_BYTE -> "add_" + normA + "_" + normalize(b);
                case ADD_WORD_WORD -> "add_" + normA + "_" + normalizeWord(b);
                case SUB_WORD_BYTE -> "sub_" + normA + "_" + normalize(b);
                case SUB_WORD_WORD -> "sub_" + normA + "_" + normalizeWord(b);
                case INC_WORD -> "inc_" + normA;
                case DEC_WORD -> "dec_" + normA;
                case CMP_C_WORD_BYTE -> "cmp_c_" + normA + "_" + normalize(b);
                case CMP_C_WORD_WORD -> "cmp_c_" + normA + "_" + normalizeWord(b);
                case CMP_Z_WORD_BYTE -> "cmp_z_" + normA + "_" + normalize(b);
                case CMP_Z_WORD_WORD -> "cmp_z_" + normA + "_" + normalizeWord(b);
                case CMP_N_WORD_BYTE -> "cmp_n_" + normA + "_" + normalize(b);
                case CMP_N_WORD_WORD -> "cmp_n_" + normA + "_" + normalizeWord(b);
            };
        }
    }
}
