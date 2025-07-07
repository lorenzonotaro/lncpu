package com.lnc.cc.codegen;

import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.assembler.parser.argument.LabelRef;
import com.lnc.common.frontend.TokenType;

public class RedundantGotoEliminationPass extends AbstractAsmLevelLinearPass {
    @Override
    public Boolean visit(EncodedData encodedData) {
        return false;
    }

    @Override
    public Boolean visit(Instruction instruction) {
        if (instruction.getOpcode().type == TokenType.GOTO && getCursor().hasNext()) {
            var target = instruction.getArguments()[0];

            if (target.type == Argument.Type.LABEL){
                var labelArg = (LabelRef) target;
                var nextInstruction = getCursor().peekNext();
                if (nextInstruction instanceof Instruction nextInst &&
                    nextInst.getLabels().stream().anyMatch(l -> l.name().equals(labelArg.labelToken.lexeme))){
                    // If the next instruction is a label that matches the goto target, we can eliminate the goto
                    getCursor().removeCurrent(); // Remove the goto instruction
                    return true;
                }
            }
        }
        return false;
    }
}
