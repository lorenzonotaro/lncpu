package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.common.*;
import com.lnc.cc.ast.*;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.types.*;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

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
    public Void accept(WhileStatement whileStatement) {

        IRBlock header = currentUnit.newBlock();
        IRBlock body = currentUnit.newBlock();
        IRBlock next = currentUnit.newBlock();

        currentUnit.getCurrentBlock().addSuccessor(header);
        currentUnit.setCurrentBlock(header);

        currentUnit.enterLoop(new LoopInfo(header, next));

        branchIfFalse(whileStatement.condition, body, next);

        header.addSuccessor(body);
        header.addSuccessor(next);

        currentUnit.setCurrentBlock(body);
        whileStatement.body.accept(this);
        emit(new Goto(header));

        currentUnit.exitLoop();

        currentUnit.setCurrentBlock(next);

        return null;
    }

    @Override
    public Void accept(DoWhileStatement doStmt) {

        IRBlock body   = currentUnit.newBlock();
        IRBlock header = currentUnit.newBlock();  // test
        IRBlock exit   = currentUnit.newBlock();

        // 1) fall-through into body
        currentUnit.getCurrentBlock().addSuccessor(body);
        currentUnit.setCurrentBlock(body);
        currentUnit.enterLoop(new LoopInfo(header, exit));

        // 2) after body, jump to header to test
        emit(new Goto(header));
        body.addSuccessor(header);

        // 3) test block
        currentUnit.setCurrentBlock(header);
        // branch *if false* to exit  (i.e. if !cond, drop out)
        header.addSuccessor(exit);
        header.addSuccessor(body);
        branchIfFalse(doStmt.condition, /* false→ */ exit, /* true→ */ body);

        currentUnit.exitLoop();

        // 4) continue in exit
        currentUnit.setCurrentBlock(exit);
        return null;
    }

    @Override
    public Void accept(ContinueStatement continueStatement) {
        LoopInfo loopInfo = currentUnit.getCurrentLoopInfo();

        if(loopInfo == null){
            throw new CompileException("continue statement outside of loop", continueStatement.token);
        }

        emit(new Goto(loopInfo.continueTarget()));

        return null;
    }

    @Override
    public Void accept(BreakStatement breakStatement) {
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
    public Void accept(ForStatement forStmt) {
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
        currentUnit.getCurrentBlock().addSuccessor(header);
        currentUnit.setCurrentBlock(header);

        // 4) Emit the test (fall into body if true, else to exit)
        if (forStmt.condition != null) {
            // branchIfFalse(cond, falseTarget, trueTarget)
            branchIfFalse(forStmt.condition, exit, body);
        } else {
            // no condition means “always true”
            emit(new Goto(body));
        }
        header.addSuccessor(body);
        header.addSuccessor(exit);

        // 5) Enter the loop and emit the body
        currentUnit.enterLoop(new LoopInfo(header, exit));
        currentUnit.setCurrentBlock(body);
        forStmt.body.accept(this);
        // after the body, unconditionally go to the incr block
        emit(new Goto(incr));
        body.addSuccessor(incr);

        // 6) Emit the increment step
        currentUnit.setCurrentBlock(incr);
        if (forStmt.increment != null) {
            forStmt.increment.accept(this);
        }
        // then loop back to the header for the next test
        emit(new Goto(header));
        incr.addSuccessor(header);

        // 7) Finish up
        currentUnit.exitLoop();
        currentUnit.setCurrentBlock(exit);
        // any following code will chain off of 'exit'
        return null;
    }

    @Override
    public Void accept(ReturnStatement returnStatement) {
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
    public Void accept(IfStatement ifStmt) {
        // 1) Create the four conceptual blocks:
        IRBlock header   = currentUnit.newBlock();                     // test
        IRBlock thenBlk  = currentUnit.newBlock();                     // “then” branch
        IRBlock elseBlk  = ifStmt.elseStatement != null
                ? currentUnit.newBlock()                   // optional “else”
                : null;
        IRBlock exitBlk  = currentUnit.newBlock();                     // join/continuation

        // 2) Link fall-through into header, then switch into it
        currentUnit.getCurrentBlock().addSuccessor(header);
        currentUnit.setCurrentBlock(header);

        // 3) Emit the abstract conditional branch
        //    branchIfFalse(cond, falseTarget, trueTarget)
        if (ifStmt.condition != null) {
            IRBlock falseTarget = elseBlk != null ? elseBlk : exitBlk;
            branchIfFalse(ifStmt.condition, falseTarget, thenBlk);
        } else {
            // “if (true)” → always go to thenBlk
            emit(new Goto(thenBlk));
        }
        header.addSuccessor(thenBlk);
        header.addSuccessor(elseBlk != null ? elseBlk : exitBlk);

        // 4) Build the “then” block
        currentUnit.setCurrentBlock(thenBlk);
        visitStatement(ifStmt.thenStatement);
        // after then, unconditionally jump to exit
        emit(new Goto(exitBlk));
        thenBlk.addSuccessor(exitBlk);

        // 5) Optionally build the “else” block
        if (elseBlk != null) {
            currentUnit.setCurrentBlock(elseBlk);
            visitStatement(ifStmt.elseStatement);
            emit(new Goto(exitBlk));
            elseBlk.addSuccessor(exitBlk);
        }

        // 6) Continue in the exit block
        currentUnit.setCurrentBlock(exitBlk);
        return null;
    }


    private void branchIfFalse(Expression condExpr,
                               IRBlock taken, IRBlock notTaken) {
        IROperand left, right;
        CondJump.Cond cond;

        if (condExpr instanceof BinaryExpression be) {
            left  = be.left .accept(this);
            right = be.right.accept(this);
            cond   = switch (be.operator) {
                case EQ -> CondJump.Cond.EQ;
                case NE -> CondJump.Cond.NE;
                case LT -> CondJump.Cond.LT;
                case LE -> CondJump.Cond.LE;
                case GT -> CondJump.Cond.GT;
                case GE -> CondJump.Cond.GE;
                default -> throw new CompileException("unsupported comparison", be.token);
            };
        } else {
            left  = condExpr.accept(this);
            right = new ImmediateOperand((byte)0);
            cond   = CondJump.Cond.NE;
        }
        emit(new CondJump(cond, left, right, /* true→ */ taken, /* false→ */ notTaken));
    }

    @Override
    public Void accept(FunctionDeclaration functionDeclaration) {
        return super.accept(functionDeclaration);
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
    public IROperand accept(AssignmentExpression assignmentExpression) {

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
    public IROperand accept(BinaryExpression binaryExpression) {

        IROperand left = binaryExpression.left.accept(this);
        IROperand right = binaryExpression.right.accept(this);
        emit(new Bin(left, right, binaryExpression.operator));

        return left;
    }

    @Override
    public IROperand accept(CallExpression callExpression) {

        List<IROperand> args = Arrays.stream(callExpression.arguments)
                .map(a -> a.accept(this))
                .toList();

        // 2) generate IR for the callee
        IROperand callee = callExpression.callee.accept(this);

        // 3) allocate a destination VR *only* if returnType != VOID
        VirtualRegister dest = callExpression.getTypeSpecifier().type != TypeSpecifier.Type.VOID
                ? allocVR()
                : null;

        // 4) emit the abstract Call node
        emit(new Call(dest, callee, args.toArray(new IROperand[0])));

        return dest;
    }

    @Override
    public IROperand accept(IdentifierExpression identifierExpression) {
        BaseSymbol symbol = resolveSymbol(identifierExpression.token);
        return new Location(symbol);
    }

    @Override
    public IROperand accept(MemberAccessExpression memberAccessExpression) {
        IROperand left = memberAccessExpression.left.accept(this);

        StructDefinitionType definition = getStructDefinitionType(left, memberAccessExpression.token);

        if(definition == null){
            throw new CompileException("struct type not defined", memberAccessExpression.token);
        }

        return new Location(new StructAccessSymbol(((Location)left).getSymbol(), memberAccessExpression.right.lexeme));
    }

    private static StructDefinitionType getStructDefinitionType(IROperand left, Token operatorToken) {
        if(left.type != IROperand.Type.LOCATION){
            throw new CompileException("invalid type for member access", operatorToken);
        }

        AbstractSymbol symbol = ((Location) left).getSymbol();

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
    public IROperand accept(NumericalExpression numericalExpression) {

        if(IntUtils.inByteRange(numericalExpression.value)){
            return new ImmediateOperand((byte) (numericalExpression.value & 0xFF));
        }

        throw new CompileException("value out of range: " + numericalExpression.value, numericalExpression.token);
    }

    @Override
    public IROperand accept(StringExpression stringExpression) {
        throw new CompileException("string expressions not supported", stringExpression.token);
    }

    @Override
    public IROperand accept(SubscriptExpression subscriptExpression) {

        IROperand array = subscriptExpression.left.accept(this);

        if(array.type == IROperand.Type.LOCATION) {
            Location loc = (Location) array;

            TypeSpecifier baseType;

            if (loc.getSymbol().getType() instanceof AbstractSubscriptableType subscriptableType){
                baseType = subscriptableType.getBaseType();
            } else{
                throw new CompileException("subscript on non-array type", subscriptExpression.token);
            }

            IROperand index = subscriptExpression.index.accept(this);

            if(index.type == IROperand.Type.IMMEDIATE){
                return new Location(new ArrayAccessSymbol(loc.getSymbol(), ((ImmediateOperand) index).getValue()));
            }else if (index.type == IROperand.Type.LOCATION) {
                VirtualRegister indexReg;
                var vr = allocVR();
                vr.setRegisterClass(RegisterClass.INDEX);
                emit(new Load(vr, (ReferenceableIROperand) index));

                // for now, repeatedly add the size to the index
                int size = baseType.allocSize();
                for (int i = 1; i < size; ++i) {
                    emit(new Bin(vr, vr, BinaryExpression.Operator.ADD));
                }

                indexReg = vr;

                return new RegisterDereference(indexReg, baseType, 0);
            } else if(index.type == IROperand.Type.VIRTUAL_REGISTER){
                VirtualRegister indexReg = (VirtualRegister) index;

                // for now, repeatedly add the size to the index
                int size = baseType.allocSize();
                for (int i = 1; i < size; ++i) {
                    emit(new Bin(indexReg, indexReg, BinaryExpression.Operator.ADD));
                }

                emit(new Bin(indexReg, new AddressOf(loc.getSymbol()), BinaryExpression.Operator.ADD));

                return new RegisterDereference(indexReg, baseType, 0);

            } else {
                throw new CompileException("invalid type for subscript index", subscriptExpression.token);
            }
        }else if(array.type == IROperand.Type.REGISTER_DEREFERENCE){
            RegisterDereference dereference = (RegisterDereference) array;

            TypeSpecifier baseType;

            if(dereference.dereferencedType instanceof AbstractSubscriptableType subscriptableType){
                baseType = subscriptableType.getBaseType();
            }else{
                throw new CompileException("subscript on non-array type", subscriptExpression.token);
            }

            IROperand index = subscriptExpression.index.accept(this);

            if(index.type == IROperand.Type.IMMEDIATE){
                dereference.addToOffset(((ImmediateOperand) index).getValue());
                return dereference;
            }else {
                if (index.type == IROperand.Type.LOCATION) {


                    Location locIndex = (Location) index;

                    if(baseType.allocSize() == 1){
                        emit(new Bin(dereference.getReg(), index, BinaryExpression.Operator.ADD));
                    }else{
                        VirtualRegister indexReg;
                        var tempVr = allocVR();
                        emit(new Load(tempVr, locIndex));

                        // for now, repeatedly add the size to the index
                        int size = baseType.allocSize();
                        for (int i = 1; i < size; ++i) {
                            emit(new Bin(dereference.getReg(), tempVr, BinaryExpression.Operator.ADD));
                        }


                        return dereference;
                    }

                } else if (index.type == IROperand.Type.VIRTUAL_REGISTER) {

                    for (int i = 1; i < baseType.allocSize(); ++i) {
                        emit(new Bin(dereference.getReg(), index, BinaryExpression.Operator.ADD));
                    }
                }

                return dereference;
            }
        }else{
            throw new CompileException("invalid type for subscript", subscriptExpression.token);
        }
    }

    @Override
    public IROperand accept(UnaryExpression unaryExpression) {
        IROperand operand = unaryExpression.operand.accept(this);


        switch (unaryExpression.operator){
            case NOT -> {
                emit(new Not(operand));
                return operand;
            }
            case NEGATE -> {
                emit(new Neg(operand));
                return operand;
            }
            case DEREFERENCE -> throw new Error("dereference IR not implemented");
            case ADDRESS_OF -> throw new Error("address of IR not implemented");
            case INCREMENT -> {
                if(unaryExpression.associativity == UnaryExpression.Associativity.LEFT){
                    emit(new Inc(operand));
                    return operand;
                }else{
                    var vr = allocVR();
                    emit(new Move(vr, operand));
                    emit(new Inc(operand));
                    return vr;
                }
            }
            case DECREMENT -> {
                if(unaryExpression.associativity == UnaryExpression.Associativity.LEFT){
                    emit(new Dec(operand));
                    return operand;
                }else{
                    var vr = allocVR();
                    emit(new Move(operand, vr));
                    emit(new Dec(operand));
                    return vr;
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + unaryExpression.operator);
        }
    }

    private VirtualRegister allocVR() {
        return currentUnit.getVrManager().getRegister();
    }

    private void emit(IRInstruction instruction){
        currentUnit.emit(instruction);
    }

    public IR getResult() {
        return new IR(blocks, globalSymbolTable);
    }
}
