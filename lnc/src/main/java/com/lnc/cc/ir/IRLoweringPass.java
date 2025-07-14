package com.lnc.cc.ir;

import com.lnc.cc.ast.UnaryExpression;
import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.operands.StructMemberAccess;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.types.FunctionType;
import com.lnc.cc.types.TypeSpecifier;

import java.util.ArrayList;
import java.util.List;

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
            if(registerClass == RegisterClass.ANY || vr.getRegisterClass() == registerClass) {
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
        } else {
            return move(operand, registerClass);
        }
    }

    private IROperand move(IROperand operand, RegisterClass registerClass) {
        if(operand.type == IROperand.Type.LOCATION) {
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
        move.setSource(move.getSource().accept(this));
        move.setDest(move.getDest().accept(this));
        return null;
    }

    @Override
    public Void visit(Store store) {

        IROperand value = store.getValue().accept(this);
        IROperand location = store.getDest().accept(this);

        if(location.type == IROperand.Type.VIRTUAL_REGISTER){
            replaceAndContinue(new Move(value, location));
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
                args[i] = moveOrLoadIntoVR(args[i], loc.regClass());
            }
        }

        call.setArguments(args);

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
    public Void visit(Push push) {

        IROperand operand = push.getArg().accept(this);
        push.setArg(operand);

        return null;
    }

    @Override
    public Void accept(LoadParam loadParam) {
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        IROperand operand = unary.getOperand().accept(this);
        IROperand target = unary.getTarget().accept(this);

        unary.setOperand(moveOrLoadIntoVR(operand));
        unary.setTarget(target);

        if(unary.getOperator() == UnaryExpression.Operator.INCREMENT || unary.getOperator() == UnaryExpression.Operator.DECREMENT && operand.type != IROperand.Type.VIRTUAL_REGISTER) {
            getCurrentInstruction().insertAfter(new Move(target, operand));
        }

        return null;
    }

    @Override
    public Void visit(Deref deref) {
        IROperand operand = deref.getOperand().accept(this);
        IROperand result = deref.getResult().accept(this);

        if(operand.type != IROperand.Type.VIRTUAL_REGISTER) {
            operand = moveOrLoadIntoVR(operand, RegisterClass.DEREF);
        }

        deref.setOperand(operand);
        deref.setResult(result);

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

        var resolvedLocal = getUnit().getLocalMappingInfo().mappings().get(location.getSymbol().getName());

        return resolvedLocal != null ? resolvedLocal : location;
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
        VirtualRegisterManager vrm = unit.getVrManager();

        unit.compileLocalMappings();

        var parameters = unit.getLocalMappingInfo().originalRegParamMappings().entrySet().stream().toList();

        List<IRInstruction> list = new ArrayList<>();
        for(var parameter : parameters){
            list.add(new LoadParam(parameter.getValue()));
        }

        for(var parameter : parameters){
            list.add(new Move(parameter.getValue(), unit.getLocalMappingInfo().mappings().get(parameter.getKey())));
        }

        unit.prependEntryBlock(list);

        unit.getEntryBlock().prependSequence(list);

        super.visit(unit);
    }
}
