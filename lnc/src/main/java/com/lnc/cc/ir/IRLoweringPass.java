package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.Location;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.optimization.IRPass;

import java.util.ArrayList;
import java.util.List;

public class IRLoweringPass extends IRPass {
    @Override
    public Void visit(Goto aGoto) {
        return null;
    }

    @Override
    public Void visit(CondJump condJump) {

        IROperand lreg, rreg;
        if(condJump.getLeft().type != IROperand.Type.VIRTUAL_REGISTER){
            lreg = moveOrLoadIntoVR(condJump, condJump.getLeft());
        }else{
            lreg = condJump.getLeft();
        }

        if(condJump.getRight().type != IROperand.Type.VIRTUAL_REGISTER && condJump.getRight().type != IROperand.Type.IMMEDIATE){
            rreg = moveOrLoadIntoVR(condJump, condJump.getRight());
        }else{
            rreg = condJump.getRight();
        }

        condJump.setLeft(lreg);
        condJump.setRight(rreg);

        return null;
    }

    private IROperand moveOrLoadIntoVR(IRInstruction instr, IROperand operand) {
        return moveOrLoadIntoVR(instr, operand, RegisterClass.ANY);
    }

    private IROperand moveOrLoadIntoVR(IRInstruction instr, IROperand operand, RegisterClass registerClass) {
        if(operand.type == IROperand.Type.VIRTUAL_REGISTER) {
            return restrictOrMoveTo((VirtualRegister) operand, registerClass, instr);
        } else if(operand.type == IROperand.Type.IMMEDIATE) {
            VirtualRegisterManager vrm = getUnit().getVrManager();
            VirtualRegister vr = vrm.getRegister(operand.getTypeSpecifier());
            vr.setRegisterClass(registerClass);
            instr.insertBefore(new Move(operand, vr));
            return operand;
        } else if(operand.type == IROperand.Type.LOCATION) {
            // Otherwise, we need to move or load it into a virtual register
            VirtualRegisterManager vrm = getUnit().getVrManager();
            VirtualRegister vr = vrm.getRegister(operand.getTypeSpecifier());
            vr.setRegisterClass(registerClass);
            instr.insertBefore(new Load((Location) operand, vr));
            return vr;
        } else {
            VirtualRegisterManager vrm = getUnit().getVrManager();
            VirtualRegister vr = vrm.getRegister(operand.getTypeSpecifier());
            vr.setRegisterClass(registerClass);
            instr.insertBefore(new Move(operand, vr));
            return vr;
        }
    }

    private IROperand restrictOrMoveTo(VirtualRegister register, RegisterClass registerClass, IRInstruction inst) {
        if(register.getRegisterClass() == registerClass) {
            return register;
        } else {
            VirtualRegisterManager vrm = getUnit().getVrManager();
            VirtualRegister newVR = vrm.getRegister(register.getTypeSpecifier());
            newVR.setRegisterClass(registerClass);
            inst.insertBefore(new Move(register, newVR));
            return newVR;
        }
    }

    @Override
    public Void visit(Load load) {
        return null;
    }

    @Override
    public Void visit(Move move) {
        return null;
    }

    @Override
    public Void visit(Store store) {
        return null;
    }

    @Override
    public Void visit(Ret ret) {

        if(ret.getValue() != null && ret.getValue().type != IROperand.Type.VIRTUAL_REGISTER) {
            ret.setValue(moveOrLoadIntoVR(ret, ret.getValue()));
        }

        return null;
    }

    @Override
    public Void visit(Bin bin) {

        IROperand left = bin.getLeft();
        IROperand right = bin.getRight();

        if(left.type != IROperand.Type.VIRTUAL_REGISTER) {
            left = moveOrLoadIntoVR(bin, left);
            bin.setLeft(left);
        }

        if(right.type != IROperand.Type.VIRTUAL_REGISTER && right.type != IROperand.Type.IMMEDIATE) {
            right = moveOrLoadIntoVR(bin, right);
            bin.setRight(right);
        }

        return null;

    }

    @Override
    public Void visit(Call call) {
        List<IROperand> originalArgs = List.of(call.getArguments());
        List<IROperand> loweredArgs = new ArrayList<>();
        boolean hasWordArg = originalArgs.stream().anyMatch(IRLoweringPass::isWordOperand);

        int byteIndex = 0;

        for (int i = 0; i < originalArgs.size(); i++) {
            IROperand arg = originalArgs.get(i);

            if (isWordOperand(arg)) {
                if (i == 0) {
                    // First word argument → RC:RD
                    IROperand lowered = moveOrLoadIntoVR(call, arg, RegisterClass.WORDPARAM_1);
                    loweredArgs.add(lowered);
                } else {
                    call.insertBefore(
                            new Push(arg)
                    );
                    loweredArgs.add(arg); // record original for structure (even if not used directly)
                }
            } else {
                RegisterClass regClass = switch (byteIndex++) {
                    case 0 -> RegisterClass.BYTEPARAM_1;
                    case 1 -> hasWordArg ? null : RegisterClass.BYTEPARAM_3;
                    case 2 -> hasWordArg ? null : RegisterClass.BYTEPARAM_4;
                    default -> null;
                };

                if (regClass != null) {
                    IROperand lowered = moveOrLoadIntoVR(call, arg, regClass);
                    loweredArgs.add(lowered);
                } else {
                    call.insertBefore(
                            new Push(arg)
                    );
                    loweredArgs.add(arg);
                }
            }
        }

        // Handle result
        IROperand originalResult = call.getReturnTarget();
        if (originalResult != null) {
            RegisterClass retClass = isWordOperand(originalResult)
                    ? RegisterClass.RET_WORD
                    : RegisterClass.RET_BYTE;

            IROperand loweredResult = moveOrLoadIntoVR(call, originalResult, retClass);
            call.setReturnTarget(loweredResult);
        }

        // Replace arguments in-place on the original call
        call.setArguments(loweredArgs.toArray(new IROperand[0]));

        return null;
    }

    private static boolean isWordOperand(IROperand arg) {
        return arg.getTypeSpecifier().typeSize() == 2;
    }


    @Override
    public Void visit(Unary unary) {
        return null;
    }

    @Override
    public Void visit(Push push) {
        return null;
    }
}
