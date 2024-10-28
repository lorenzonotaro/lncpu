package com.lnc.cc.ir;

public abstract class BranchingIRVisitor implements IIRVisitor<Void> {

    @Override
    public Void accept(Jge jge) {

        visit(jge.getFallThrough());
        visit(jge.getTarget());
        visit(jge.getContinueTo());

        return null;
    }

    @Override
    public Void accept(Jle jle) {

        visit(jle.getFallThrough());
        visit(jle.getTarget());
        visit(jle.getContinueTo());


        return null;
    }

    @Override
    public Void accept(Jne jne) {

        visit(jne.getFallThrough());
        visit(jne.getTarget());
        visit(jne.getContinueTo());

        return null;
    }

    @Override
    public Void accept(Jeq je) {

        visit(je.getFallThrough());
        visit(je.getTarget());
        visit(je.getContinueTo());

        return null;
    }

    @Override
    public Void accept(Jlt jle) {

        visit(jle.getFallThrough());
        visit(jle.getTarget());
        visit(jle.getContinueTo());

        return null;
    }

    @Override
    public Void accept(Jgt jgt) {

        visit(jgt.getFallThrough());
        visit(jgt.getTarget());
        visit(jgt.getContinueTo());

        return null;
    }

    protected void visit(IRBlock block) {

        if(block == null) {
            return;
        }

        for (IRInstruction instruction : block.getInstructions()) {
            instruction.accept(this);
        }

        if(block.hasNext()) {
            visit(block.getNext());
        }
    }
}
