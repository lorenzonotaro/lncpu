package com.lnc.cc.optimization.asm;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.TokenType;

import java.util.Arrays;

public class RedundantCmpPass extends AbstractAsmLevelLinearPass {
    @Override
    public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {
        if(instruction.getOpcode().type == TokenType.SUB && iterator.hasNext()){
            CodeElement next = iterator.peek();
            if(
                    next.getLabels().isEmpty() && // The next element must have no labels
                    next instanceof Instruction nextInst && nextInst.getOpcode().type == TokenType.CMP && // The next element must be a CMP instruction
                            Arrays.equals(nextInst.getArguments(), instruction.getArguments()) // The arguments of the CMP must match the SUB
            ){
                // Remove the redundant CMP after SUB
                iterator.removeNext();
                return true;
            }
        }
        return false;
    }
}
