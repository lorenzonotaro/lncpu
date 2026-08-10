package com.lnc.cc.ast;

/**
 * Reports whether a loop body contains a {@code continue} that binds to that loop, i.e. one that is
 * not captured by a nested loop. Nested {@code for}/{@code while}/{@code do} statements are not
 * descended into, because their own bodies rebind {@code continue}.
 */
public final class ContinueTargetScanner implements IStatementVisitor<Boolean> {

    private static final ContinueTargetScanner INSTANCE = new ContinueTargetScanner();

    private ContinueTargetScanner() {
    }

    public static boolean bindsContinue(Statement body) {
        return body != null && body.accept(INSTANCE);
    }

    @Override
    public Boolean visit(BlockStatement blockStatement) {
        for (Statement statement : blockStatement.statements) {
            if (bindsContinue(statement)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visit(IfStatement ifStatement) {
        return bindsContinue(ifStatement.thenStatement) || bindsContinue(ifStatement.elseStatement);
    }

    @Override
    public Boolean visit(ContinueStatement continueStatement) {
        return true;
    }

    @Override
    public Boolean visit(ForStatement forStatement) {
        return false;
    }

    @Override
    public Boolean visit(WhileStatement whileStatement) {
        return false;
    }

    @Override
    public Boolean visit(DoWhileStatement doWhileStatement) {
        return false;
    }

    @Override
    public Boolean visit(ExpressionStatement expressionStatement) {
        return false;
    }

    @Override
    public Boolean visit(FunctionDeclaration functionDeclaration) {
        return false;
    }

    @Override
    public Boolean visit(ReturnStatement returnStatement) {
        return false;
    }

    @Override
    public Boolean visit(VariableDeclaration variableDeclaration) {
        return false;
    }

    @Override
    public Boolean visit(BreakStatement breakStatement) {
        return false;
    }

    @Override
    public Boolean visit(StructDeclaration structDeclaration) {
        return false;
    }
}
