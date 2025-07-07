package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.common.BaseSymbol;
import com.lnc.cc.ir.operands.StructMemberAccess;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.optimization.IRPass;
import com.lnc.cc.types.FunctionType;

import java.util.ArrayList;
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
            lreg = moveOrLoadIntoVR(lreg);
        }

        if(rreg.type != IROperand.Type.VIRTUAL_REGISTER && rreg.type != IROperand.Type.IMMEDIATE){
            rreg = moveOrLoadIntoVR(rreg);
        }

        condJump.setLeft(lreg);
        condJump.setRight(rreg);

        return null;
    }

    private IROperand moveOrLoadIntoVR(IROperand operand) {
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
            return vr;
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

        if(ret.getValue() != null) {
            ret.setValue(moveOrLoadIntoVR(ret.getValue(), RegisterClass.RETURN));
        }

        return null;
    }

    @Override
    public Void visit(Bin bin) {

        IROperand left = bin.getLeft().accept(this);
        IROperand right = bin.getRight().accept(this);

        if(left.type != IROperand.Type.VIRTUAL_REGISTER) {
            left = moveOrLoadIntoVR(left);
        }

        bin.setLeft(left);

        if(right.type != IROperand.Type.VIRTUAL_REGISTER && right.type != IROperand.Type.IMMEDIATE) {
            right = moveOrLoadIntoVR(right);
        }

        bin.setRight(right);

        return null;

    }

    @Override
    public Void visit(Call call) {
        IROperand[] args = call.getArguments();

        var funType = (FunctionType) call.getCallee().getTypeSpecifier();

        record StackArg(IROperand operand, int offset){}

        List<StackArg> stackArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            CallingConvention.ParamLocation loc = funType.getParameterMapping().get(i);
            if (loc.onStack()) {
                stackArgs.add(new StackArg(args[i], loc.stackOffset()));
            } else {
                moveOrLoadIntoVR(args[i], loc.regClass());
            }
        }

        // Sort descending by stack offset to ensure we push the highest offset first
        stackArgs.sort((a, b) -> Integer.compare(b.offset, a.offset));

        for(StackArg sa : stackArgs) {
            call.insertBefore(new Push(sa.operand));
        }

        RegisterClass retRC = CallingConvention.returnRegisterFor(call.getReturnTarget().getTypeSpecifier());
        call.getReturnTarget().setRegisterClass(retRC);

        return null;
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
            CallingConvention.ParamLocation paramLocation = getUnit().getFunctionType().getParameterMapping().get(bs.getParameterIndex());
            if(paramLocation.onStack()){
                int offset = paramLocation.stackOffset();
                return new StackFrameOperand(location.getTypeSpecifier(), offset);
            }else{
                VirtualRegisterManager vrm = getUnit().getVrManager();
                VirtualRegister vr = vrm.getRegister(location.getTypeSpecifier());
                vr.setRegisterClass(paramLocation.regClass());
                return vr;
            }
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
