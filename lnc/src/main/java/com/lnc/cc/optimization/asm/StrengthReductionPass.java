package com.lnc.cc.optimization.asm;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.assembler.parser.argument.Byte;
import com.lnc.assembler.parser.argument.Register;
import com.lnc.assembler.parser.argument.ReservedSpace;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

/**
 * Rewrites {@code add R, 1} to {@code inc R} and {@code sub R, 1} to {@code dec R}, saving one
 * byte and one clock cycle each.
 *
 * The control-unit microcode for {@code inc}/{@code dec} is the same ALU operation and the same
 * FLAGS_MODE_SEL as the corresponding {@code add}/{@code sub} against an immediate, so N, Z and C
 * are all preserved and the rewrite is unconditional. This does not apply to SP/BP, which have
 * {@code add}/{@code sub} immediate forms but no {@code inc}/{@code dec}.
 */
public class StrengthReductionPass extends AbstractAsmLevelLinearPass {

    @Override
    public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public Boolean visit(ReservedSpace reservedSpace, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {
        TokenType replacement = switch (instruction.getOpcode().type) {
            case ADD -> TokenType.INC;
            case SUB -> TokenType.DEC;
            default -> null;
        };

        if (replacement == null) {
            return false;
        }

        Argument[] args = instruction.getArguments();
        if (args.length != 2 || !isGeneralPurposeRegister(args[0]) || !isOne(args[1])) {
            return false;
        }

        Instruction rewritten = new Instruction(
                Token.__internal(replacement, replacement.toString()),
                new Argument[]{args[0]});
        rewritten.setSourceToken(instruction.getSourceToken());
        rewritten.setLabels(instruction.getLabels());
        iterator.set(rewritten);
        return true;
    }

    private static boolean isGeneralPurposeRegister(Argument argument) {
        return argument instanceof Register register && register.reg.generalPurpose;
    }

    private static boolean isOne(Argument argument) {
        return argument instanceof Byte immediate && immediate.value == 1;
    }
}
