package com.lnc.cc.optimization.asm;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.TokenType;

public class CommuteAndEliminateMovePass extends AbstractAsmLevelLinearPass {

    @Override
    public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public Boolean visit(Instruction inst, ExtendedListIterator<CodeElement> iterator) {
        if(isCommutativeBinaryOp(inst.getOpcode().type)) {
            if(!iterator.hasNext()){
                return false; // No next inst to commute with
            }
            var nextInstruction = iterator.peek();

            if(nextInstruction instanceof Instruction nextInst &&
               nextInst.getOpcode().type == TokenType.MOV &&
               nextInst.getArguments().length == 2 &&
               nextInst.getArguments()[1].toString().equals(inst.getArguments()[1].toString()) &&
               nextInst.getArguments()[0].toString().equals(inst.getArguments()[0].toString())) {


                Instruction commuted = new Instruction(
                        inst.getOpcode(),
                        new Argument[]{
                                nextInst.getArguments()[1], // Use the second argument of the move
                                nextInst.getArguments()[0]  // Use the first argument of the move
                        }
                );

                commuted.setLabels(inst.getLabels());
                inst.clearLabels();

                // 3) Replace the ADD in‐place with the commuted form
                iterator.removeCurrent();

                var movLabels = iterator.next().getLabels();

                iterator.addBeforeCurrent(commuted);

                iterator.removeCurrent();

                if(iterator.hasNext()){
                    iterator.peek().setLabels(movLabels);
                }

                return true; // Indicate that a change was made
            }
        }
        return false;
    }

    private static boolean isCommutativeBinaryOp(TokenType tt){
        return switch (tt) {
            case ADD, AND, OR, XOR -> true;
            default -> false;
        };
    }

}
