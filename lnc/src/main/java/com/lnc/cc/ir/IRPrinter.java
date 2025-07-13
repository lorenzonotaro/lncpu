package com.lnc.cc.ir;

public class IRPrinter extends GraphicalIRVisitor {

    public IRPrinter() {
        super(TraversalOrder.REVERSE_POST_ORDER_ONLY);
    }

    private final StringBuilder sb = new StringBuilder();
    
    @Override
    public Void visit(Goto aGoto) {
        appendInstr(aGoto);
        return null;
    }

    @Override
    public Void visit(CondJump condJump) {
        appendInstr(condJump);

        return null;
    }

    @Override
    public Void visit(Load load) {
        appendInstr(load);
        return null;
    }

    @Override
    public Void visit(Move move) {
        appendInstr(move);
        return null;
    }

    @Override
    public Void visit(Store store) {
        appendInstr(store);
        return null;
    }

    @Override
    public Void visit(Ret ret) {
        appendInstr(ret);
        return null;
    }

    @Override
    public Void visit(Bin bin) {
        appendInstr(bin);
        return null;
    }

    @Override
    public Void visit(Call call) {
        appendInstr(call);
        return null;
    }

    @Override
    public Void visit(Push push) {
        appendInstr(push);
        return null;
    }

    @Override
    public Void accept(LoadParam loadParam) {
        appendInstr(loadParam);
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        appendInstr(unary);
        return null;
    }

    @Override
    protected void visit(IRBlock block) {
        sb.append("\n_l").append(block.getId()).append(":\n");

        super.visit(block);
    }

    @Override
    public void visit(IRUnit unit) {
        sb.append("==== Function: ").append(unit.getFunctionDeclaration().name.lexeme).append(" ====");
        super.visit(unit);
        sb.append("\n");
    }

    public void appendInstr(IRInstruction instr) {
        sb.append("\t").append(instr.toString()).append("\n");
    }

    public String getResult() {
        return sb.toString();
    }
}
