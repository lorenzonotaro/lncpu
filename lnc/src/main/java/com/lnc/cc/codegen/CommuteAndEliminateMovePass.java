package com.lnc.cc.codegen;

import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.cc.ast.BinaryExpression;
import com.lnc.common.frontend.TokenType;

public class CommuteAndEliminateMovePass extends AbstractAsmLevelLinearPass {

    @Override
    public Boolean visit(EncodedData encodedData) {
        return false;
    }

    @Override
    public Boolean visit(Instruction inst) {
        if(isCommutativeBinaryOp(inst.getOpcode().type)) {
            AsmCursor cur = getCursor();
            if(!cur.hasNext()){
                return false; // No next inst to commute with
            }
            var nextInstruction = cur.peekNext();

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

                // 3) Replace the ADD in‐place with the commuted form
                cur.replaceCurrent(commuted);

                cur.next(); // Move the cursor to the next instruction after replacement

                // 4) Remove the next instruction (the MOV)
                cur.removeCurrent();

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
