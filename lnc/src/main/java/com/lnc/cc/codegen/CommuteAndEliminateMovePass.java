package com.lnc.cc.codegen;

import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.cc.ast.BinaryExpression;
import com.lnc.common.frontend.TokenType;

public class CommuteAndEliminateMovePass extends AbstractAsmLevelLinearPass {

    @Override
    public Boolean visit(EncodedData encodedData) {
        return null;
    }

    @Override
    public Boolean visit(Instruction instruction) {
        if(isCommutativeBinaryOp(instruction.getOpcode().type)) {
            if(!getCursor().hasNext()){
                return false; // No next instruction to commute with
            }
            var nextInstruction = getCursor().peekNext();

            if(nextInstruction instanceof Instruction nextInst &&
               nextInst.getOpcode().type == TokenType.MOV &&
               nextInst.getArguments().length == 2 &&
               nextInst.getArguments()[1].equals(instruction.getArguments()[1]) &&
               nextInst.getArguments()[0].equals(instruction.getArguments()[0])) {


                Instruction commuted = new Instruction(
                        instruction.getOpcode(),
                        new Argument[]{
                                nextInst.getArguments()[1], // Use the second argument of the move
                                nextInst.getArguments()[0]  // Use the first argument of the move
                        }
                );

                getCursor().replaceCurrent(commuted);

                getCursor().next(); // Move to the next instruction after the current one
                getCursor().removeCurrent();  // Remove the move instruction

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
