package com.lnc.cc.optimization.asm;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.assembler.parser.argument.Register;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.TokenType;

public class RedundantRegisterMoveEliminationPass extends AbstractAsmLevelLinearPass{
    @Override
    public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {
        if(instruction.getOpcode().type == TokenType.MOV){
            var args = instruction.getArguments();
            if(args.length == 2 && args[0].type == Argument.Type.REGISTER && args[1].type == Argument.Type.REGISTER){
                // Check if the move is redundant (i.e., moving a register to itself)
                if(((Register) args[0]).reg.equals(((Register)args[1]).reg)){
                    var labels = instruction.getLabels();
                    iterator.removeCurrent(); // Remove the redundant move instruction

                    // If the instruction had labels, we need to reattach them to the next instruction
                    if(iterator.hasNext()){
                        var next = iterator.peek();
                        next.getLabels().addAll(0, labels);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
