package com.lnc.cc.codegen;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.assembler.parser.argument.LabelRef;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.TokenType;

public class RedundantGotoEliminationPass extends AbstractAsmLevelLinearPass {
    @Override
    public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {
        if (instruction.getOpcode().type == TokenType.GOTO && iterator.hasNext()) {
            var target = instruction.getArguments()[0];

            if (target.type == Argument.Type.LABEL){
                var labelArg = (LabelRef) target;
                var nextInstruction = iterator.peek();
                if (nextInstruction instanceof Instruction nextInst &&
                    nextInst.getLabels().stream().anyMatch(l -> l.extractSubLabelName().equals(labelArg.labelToken.lexeme))){
                    // If the next instruction is a label that matches the goto target, we can eliminate the goto
                    iterator.removeCurrent(); // Remove the goto instruction
                    return true;
                }
            }
        }
        return false;
    }
}
