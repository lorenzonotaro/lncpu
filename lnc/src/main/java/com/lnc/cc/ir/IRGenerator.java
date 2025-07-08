package com.lnc.cc.ir;

import com.lnc.cc.common.*;
import com.lnc.cc.ast.*;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.types.*;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IRGenerator extends ScopedASTVisitor<IROperand> {


    private final List<IRUnit> blocks = new ArrayList<>();

    private final FlatSymbolTable globalSymbolTable;
    private final Scope globalScope;

    private IRUnit currentUnit;

    public IRGenerator(AST ast) {
        super(ast);
        this.globalScope = ast.getGlobalScope();
        globalSymbolTable = FlatSymbolTable.flatten(ast.getGlobalScope());
    }

    @Override
    public Void visit(WhileStatement whileStatement) {

        IRBlock header = currentUnit.newBlock();
        IRBlock body = currentUnit.newBlock();
        IRBlock next = currentUnit.newBlock();

        currentUnit.continueTo(header);

        currentUnit.enterLoop(new LoopInfo(header, next));

        branch(whileStatement.condition, body, next);

        currentUnit.setCurrentBlock(body);
        whileStatement.body.accept(this);
        emit(new Goto(header));

        currentUnit.exitLoop();

        currentUnit.setCurrentBlock(next);

        return null;
    }

    @Override
    public Void visit(DoWhileStatement doStmt) {

        IRBlock body   = currentUnit.newBlock();
        IRBlock header = currentUnit.newBlock();  // test
        IRBlock exit   = currentUnit.newBlock();

        // 1) fall-through into body
        currentUnit.continueTo(body);
        currentUnit.enterLoop(new LoopInfo(header, exit));

        doStmt.body.accept(this);

        currentUnit.continueTo(header);
        branch(doStmt.condition, body, exit);
        currentUnit.exitLoop();

        // 4) continue in exit
        currentUnit.setCurrentBlock(exit);
        return null;
    }

    @Override
    public Void visit(ContinueStatement continueStatement) {
        LoopInfo loopInfo = currentUnit.getCurrentLoopInfo();

        if(loopInfo == null){
            throw new CompileException("continue statement outside of loop", continueStatement.token);
        }

        emit(new Goto(loopInfo.continueTarget()));

        return null;
    }

    @Override
    public Void visit(BreakStatement breakStatement) {
        LoopInfo loopInfo = currentUnit.getCurrentLoopInfo();

        if(loopInfo == null){
            throw new CompileException("break statement outside of loop", breakStatement.token);
        }

        emit(new Goto(loopInfo.breakTarget()));

        return null;
    }

    @Override
    public Void visit(StructDeclaration structDeclaration) {
        return null;
    }

    @Override
    public Void visit(ForStatement forStmt) {
        // 1) Emit the initializer in the *current* (pre-header) block
        if (forStmt.initializer != null) {
            forStmt.initializer.accept(this);
        }

        // 2) Create the four conceptual blocks:
        //    header: test the condition
        //    body:   execute the loop body
        //    incr:   do the increment step
        //    exit:   continuation after the loop
        IRBlock header = currentUnit.newBlock();
        IRBlock body   = currentUnit.newBlock();
        IRBlock incr   = currentUnit.newBlock();
        IRBlock exit   = currentUnit.newBlock();

        // 3) Link pre-header → header, then switch into header
        currentUnit.continueTo(header);

        // 4) Emit the test (fall into body if true, else to exit)
        if (forStmt.condition != null) {
            // branchIfFalse(cond, falseTarget, trueTarget)
            branch(forStmt.condition, body, exit);
        } else {
            // no condition means “always true”
            emit(new Goto(body));
        }

        // 5) Enter the loop and emit the body
        currentUnit.enterLoop(new LoopInfo(header, exit));
        currentUnit.setCurrentBlock(body);
        forStmt.body.accept(this);

        // after the body, unconditionally go to the incr block
        currentUnit.continueTo(incr);
        if (forStmt.increment != null) {
            forStmt.increment.accept(this);
        }
        // then loop back to the header for the next test
        emit(new Goto(header));

        // 7) Finish up
        currentUnit.exitLoop();
        currentUnit.setCurrentBlock(exit);
        // any following code will chain off of 'exit'
        return null;
    }

    @Override
    public Void visit(ReturnStatement returnStatement) {
        IROperand value = null;

        if(returnStatement.value != null){
            value = returnStatement.value.accept(this);
        }

        if(value == null) {
            emit(new Ret(null));
        }else{
            emit(new Ret(value));
        }

        return null;
    }

    @Override
    public Void visit(IfStatement ifStmt) {
        // 1) Create the four conceptual blocks:
        IRBlock header   = currentUnit.newBlock();                     // test
        IRBlock thenBlk  = currentUnit.newBlock();                     // “then” branch
        IRBlock elseBlk  = ifStmt.elseStatement != null
                ? currentUnit.newBlock()                   // optional “else”
                : null;
        IRBlock exitBlk  = currentUnit.newBlock();                     // join/continuation

        // 2) Link fall-through into header, then switch into it
        currentUnit.continueTo(header);

        // 3) Emit the abstract conditional branch
        //    branchIfFalse(cond, falseTarget, trueTarget)
        if (ifStmt.condition != null) {
            IRBlock falseTarget = elseBlk != null ? elseBlk : exitBlk;
            branch(ifStmt.condition, thenBlk, falseTarget);
        } else {
            // “if (true)” → always go to thenBlk
            emit(new Goto(thenBlk));
        }

        // 4) Build the “then” block
        currentUnit.setCurrentBlock(thenBlk);
        visitStatement(ifStmt.thenStatement);
        // after then, unconditionally jump to exit
        emit(new Goto(exitBlk));

        // 5) Optionally build the “else” block
        if (elseBlk != null) {
            currentUnit.setCurrentBlock(elseBlk);
            visitStatement(ifStmt.elseStatement);
            emit(new Goto(exitBlk));
        }

        // 6) Continue in the exit block
        currentUnit.setCurrentBlock(exitBlk);
        return null;
    }


    private void branch(Expression condExpr,
                               IRBlock takenIfTrue, IRBlock takenIfFalse) {
        IROperand left, right;
        CondJump.Cond originalCond;

        if (condExpr instanceof BinaryExpression be) {
            left  = be.left .accept(this);
            right = be.right.accept(this);
            originalCond = switch (be.operator) {
                case EQ -> CondJump.Cond.EQ;
                case NE -> CondJump.Cond.NE;
                case LT -> CondJump.Cond.LT;
                case LE -> CondJump.Cond.LE;
                case GT -> CondJump.Cond.GT;
                case GE -> CondJump.Cond.GE;
                default -> throw new CompileException("unsupported comparison", be.token);
            };
        } else {
            left         = condExpr.accept(this);
            right        = new ImmediateOperand((byte)0);
            originalCond = CondJump.Cond.NE;  // “cond != 0”
        }

        // Emit with the original relation, but swap targets
        emit(new CondJump(
                originalCond,
                left,
                right,
                /* true→ */ takenIfTrue,
                /* false→*/ takenIfFalse
        ));
    }

    @Override
    public Void visit(FunctionDeclaration functionDeclaration) {
        return super.visit(functionDeclaration);
    }

    @Override
    public BaseSymbol resolveSymbol(Token token) {
        Scope scope = getCurrentScope();

        BaseSymbol symbol;

        if(currentUnit != null){
            symbol = currentUnit.resolveSymbol(scope, token.lexeme);
            if(symbol != null){
                return symbol;
            }

            symbol = globalSymbolTable.resolveSymbol(globalScope, token.lexeme);


            if(symbol != null){
                return symbol;
            }

            throw new CompileException("symbol not found: " + token.lexeme, token);
        }

        symbol = globalSymbolTable.resolveSymbol(scope, token.lexeme);

        if(symbol != null){
            return symbol;
        }

        throw new CompileException("symbol not found: " + token.lexeme, token);
    }

    @Override
    public void visitStatement(Statement statement) {

        if(statement instanceof FunctionDeclaration functionDeclaration){
            currentUnit = new IRUnit(functionDeclaration);
            blocks.add(currentUnit);
        }

        super.visitStatement(statement);
    }

    @Override
    public IROperand visit(AssignmentExpression assignmentExpression) {

        var value = assignmentExpression.right.accept(this);

        var dest = assignmentExpression.left.accept(this);

        if(dest.type == IROperand.Type.LOCATION){
            emit(new Store((Location) dest, value));
        }else{
            emit(new Move(value, dest));
        }

        return dest;
    }

    @Override
    public IROperand visit(BinaryExpression binaryExpression) {

        IROperand left = binaryExpression.left.accept(this);
        IROperand right = binaryExpression.right.accept(this);

        IROperand target = allocVR(left.getTypeSpecifier());

        emit(new Bin(target, left, right, binaryExpression.operator));

        return target;
    }

    @Override
    public IROperand visit(CallExpression callExpression) {

        List<IROperand> args = Arrays.stream(callExpression.arguments)
                .map(a -> a.accept(this))
                .toList();

        // 2) generate IR for the callee
        IROperand callee = callExpression.callee.accept(this);

        TypeSpecifier returnType = null;

        if(callee.getTypeSpecifier() instanceof FunctionType ft){
            returnType = ft.returnType;
        }else{
            throw new CompileException("callee is not a function", callExpression.callee.token);
        }

        // 3) allocate a destination VR *only* if returnType != VOID
        VirtualRegister dest = callExpression.getTypeSpecifier().type != TypeSpecifier.Type.VOID
                ? allocVR(returnType)
                : null;

        // 4) emit the abstract Call node
        emit(new Call(dest, callee, args.toArray(new IROperand[0])));

        return dest;
    }

    @Override
    public IROperand visit(IdentifierExpression identifierExpression) {
        BaseSymbol symbol = resolveSymbol(identifierExpression.token);

        return new Location(symbol);
    }

    @Override
    public IROperand visit(MemberAccessExpression memberAccessExpression) {
        IROperand left = memberAccessExpression.left.accept(this);

        StructDefinitionType definition = getStructDefinitionType(left, memberAccessExpression.token);

        if(definition == null){
            throw new CompileException("struct type not defined", memberAccessExpression.token);
        }

        StructFieldEntry fieldEntry = definition.getField(memberAccessExpression.right.lexeme);

        if (fieldEntry == null) throw new CompileException("no such field", memberAccessExpression.right);

        return new StructMemberAccess(left, fieldEntry);
    }

    private static StructDefinitionType getStructDefinitionType(IROperand left, Token operatorToken) {
        if(left.type != IROperand.Type.LOCATION){
            throw new CompileException("invalid type for member access", operatorToken);
        }

        BaseSymbol symbol = ((Location) left).getSymbol();

        if(symbol.getType().type == TypeSpecifier.Type.STRUCT){
            StructType structType = (StructType) symbol.getType();
            return structType.getDefinition();
        }else if (symbol.getType().type == TypeSpecifier.Type.POINTER){
            PointerType pointerType = (PointerType) symbol.getType();
            if(pointerType.getBaseType().type == TypeSpecifier.Type.STRUCT){
                StructType structType = (StructType) pointerType.getBaseType();
                return structType.getDefinition();
            }
        }

        throw new CompileException("member access on non-struct type", operatorToken);
    }

    @Override
    public IROperand visit(NumericalExpression numericalExpression) {

        if(IntUtils.inByteRange(numericalExpression.value)){
            return new ImmediateOperand((byte) (numericalExpression.value & 0xFF));
        }

        throw new CompileException("value out of range: " + numericalExpression.value, numericalExpression.token);
    }

    @Override
    public IROperand visit(StringExpression stringExpression) {
        throw new CompileException("string expressions not supported", stringExpression.token);
    }

    @Override
    public IROperand visit(SubscriptExpression expr) {
        // 1) lower the “array” and “index” sub-expressions
        IROperand baseOp  = expr.left.accept(this);
        IROperand idxOp   = expr.index.accept(this);

        // 2) check at semantic time that left.type is array or pointer
        TypeSpecifier leftType = expr.left.getTypeSpecifier();
        if (leftType instanceof AbstractSubscriptableType at) {// 3) fetch element type and stride
            TypeSpecifier elemType = at.getBaseType();
            int stride = elemType.allocSize();

            // 4) emit NO loads/stores here—just make the abstract IR node
            return new ArrayElementAccess(baseOp, idxOp, elemType, stride);
        } else {
            throw new CompileException(
                    "Subscript on non-array/pointer type", expr.token);
        }
    }

    @Override
    public IROperand visit(UnaryExpression unaryExpression) {
        IROperand operand = unaryExpression.operand.accept(this);
        IROperand target = allocVR(operand.getTypeSpecifier());

        emit(new Unary(target, operand, unaryExpression.operator, unaryExpression.unaryPosition));

        return target;
    }
    private VirtualRegister allocVR(TypeSpecifier typeSpecifier) {
        return currentUnit.getVrManager().getRegister(typeSpecifier);
    }

    private void emit(IRInstruction instruction){
        currentUnit.emit(instruction);
    }

    public IR getResult() {
        return new IR(blocks, globalSymbolTable);
    }
}
