package com.lnc.cc.ir;

import com.lnc.cc.optimization.IRPass;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.Logger;

public class IRAnalysisPass extends IRPass {
    @Override
    public Void visit(Goto aGoto) {
        return null;
    }

    @Override
    public Void visit(CondJump condJump) {
        return null;
    }

    @Override
    public Void visit(Move move) {
        return null;
    }

    @Override
    public Void visit(Ret ret) {
        return null;
    }

    @Override
    public Void visit(Bin bin) {
        return null;
    }

    @Override
    public Void visit(Call call) {
        return null;
    }

    @Override
    public Void visit(Push push) {
        return null;
    }

    @Override
    public Void accept(LoadParam loadParam) {
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        return null;
    }

    @Override
    protected void visit(IRBlock block) {
        if(block.getSuccessors().isEmpty() && !(block.last instanceof Ret)){
            // If the block has no successors and is not a return block, we add a return instruction
            // to ensure that the control flow is well-defined.
            if (getUnit().getFunctionType().returnType.type != TypeSpecifier.Type.VOID) {
                Logger.warning(String.format("no return statement in function '%s' returning non-void", getUnit().getFunctionDeclaration().name.lexeme));
            }
            block.emit(new Ret(null));
            markAsChanged();
        }
        super.visit(block);
    }
}
