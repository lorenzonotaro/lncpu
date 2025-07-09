package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.common.BaseSymbol;
import com.lnc.cc.ir.operands.StructMemberAccess;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.optimization.IRPass;
import com.lnc.cc.types.FunctionType;
import com.lnc.cc.types.TypeSpecifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRLoweringPass extends GraphicalIRVisitor implements IIROperandVisitor<IROperand> {
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
            VirtualRegister vr = (VirtualRegister) operand;
            if(vr.getRegisterClass() == registerClass) {
                return vr; // Already in the correct register class
            } else {
                return move(vr, registerClass); // Move to the correct register class
            }
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

    @Override
    public Void visit(Load load) {
        var dest = load.getDest().accept(this);
        var src = load.getSrc().accept(this);

        if(src.type == IROperand.Type.VIRTUAL_REGISTER){
            replaceAndContinue(new Move(src, dest));
        }

        return null;
    }

    @Override
    public Void visit(Move move) {

        return null;
    }

    @Override
    public Void visit(Store store) {

        IROperand value = store.getValue().accept(this);
        IROperand location = store.getDest().accept(this);

        if(location.type == IROperand.Type.VIRTUAL_REGISTER){
            replaceAndContinue(new Move(location, value));
        }

        return null;
    }

    @Override
    public Void visit(Ret ret) {

        if(ret.getValue() != null) {
            ret.setValue(moveOrLoadIntoVR(ret.getValue().accept(this), RegisterClass.RETURN));
        }

        return null;
    }

    private IROperand move(IROperand value, RegisterClass registerClass) {
        if(value.type == IROperand.Type.LOCATION) {
            VirtualRegisterManager vrm = getUnit().getVrManager();
            VirtualRegister vr = vrm.getRegister(value.getTypeSpecifier());
            vr.setRegisterClass(registerClass);
            getCurrentInstruction().insertBefore(new Load((Location) value, vr));
            return vr;
        } else {
            VirtualRegisterManager vrm = getUnit().getVrManager();
            VirtualRegister vr = vrm.getRegister(value.getTypeSpecifier());
            vr.setRegisterClass(registerClass);
            getCurrentInstruction().insertBefore(new Move(value, vr));
            return vr;
        }
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

        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].accept(this);
        }

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

        if(funType.returnType.type != TypeSpecifier.Type.VOID){
            RegisterClass retRC = CallingConvention.returnRegisterFor(call.getReturnTarget().getTypeSpecifier());
            call.getReturnTarget().setRegisterClass(retRC);
        }

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

        if(location.getSymbol().isParameter()){
            return getUnit().getParameterOperandMapping().get(location.getSymbol().getName());
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

    @Override
    public void visit(IRUnit unit) {
        Map<String, IROperand> parameterMapping = new HashMap<>();
        var parameters = CallingConvention.mapCallArguments(unit.getFunctionDeclaration().parameters);
        for(var parameter : parameters){
            if(parameter.onStack()) {
                // If the parameter is on the stack, we create a StackFrameOperand
                int offset = parameter.stackOffset();
                IROperand operand = new StackFrameOperand(parameter.type(), StackFrameOperand.OperandType.PARAMETER, offset);
                parameterMapping.put(parameter.name(), operand);
            } else {
                // Otherwise, we create a VirtualRegister assigned to the class and a move to a new virtual register
                // of class any, that will be coalesced later if not necessary.
                VirtualRegisterManager vrm = unit.getVrManager();

                VirtualRegister vr = vrm.getRegister(parameter.type());
                vr.setRegisterClass(parameter.regClass());

                VirtualRegister movedVr = vrm.getRegister(parameter.type());
                movedVr.setRegisterClass(RegisterClass.ANY); // Set to ANY for coalescing later

                // we store the moved Vr in the parameter mapping
                parameterMapping.put(parameter.name(), movedVr);

                unit.getEntryBlock().emitFirst(new Move(vr, movedVr));
            }
        }
        unit.setParameterOperandMapping(parameterMapping);

        super.visit(unit);
    }
}
