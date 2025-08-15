package com.lnc.cc.codegen;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.TokenType;

public class TwoWayMoveEliminationPass extends AbstractAsmLevelLinearPass{
    @Override
    public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    /* If:
    *   - the current move is followed by another move instruction AND
    *   - the next move instruction is moving the dest of the current move to the source of the current move AND
    *   - the next move has no labels
    * Then:
    *  - remove the next move instruction
    */
    @Override
    public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {
        if(instruction.getOpcode().type == TokenType.MOV && iterator.hasNext() && iterator.peek() instanceof Instruction nextInstr && nextInstr.getOpcode().type == TokenType.MOV){
            if(nextInstr.getArguments()[0].equals(instruction.getArguments()[1]) &&
               nextInstr.getArguments()[1].equals(instruction.getArguments()[0]) &&
               nextInstr.getLabels().isEmpty()) {
                // Remove the next move instruction
                iterator.removeNext();
                return true; // Indicate that a change was made
            }
        }
        return false;
    }
}
