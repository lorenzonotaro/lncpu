package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.*;

public abstract class LinearIROptimizerPass implements ILinearIRVisitor<Boolean, Boolean> {


    private LinearIRUnit currentUnit;

    /**
     * Runs the optimization pass on the given linear IR unit.
     * @param unit The linear IR unit to optimize.
     * @return true if the unit was modified, false otherwise.
     */
    public boolean apply(LinearIRUnit unit){

        boolean result = false;

        setCurrentUnit(unit);

        IRInstruction current = unit.head;

        while(current != null){
            result |= visit(current);
            current = current.getNext();
        }

        return result;
    }

    protected void remove(IRInstruction instruction){
        if(instruction.getPrev() != null){
            instruction.getPrev().setNext(instruction.getNext());
        }

        if(instruction.getNext() != null){
            instruction.getNext().setPrev(instruction.getPrev());
        }

        instruction.onRemove();

        if(instruction == currentUnit.head){
            currentUnit.head = instruction.getNext();
        }
    }

    protected boolean visit(IRInstruction instruction){
        return instruction.accept(this);
    }

    protected void setCurrentUnit(LinearIRUnit unit) {
        this.currentUnit = unit;
    }

    @Override
    public Boolean accept(Call call) {
        return false;
    }

    @Override
    public Boolean accept(Bin bin) {
        return false;
    }

    @Override
    public Boolean accept(Not not) {
        return false;
    }

    @Override
    public Boolean accept(Neg neg) {
        return false;
    }

    @Override
    public Boolean accept(Ret sub) {
        return false;
    }

    @Override
    public Boolean accept(Store store) {
        return false;
    }

    @Override
    public Boolean accept(Move move) {
        return false;
    }

    @Override
    public Boolean accept(Load load) {
        return false;
    }

    @Override
    public Boolean accept(Jlt jle) {
        return false;
    }

    @Override
    public Boolean accept(Jeq je) {
        return false;
    }

    @Override
    public Boolean accept(Jle jle) {
        return false;
    }

    @Override
    public Boolean accept(Inc inc) {
        return false;
    }

    @Override
    public Boolean accept(Dec dec) {
        return false;
    }

    @Override
    public Boolean accept(Goto aGoto) {
        return false;
    }

    @Override
    public Boolean accept(Label label) {
        return false;
    }

    @Override
    public Boolean accept(ImmediateOperand immediateOperand) {
        return null;
    }

    @Override
    public Boolean accept(VirtualRegister vr) {
        return null;
    }

    @Override
    public Boolean accept(RegisterDereference rd) {
        return null;
    }

    @Override
    public Boolean accept(Location location) {
        return null;
    }

    @Override
    public Boolean accept(AddressOf addressOf) {
        return null;
    }
}
