package com.lnc.cc.ir;

import com.lnc.cc.ast.UnaryExpression;
import com.lnc.cc.ast.BinaryExpression;
import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.types.FunctionType;
import com.lnc.cc.types.PointerType;
import com.lnc.cc.types.StorageLocation;
import com.lnc.cc.types.TypeSpecifier;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that performs the lowering pass on an intermediate representation (IR) of code.
 * This class transforms high-level IR operations into lower-level constructs, often preparing
 * the code for assembly generation or subsequent optimizations. Operations are altered to ensure
 * compatibility with the lncpu's execution environment's constraints such as register usage, calling
 * conventions, and instruction limitations.
 *
 * This class extends {@link GraphicalIRVisitor} to traverse and modify the control flow graph
 * represented by the IR and implements {@link IIROperandVisitor} to support operand-level
 * transformations.
 *
 * Responsibilities:
 * - Adjust operand types, ensuring proper usage of virtual registers and immediate values.
 * - Transform conditional jumps, binary/unary operations, function calls, and return statements.
 * - Handle the movement of operands into appropriate virtual registers or memory.
 * - Conform to the calling convention by managing function arguments and return targets.
 * - Insert auxiliary instructions such as move and push as needed to adhere to constraints.
 *
 * Features and Capabilities:
 * - Utilizes register classes (e.g., general-purpose, return-specific) to organize operand placement.
 * - Ensures stack-based argument passing where required by the calling convention.
 * - Resolves operand locations for variables, constants, and other entities in the IR.
 * - Supports extensibility for unimplemented operations (e.g., struct member or array access).
 *
 */
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
            emitBefore(new Move(operand, vr));
            return vr;
        } else {
            return move(operand, registerClass);
        }
    }

    private IROperand move(IROperand operand, RegisterClass registerClass) {
        VirtualRegisterManager vrm = getUnit().getVrManager();
        VirtualRegister vr = vrm.getRegister(operand.getTypeSpecifier());
        vr.setRegisterClass(registerClass);
        emitBefore(new Move(operand, vr));
        return vr;
    }

    @Override
    public Void visit(Move move) {
        move.setSource(move.getSource().accept(this));
        move.setDest(move.getDest().accept(this));
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

        boolean requiresRegisterRhs = (bin.getOperator() == BinaryExpression.Operator.ADD || bin.getOperator() == BinaryExpression.Operator.SUB)
                && bin.getDest().getTypeSpecifier().allocSize() > 1;

        if(right.type != IROperand.Type.VIRTUAL_REGISTER && (requiresRegisterRhs || right.type != IROperand.Type.IMMEDIATE)) {
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
            VirtualRegister userTarget = call.getReturnTarget(); //
            // the register that will be used for the rest of the function
            VirtualRegister constrainedTarget = getUnit().getVrManager().getRegister(userTarget.getTypeSpecifier());
            constrainedTarget.setRegisterClass(retRC);

            call.setReturnTarget(constrainedTarget);
            call.insertAfter(new Move(constrainedTarget, userTarget));
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
    public Void visit(Unary unary) {
        IROperand operand = unary.getOperand().accept(this);
        IROperand target = unary.getTarget().accept(this);

        unary.setOperand(moveOrLoadIntoVR(operand));
        unary.setTarget(target);

        if(unary.getOperator() == UnaryExpression.Operator.INCREMENT || unary.getOperator() == UnaryExpression.Operator.DECREMENT && operand.type != IROperand.Type.VIRTUAL_REGISTER) {
            emitAfter(new Move(target, operand));
        }

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
        return switch (location.locType) {
            case SYMBOL -> {
                var symbolLoc = (StaticSymbolLocation) location;
                var resolvedLocal = getUnit().getLocalMappingInfo().mappings().get(symbolLoc.getSymbol().getName());
                if (resolvedLocal == null || resolvedLocal == location) {
                    yield location;
                }
                yield resolvedLocal.accept(this);
            }
            case STACK_FRAME, STATIC_DERIVED -> location;
            case DEREF -> {
                var deref = (DerefLocation) location;
                var loweredTarget = deref.getTarget().accept(this);
                if(loweredTarget instanceof AddressOf loweredAddr) {
                    // the dereference of an address-of is simply the address-of target
                    yield loweredAddr.getOperand();
                }
                moveOrLoadIntoVR(loweredTarget, loweredTarget.getTypeSpecifier().allocSize() > 1 ? RegisterClass.FAR_DEREF : RegisterClass.NEAR_DEREF);
                yield new DerefLocation(loweredTarget);
            }
            case ARRAY_INDEX -> lowerArrayIndex((ArrayIndexLocation) location);
            case STRUCT_MEMBER -> lowerStructMember((StructMemberAccess) location);
        };
    }

    @Override
    public IROperand visit(AddressOf addressOf) {
        IROperand loweredOperand = addressOf.getOperand().accept(this);
        if (!(loweredOperand instanceof Location loweredLoc)) {
            throw new IllegalStateException("address-of target is not a location after lowering: " + loweredOperand);
        }else if(loweredOperand instanceof DerefLocation deref){
            // the address of a dereference is simply the dereference target
            return deref.getTarget();
        }
        return new AddressOf(loweredLoc);
    }

    private IROperand lowerStructMember(StructMemberAccess memberAccess) {
        IROperand loweredBase = memberAccess.getBase().accept(this);
        if (!(loweredBase instanceof Location baseLoc)) {
            throw new IllegalStateException("struct member base is not a location after lowering: " + loweredBase);
        }

        int offset = memberAccess.getByteOffset();

        if (baseLoc instanceof StaticLocation staticLoc) {
            return new StaticDerivedLocation(staticLoc, offset);
        }

        IROperand baseAddress = new AddressOf(baseLoc);
        IROperand pointerWithOffset = addConstantOffset(baseAddress, offset);
        return new DerefLocation(pointerWithOffset, memberAccess.getTypeSpecifier());
    }

    private IROperand lowerArrayIndex(ArrayIndexLocation arrayLoc) {
        IROperand loweredBase = arrayLoc.getBase().accept(this);
        if (!(loweredBase instanceof Location baseLoc)) {
            throw new IllegalStateException("array index base is not a location after lowering: " + loweredBase);
        }

        IROperand loweredIndex = arrayLoc.getIndex().accept(this);

        if (loweredIndex instanceof ImmediateOperand imm && baseLoc instanceof StaticLocation staticBase) {
            int byteOffset = imm.getValue() * arrayLoc.getStride();
            return new StaticDerivedLocation(staticBase, byteOffset);
        }

        IROperand baseAddress = (baseLoc instanceof DerefLocation deref) ? deref.getTarget() : new AddressOf(baseLoc);
        IROperand byteOffset = scaleIndex(loweredIndex, arrayLoc.getStride());
        IROperand indexedAddress = addOffset(baseAddress, byteOffset, baseLoc.getPointerKind());

        return new DerefLocation(indexedAddress);
    }

    private IROperand scaleIndex(IROperand index, int stride) {
        if (stride == 1) {
            return index;
        }

        if (index instanceof ImmediateOperand imm) {
            return new ImmediateOperand(imm.getValue() * stride, imm.getTypeSpecifier());
        }

        IROperand indexVr = moveOrLoadIntoVR(index);
        VirtualRegister acc = getUnit().getVrManager().getRegister(indexVr.getTypeSpecifier());
        acc.setRegisterClass(RegisterClass.ANY);
        emitBefore(new Move(new ImmediateOperand(0, indexVr.getTypeSpecifier()), acc));

        for (int i = 0; i < stride; i++) {
            emitBefore(new Bin(acc, acc, indexVr, BinaryExpression.Operator.ADD));
        }

        return acc;
    }

    private IROperand addOffset(IROperand baseAddress, IROperand byteOffset, StorageLocation pointerKind) {
        if (byteOffset instanceof ImmediateOperand imm && imm.getValue() == 0) {
            return baseAddress;
        }

        VirtualRegister pointerVr = getUnit().getVrManager().getRegister(baseAddress.getTypeSpecifier());
        moveOrLoadIntoVR(pointerVr, pointerKind == StorageLocation.FAR ? RegisterClass.FAR_DEREF : RegisterClass.NEAR_DEREF);

        IROperand rhs = byteOffset instanceof ImmediateOperand
                ? byteOffset
                : moveOrLoadIntoVR(byteOffset, RegisterClass.ANY);

        emitBefore(new Bin(pointerVr, pointerVr, rhs, BinaryExpression.Operator.ADD));
        return pointerVr;
    }

    private IROperand addConstantOffset(IROperand baseAddress, int offset) {
        if (offset == 0) {
            return baseAddress;
        }

        VirtualRegister pointerVr = getUnit().getVrManager().getRegister(baseAddress.getTypeSpecifier());
        pointerVr.setRegisterClass(baseAddress.getTypeSpecifier() instanceof PointerType pointerType
                && pointerType.getPointerKind() == StorageLocation.FAR ? RegisterClass.FAR_DEREF : RegisterClass.NEAR_DEREF);
        emitBefore(new Move(baseAddress, pointerVr));
        emitBefore(new Bin(pointerVr, pointerVr, new ImmediateOperand(offset, pointerVr.getTypeSpecifier()), BinaryExpression.Operator.ADD));
        return pointerVr;
    }

    @Override
    public void visit(IRUnit unit) {

        unit.compileLocalMappings();

        List<IRInstruction> instrs = new ArrayList<>();

        for(var entry : unit.getLocalMappingInfo().originalRegParamMappings().entrySet()){
            instrs.add(new Move(entry.getValue(), unit.getLocalMappingInfo().mappings().get(entry.getKey()), true));
        }

        if(!instrs.isEmpty())
            unit.prependEntryBlock(instrs);

        super.visit(unit);
    }

    /**
     * Emits the given instructions before the current instruction, in the order they are provided.
     * For example, if the current instruction is C, then emitBefore(A, B) will result in
     * A, B, C
     * */
    public void emitBefore(IRInstruction... instrs){
        getCurrentInstruction().insertBefore(List.of(instrs));
    }

    /**
     * Emits the given instructions after the current instruction, in the order they are provided.
     * For example, if the current instruction is C, then emitAfter(A, B) will result in
     * C, A, B
     * */
    public void emitAfter(IRInstruction... instrs){
        getCurrentInstruction().insertAfter(List.of(instrs));
    }
}
