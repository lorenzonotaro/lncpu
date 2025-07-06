package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.common.BaseSymbol;
import com.lnc.cc.common.StructMemberAccess;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.optimization.IRPass;

import java.util.Arrays;
import java.util.List;

public class IRLoweringPass extends IRPass implements IIROperandVisitor<IROperand> {
    @Override
    public Void visit(Goto aGoto) {
        return null;
    }

    @Override
    public Void visit(CondJump condJump) {

        IROperand lreg = condJump.getLeft().accept(this), rreg = condJump.getRight().accept(this);
        if(lreg.type != IROperand.Type.VIRTUAL_REGISTER){
            lreg = moveOrLoadIntoVR(condJump, lreg);
        }

        if(rreg.type != IROperand.Type.VIRTUAL_REGISTER && rreg.type != IROperand.Type.IMMEDIATE){
            rreg = moveOrLoadIntoVR(condJump, rreg);
        }

        condJump.setLeft(lreg);
        condJump.setRight(rreg);

        return null;
    }

    private IROperand moveOrLoadIntoVR(IRInstruction instr, IROperand operand) {
        return moveOrLoadIntoVR(operand, RegisterClass.ANY);
    }

    private IROperand moveOrLoadIntoVR(IROperand operand, RegisterClass registerClass) {
        if(operand.type == IROperand.Type.VIRTUAL_REGISTER) {
            return restrictOrMoveTo((VirtualRegister) operand, registerClass);
        } else if(operand.type == IROperand.Type.IMMEDIATE) {
            VirtualRegisterManager vrm = getUnit().getVrManager();
            VirtualRegister vr = vrm.getRegister(operand.getTypeSpecifier());
            vr.setRegisterClass(registerClass);
            getCurrentInstruction().insertBefore(new Move(operand, vr));
            return operand;
        } else if(operand.type == IROperand.Type.LOCATION) {
            // Otherwise, we need to move or load it into a virtual register
            VirtualRegisterManager vrm = getUnit().getVrManager();
            VirtualRegister vr = vrm.getRegister(operand.getTypeSpecifier());
            vr.setRegisterClass(registerClass);
            getCurrentInstruction().insertBefore(new Load((Location) operand, vr));
            return vr;
        } else {
            VirtualRegisterManager vrm = getUnit().getVrManager();
            VirtualRegister vr = vrm.getRegister(operand.getTypeSpecifier());
            vr.setRegisterClass(registerClass);
            getCurrentInstruction().insertBefore(new Move(operand, vr));
            return vr;
        }
    }

    private IROperand restrictOrMoveTo(VirtualRegister register, RegisterClass registerClass) {
        if(register.getRegisterClass() == registerClass) {
            return register;
        } else {
            VirtualRegisterManager vrm = getUnit().getVrManager();
            VirtualRegister newVR = vrm.getRegister(register.getTypeSpecifier());
            newVR.setRegisterClass(registerClass);
            getCurrentInstruction().insertBefore(new Move(register, newVR));
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
            ret.setValue(moveOrLoadIntoVR(ret.getValue(), RegisterClass.RETURN));
        }

        return null;
    }

    @Override
    public Void visit(Bin bin) {

        IROperand left = bin.getLeft().accept(this);
        IROperand right = bin.getRight().accept(this);

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
        IROperand[] originalArgs = call.getArguments();
        boolean hasWordArg = Arrays.stream(originalArgs).anyMatch(IRLoweringPass::isWordOperand);

        int byteIndex = 0;

        for (int i = 0; i < originalArgs.length; i++) {
            IROperand arg = originalArgs[i].accept(this);

            if (isWordOperand(arg)) {
                if (i == 0) {
                    // First word argument → RC:RD
                    moveOrLoadIntoVR(arg, RegisterClass.WORDPARAM_1);
                } else {
                    call.insertBefore(
                            new Push(arg)
                    );
                }
            } else {
                RegisterClass regClass = switch (byteIndex++) {
                    case 0 -> RegisterClass.BYTEPARAM_1;
                    case 1 -> hasWordArg ? null : RegisterClass.BYTEPARAM_3;
                    case 2 -> hasWordArg ? null : RegisterClass.BYTEPARAM_4;
                    default -> null;
                };

                if (regClass != null) {
                    moveOrLoadIntoVR(arg, regClass);
                } else {
                    call.insertBefore(
                            new Push(arg)
                    );
                }
            }
        }

        // Handle result
        VirtualRegister originalResult = call.getReturnTarget();
        if (originalResult != null) {
            RegisterClass retClass = isWordOperand(originalResult)
                    ? RegisterClass.RET_WORD
                    : RegisterClass.RET_BYTE;

            originalResult.setRegisterClass(retClass);
        }

        // Replace arguments in-place on the original call
        call.setArguments(new IROperand[0]);

        return null;
    }

    private static boolean isWordOperand(IROperand arg) {
        return arg.getTypeSpecifier().allocSize() == 2;
    }


    @Override
    public Void visit(Unary unary) {

        unary.setOperand(unary.getOperand().accept(this));

        return null;
    }

    @Override
    public Void visit(Push push) {

        IROperand operand = push.getArg().accept(this);
        push.setArg(operand);

        return null;
    }

    @Override
    public IROperand visit(ImmediateOperand immediateOperand) {
        return immediateOperand;
    }

    @Override
    public IROperand visit(VirtualRegister vr) {
        return vr;
    }

    @Override
    public IROperand visit(Location location) {

        if(location.getSymbol() instanceof BaseSymbol bs && bs.isParameter()){
            // Replace the parameter symbol with its respective (vr or stack frame offset) location, following the calling convention
        }

        return location;
    }

    @Override
    public IROperand visit(AddressOf addressOf) {
        //TODO
        return addressOf;
    }

    @Override
    public IROperand visit(StructMemberAccess structMemberAccess) {
        //TODO
        return structMemberAccess;
    }

    @Override
    public IROperand visit(ArrayElementAccess arrayElementAccess) {
        //TODO
        return arrayElementAccess;
    }

    @Override
    public IROperand visit(StackFrameOperand stackFrameOperand) {
        //TODO
        return stackFrameOperand;
    }
}
