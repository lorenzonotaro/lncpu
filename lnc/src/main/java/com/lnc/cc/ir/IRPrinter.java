package com.lnc.cc.ir;

public class IRPrinter extends BranchingIRVisitor{

    public void print(IRUnit unit) {

        System.out.println("Function: " + unit.getFunctionDeclaration());

        System.out.println("Symbol Table: ");

        System.out.println(unit.getSymbolTable());

        System.out.println("IR:\n");

        visit(unit.getStartBlock());
    }

    @Override
    public Void accept(Goto aGoto) {
        System.out.println("    " + aGoto);

        return null;
    }

    @Override
    public Void accept(Dec dec) {
        System.out.println("    " + dec);

        return null;
    }

    @Override
    public Void accept(Inc inc) {
        System.out.println("    " + inc);

        return null;
    }

    @Override
    public Void accept(Jge jge) {
        System.out.println("    " + jge);

        super.accept(jge);

        return null;
    }

    @Override
    public Void accept(Jle jle) {
        System.out.println("    " + jle);

        super.accept(jle);

        return null;
    }

    @Override
    public Void accept(Jne jne) {

        System.out.println("    " + jne);

        super.accept(jne);

        return null;
    }

    @Override
    public Void accept(Jeq je) {

        System.out.println("    " + je);

        super.accept(je);

        return null;
    }

    @Override
    public Void accept(Jlt jle) {

        System.out.println("    " + jle);

        super.accept(jle);

        return null;
    }

    @Override
    public Void accept(Jgt jgt) {

        System.out.println("    " + jgt);

        super.accept(jgt);

        return null;
    }

    @Override
    public Void accept(Load load) {

        System.out.println("    " + load);

        return null;
    }

    @Override
    public Void accept(Move move) {

        System.out.println("    " + move);

        return null;
    }

    @Override
    public Void accept(Store store) {

        System.out.println("    " + store);

        return null;
    }

    @Override
    public Void accept(Ret sub) {

        System.out.println("    " + sub);

        return null;
    }

    @Override
    public Void accept(Neg neg) {
        System.out.println("    " + neg);

        return null;
    }

    @Override
    public Void accept(Not not) {
        System.out.println("    " + not);

        return null;
    }

    @Override
    public Void accept(Bin bin) {

        System.out.println("    " + bin);

        return null;

    }

    @Override
    public Void accept(Call call) {

        System.out.println("    " + call);

        return null;
    }

    protected void visit(IRBlock target) {

        if(target != null)
            System.out.println(target + ": ");

        super.visit(target);
    }
}
