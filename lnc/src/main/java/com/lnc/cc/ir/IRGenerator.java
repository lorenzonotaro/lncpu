package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
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

        branchIfTrue(whileStatement.condition, body, next, true);

        currentUnit.setCurrentBlock(body);

        visitStatement(whileStatement.body);

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

        visitStatement(doStmt.body);

        currentUnit.continueTo(header);
        branchIfTrue(doStmt.condition, body, exit, true);
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
            visitStatement(forStmt.initializer);
        }

        // 2) Create the four conceptual blocks:
        //    header: test the condition
        //    body:   execute the loop body
        //    incr:   do the increment step
        //    exit:   continuation after the loop
        IRBlock header = currentUnit.newBlock();
        IRBlock body   = currentUnit.newBlock();
        IRBlock exit   = currentUnit.newBlock();

        // 3) Link pre-header → header, then switch into header
        currentUnit.continueTo(header);

        // 4) Emit the test (fall into body if true, else to exit)
        if (forStmt.condition != null) {
            // branchIfFalse(cond, falseTarget, trueTarget)
            branchIfTrue(forStmt.condition, body, exit, true);
        } else {
            // no condition means “always true”
            emit(new Goto(body));
        }

        // 5) Enter the loop and emit the body
        currentUnit.enterLoop(new LoopInfo(header, exit));
        currentUnit.setCurrentBlock(body);

        visitStatement(forStmt.body);


        // after the body, unconditionally go to the incr block
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
            branchIfTrue(ifStmt.condition, thenBlk, falseTarget, false);
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


    private void branchIfTrue(Expression condExpr,
                               IRBlock takenIfTrue, IRBlock takenIfFalse, boolean loopTest) {
        IROperand left, right;
        CondJump.Cond originalCond;

        if (condExpr instanceof BinaryExpression be) {
            left  = be.left .accept(this);
            right = be.right.accept(this);
            originalCond = CondJump.Cond.of(be.operator, be.token);
        } else {
            left         = condExpr.accept(this);
            right        = new ImmediateOperand((byte)0, new I8Type());
            originalCond = CondJump.Cond.NE;  // “cond != 0”
        }

        // Emit with the original relation, but swap targets
        emit(new CondJump(
                originalCond,
                left,
                right,
                /* true→ */ takenIfTrue,
                /* false→*/ takenIfFalse,
                loopTest));
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

        if(statement instanceof FunctionDeclaration functionDeclaration && !functionDeclaration.isForwardDeclaration()){
            currentUnit = new IRUnit(functionDeclaration);
            blocks.add(currentUnit);
        }

        super.visitStatement(statement);
    }

    @Override
    public IROperand visit(AssignmentExpression assignmentExpression) {

        var value = assignmentExpression.right.accept(this);

        var dest = assignmentExpression.left.accept(this);

        if(!assignmentExpression.isInitializer() && dest.type == IROperand.Type.LOCATION && ((Location) dest).getSymbol().getTypeQualifier().isConst()){
            throw new CompileException("assignment to constant variable", assignmentExpression.left.token);
        }

        emit(new Move(value, dest));

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

        // 2) generate IR for the callee
        IROperand callee = callExpression.callee.accept(this);

        List<IROperand> args = Arrays.stream(callExpression.arguments)
                .map(a -> a.accept(this))
                .toList();

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

        if(symbol.getTypeSpecifier().type == TypeSpecifier.Type.STRUCT){
            StructType structType = (StructType) symbol.getTypeSpecifier();
            return structType.getDefinition();
        }else if (symbol.getTypeSpecifier().type == TypeSpecifier.Type.POINTER){
            PointerType pointerType = (PointerType) symbol.getTypeSpecifier();
            if(pointerType.getBaseType().type == TypeSpecifier.Type.STRUCT){
                StructType structType = (StructType) pointerType.getBaseType();
                return structType.getDefinition();
            }
        }

        throw new CompileException("member access on non-struct type", operatorToken);
    }

    @Override
    public IROperand visit(NumericalExpression numericalExpression) {

        int value = numericalExpression.value;

        return new ImmediateOperand(
                value,
                numericalExpression.typeSpecifier
        );
    }

    @Override
    public IROperand visit(StringExpression stringExpression) {
        return new Location(resolveConstant(
                stringExpression.token.lexeme));
    }

    private BaseSymbol resolveConstant(String lexeme) {
        return globalSymbolTable.resolveConstant(lexeme);
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

        IROperand returnVal = null;


        if((unaryExpression.operator == UnaryExpression.Operator.INCREMENT ||
            unaryExpression.operator == UnaryExpression.Operator.DECREMENT) && unaryExpression.unaryPosition == UnaryExpression.UnaryPosition.POST){
            VirtualRegister target = allocVR(operand.getTypeSpecifier());
            // load into a temporary VR
            VirtualRegister vr = moveOrLoadIntoVR(operand);

            emit(new Unary(target, operand, unaryExpression.operator));

            returnVal = vr;
        }else if(unaryExpression.operator == UnaryExpression.Operator.NOT ||
                  unaryExpression.operator == UnaryExpression.Operator.NEGATE ||
                unaryExpression.operator == UnaryExpression.Operator.INCREMENT ||
                unaryExpression.operator == UnaryExpression.Operator.DECREMENT){
            VirtualRegister target = allocVR(operand.getTypeSpecifier());
            emit(new Unary(target, operand, unaryExpression.operator));
            returnVal = target;
        }else if(unaryExpression.operator == UnaryExpression.Operator.DEREFERENCE) {
            if(operand.getTypeSpecifier().type != TypeSpecifier.Type.POINTER){
                throw new CompileException("dereference of non-pointer type", unaryExpression.token);
            }

            returnVal = new Deref(operand);
        }else if(unaryExpression.operator == UnaryExpression.Operator.ADDRESS_OF){

            if(operand.type != IROperand.Type.LOCATION){
                throw new CompileException("requested address of non-memory operand", unaryExpression.token);
            }

            return (Location) operand;
        }

        return returnVal;
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

    private VirtualRegister moveOrLoadIntoVR(IROperand operand) {
        return moveOrLoadIntoVR(operand, RegisterClass.ANY);
    }

    private VirtualRegister moveOrLoadIntoVR(IROperand operand, RegisterClass registerClass) {
        if(operand.type == IROperand.Type.VIRTUAL_REGISTER) {
            VirtualRegister vr = (VirtualRegister) operand;
            if(registerClass == RegisterClass.ANY || vr.getRegisterClass() == registerClass) {
                return vr; // Already in the correct register class
            } else {
                return moveToVr(vr, registerClass); // Move to the correct register class
            }
        } else if(operand.type == IROperand.Type.IMMEDIATE) {
            VirtualRegisterManager vrm = currentUnit.getVrManager();
            VirtualRegister vr = vrm.getRegister(operand.getTypeSpecifier());
            vr.setRegisterClass(registerClass);
            emit(new Move(operand, vr));
            return vr;
        } else {
            return moveToVr(operand, registerClass);
        }
    }

    private VirtualRegister moveToVr(IROperand operand, RegisterClass registerClass) {
        VirtualRegister vr = allocVR(operand.getTypeSpecifier());
        vr.setRegisterClass(registerClass);
        emit(new Move(operand, vr));
        return vr;
    }

}
