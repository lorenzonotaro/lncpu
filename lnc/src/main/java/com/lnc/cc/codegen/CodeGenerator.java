package com.lnc.cc.codegen;

import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.argument.*;
import com.lnc.assembler.parser.argument.Byte;
import com.lnc.cc.ast.BinaryExpression;
import com.lnc.cc.ast.UnaryExpression;
import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.types.StorageLocation;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.IntUtils;
import com.lnc.common.Logger;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.*;

/**
 * The CodeGenerator class is responsible for generating low-level assembly-like code
 * from an intermediate representation (IR). It extends the GraphicalIRVisitor and implements
 * the IIROperandVisitor interface to traverse and process different IR constructs.
 *
 * This class handles tasks such as:
 * - Emitting the data and constant sections.
 * - Visiting and processing IR units.
 * - Allocating registers using Graph Coloring for optimal code generation.
 * - Optimizing post-register allocation through PostRAOptimizer.
 * - Adding support for specific IR operations, such as Goto, Conditional Jumps, Binary and Unary
 *   operations, and more.
 *
 * The generated code is grouped into CompilerOutput objects, which collectively represent
 * the generated output for the given IR.
 *
 * Key features:
 * - Handles different targets (e.g., Function Units, Data, and Constant sections).
 * - Emits optimized instructions for conditional and unconditional jumps.
 * - Supports register allocation and post-allocation optimization.
 * - Utilizes a LIFO-based approach for target scheduling.
 */
public class CodeGenerator extends GraphicalIRVisitor implements IIROperandVisitor<Argument>{
    private final IR ir;
    private final SoftwareExtensionsManager softwareExtensionsManager = new SoftwareExtensionsManager();

    private CompilerOutput currentOutput;

    private final List<CompilerOutput> outputs = new ArrayList<>();

    public CodeGenerator(IR ir){
        this.ir = ir;
    }

    public boolean run(){

        outputDataSections();

        outputConstSection();

        for(IRUnit unit : ir.units()){

            if(unit.getFunctionDeclaration().isForwardDeclaration()){
                // Skip forward declarations
                continue;
            }

            currentOutput = new CompilerOutput(unit, new SectionInfo("LNC_" + unit.getFunctionDeclaration().name.lexeme, -1, LinkTarget.ROM, LinkMode.PAGE_FIT, false, false, false));

            GraphColoringRegisterAllocator.AllocationInfo allocationInfo;
            try {
                allocationInfo = GraphColoringRegisterAllocator.run(unit);
            }catch (RuntimeException e){
                Logger.error("Register allocation failed: " + e.getMessage());
                return false;
            }
            PostRAOptimizer.run(unit, allocationInfo);

            unit.compileFrameInfo();

            visit(unit);

            outputs.add(currentOutput);

            super.reset();
        }

        var extensionOutput = softwareExtensionsManager.emitOutput();
        if (extensionOutput != null) {
            outputs.add(extensionOutput);
        }

        return true;
    }

    private void outputConstSection() {
        this.currentOutput = new CompilerOutput(null, new SectionInfo("LNCCONST", -1, LinkTarget.ROM, LinkMode.PAGE_FIT, false, false, false));

        for(var constSymbol : ir.symbolTable().getConstants().values()){
            label(constSymbol.getAsmName());
            currentOutput.append(constSymbol.getValue());
        }

        outputs.add(currentOutput);
    }

    private void outputDataSections() {

        var dataPage = new CompilerOutput(null, new SectionInfo("LNCDPAGE", 0x2000, LinkTarget.RAM, LinkMode.FIXED, false, true, false));
        var farData = new CompilerOutput(null, new SectionInfo("LNCDATA", -1, LinkTarget.RAM, LinkMode.PAGE_FIT, false, false, false));

        for(var entry : ir.symbolTable().getSymbols().values()){
            var type = entry.getTypeSpecifier();
            var storageQualifier = entry.getStorageQualifier();
            if(type.type != TypeSpecifier.Type.FUNCTION && (entry.getScope().isRoot() || entry.getStorageQualifier().isStatic()) && !entry.getStorageQualifier().isExtern()){
                this.currentOutput = storageQualifier.storageLocation() == StorageLocation.FAR ? farData : dataPage;
                variable(entry.getName(), type.allocSize());
                if(entry.getStorageQualifier().isExport()){
                    this.currentOutput.exportLabel(entry.getName());
                }
            }
        }

        outputs.add(dataPage);
        outputs.add(farData);
    }

    @Override
    public Void visit(Goto aGoto) {
        var target = CodeGenUtils.labelRef(aGoto.getTarget());

        instrf(TokenType.GOTO, target);

        return null;
    }

    @Override
    public Void visit(CondJump condJump) {

        var left = condJump.getLeft().accept(this);
        var right = condJump.getRight().accept(this);

        IRBlock trueTarget = condJump.getTarget();
        IRBlock falseTarget = condJump.getFalseTarget();
        IRBlock continueTo = condJump.getContinueTo();

        // Emit minimal conditional jumps and schedule targets using LIFO (push in reverse order)
        switch(condJump.getCond()){
            case EQ -> {
                // Z == 1 => true; fallthrough to false
                instrf(TokenType.JZ, CodeGenUtils.labelRef(trueTarget));

                enqueue(trueTarget);   // push first
                enqueue(falseTarget);  // so false is visited next (fallthrough)

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(falseTarget));
            }
            case NE -> {
                // complement via JZ to false; fallthrough to true
                instrf(TokenType.JZ, CodeGenUtils.labelRef(falseTarget));
                enqueue(falseTarget);
                enqueue(trueTarget);

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(trueTarget));
            }
            case LT -> {
                // C == 1 => true; fallthrough to false
                instrf(TokenType.JC, CodeGenUtils.labelRef(trueTarget));
                enqueue(trueTarget);
                enqueue(falseTarget);

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(falseTarget));
            }
            case GE -> {
                // complement via JC to false; fallthrough to true
                instrf(TokenType.JC, CodeGenUtils.labelRef(falseTarget));
                enqueue(falseTarget);
                enqueue(trueTarget);

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(trueTarget));
            }
            case LE -> {
                // C == 1 or Z == 1 => true; fallthrough to false
                instrf(TokenType.JC, CodeGenUtils.labelRef(trueTarget));
                instrf(TokenType.JZ, CodeGenUtils.labelRef(trueTarget));
                enqueue(trueTarget);
                enqueue(falseTarget);

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(falseTarget));
            }
            case GT -> {
                // complement via (JC || JZ) to false; fallthrough to true
                instrf(TokenType.JC, CodeGenUtils.labelRef(falseTarget));
                instrf(TokenType.JZ, CodeGenUtils.labelRef(falseTarget));
                enqueue(falseTarget);
                enqueue(trueTarget);

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(trueTarget));
            }
        }

        if(continueTo != null){
            enqueueLast(continueTo);
        }

        return null;
    }


    @Override
    public Void visit(Move move) {

        int destSize = move.getDest().getTypeSpecifier().allocSize();
        int srcSize = move.getSource().getTypeSpecifier().allocSize();

        var target = move.getDest().accept(this);
        var source = move.getSource().accept(this);

        if(destSize != srcSize){
            throw new IllegalArgumentException("Mov argument size mismatch: " +
                    destSize + " vs " +
                    srcSize);
        }

        if(destSize == 1){
            instrf(TokenType.MOV, source, target);
        }else{
            var splitSource = CodeGenUtils.splitWord(source);
            var splitTarget = CodeGenUtils.splitWord(target);

            instrf(TokenType.MOV, splitSource[0], splitTarget[0]);
            instrf(TokenType.MOV, splitSource[1], splitTarget[1]);
        }


        return null;
    }

    @Override
    public Void visit(Ret ret) {
        instrf(TokenType.GOTO, new LabelRef(Token.__internal(TokenType.IDENTIFIER, "_ret")));
        return null;
    }

    @Override
    public Void visit(Bin bin) {

        if (emitSoftwareWordBin(bin)) {
            return null;
        }

        var dest = bin.getDest().accept(this);
        var left = bin.getLeft().accept(this);
        var right = bin.getRight().accept(this);

        var destStr = dest.toString();
        var leftStr = left.toString();
        var rightStr = right.toString();

        BinaryExpression.Operator operator = bin.getOperator();

        boolean isShift = operator == BinaryExpression.Operator.SHL || operator == BinaryExpression.Operator.SHR;
        if(destStr.equals(leftStr)){
            emitBinOp(operator, dest, right);
        }else if(operator.isCommutative() && destStr.equals(rightStr)){
            // If the operator is commutative, we can swap left and right
            emitBinOp(operator, dest, left);
        }else if(isShift){
            // Shifts are emitted in-place on the value being shifted because the target
            // instruction form expects that operand/register layout; move the result to
            // dest afterwards when dest is different from left.
            emitBinOp(operator, left, right);
            instrf(TokenType.MOV, left, dest);
        } else{
            instrf(TokenType.MOV, left, dest);
            emitBinOp(operator, dest, right);
        }

        return null;
    }

    private boolean emitSoftwareWordBin(Bin bin) {
        BinaryExpression.Operator op = bin.getOperator();
        if (op != BinaryExpression.Operator.ADD && op != BinaryExpression.Operator.SUB) {
            return false;
        }

        Register destWord = asWordVirtualRegister(bin.getDest());
        if (destWord == null) {
            return false;
        }

        IROperand rhs = bin.getRight();
        if (!bin.getDest().equals(bin.getLeft())) {
            if (op.isCommutative() && bin.getDest().equals(bin.getRight())) {
                rhs = bin.getLeft();
            } else {
                emitWordMove(bin.getLeft(), destWord);
            }
        }

        Register byteSrc = asByteVirtualRegister(rhs);
        String symbol;
        if (byteSrc != null) {
            symbol = op == BinaryExpression.Operator.ADD
                    ? softwareExtensionsManager.requireAddWordByte(destWord, byteSrc)
                    : softwareExtensionsManager.requireSubWordByte(destWord, byteSrc);
        } else {
            Register wordSrc = asWordVirtualRegister(rhs);
            if (wordSrc == null) {
                throw new UnsupportedOperationException("Word " + op + " requires register RHS after lowering: " + rhs);
            }

            symbol = op == BinaryExpression.Operator.ADD
                    ? softwareExtensionsManager.requireAddWordWord(destWord, wordSrc)
                    : softwareExtensionsManager.requireSubWordWord(destWord, wordSrc);
        }

        instrf(TokenType.LCALL, CodeGenUtils.labelRef(symbol));
        return true;
    }

    private void emitWordMove(IROperand source, Register dstWord) {
        Register srcWord = asWordVirtualRegister(source);
        if (srcWord != null) {
            instrf(TokenType.MOV, CodeGenUtils.reg(srcWord.getComponents()[0]), CodeGenUtils.reg(dstWord.getComponents()[0]));
            instrf(TokenType.MOV, CodeGenUtils.reg(srcWord.getComponents()[1]), CodeGenUtils.reg(dstWord.getComponents()[1]));
            return;
        }

        Argument sourceArg = source.accept(this);
        var splitSource = CodeGenUtils.splitWord(sourceArg);
        instrf(TokenType.MOV, splitSource[0], CodeGenUtils.reg(dstWord.getComponents()[0]));
        instrf(TokenType.MOV, splitSource[1], CodeGenUtils.reg(dstWord.getComponents()[1]));
    }

    private void emitBinOp(BinaryExpression.Operator op, Argument target, Argument right) {
        if (emitSoftwareBinOp(op, target, right)) {
            return;
        }

        switch(op){
            case ADD -> {
                instrf(TokenType.ADD, target, right);
            }
            case SUB -> {
                instrf(TokenType.SUB, target, right);
            }
            case MUL -> {
                throw new UnsupportedOperationException("MUL operator is not supported in this code generator.");
            }
            case DIV -> {
                throw new UnsupportedOperationException("DIV operator is not supported in this code generator.");
            }
            case AND -> {
                instrf(TokenType.AND, target, right);
            }
            case OR -> {
                instrf(TokenType.OR, target, right);
            }
            case XOR -> {
                instrf(TokenType.XOR, target, right);
            }
            case SHL -> {
                if (!(right instanceof Byte shiftAmount)) {
                    throw new IllegalArgumentException("Shift operations require an immediate byte argument; this should be guaranteed by IR generation.");
                }
                for (int i = 0; i < shiftAmount.value; i++) {
                    instrf(TokenType.SHL, target);
                }
            }
            case SHR -> {
                if (!(right instanceof Byte shiftAmount)) {
                    throw new IllegalArgumentException("Shift operations require an immediate byte argument; this should be guaranteed by IR generation.");
                }
                for (int i = 0; i < shiftAmount.value; i++) {
                    instrf(TokenType.SHR, target);
                }
            }
            default -> {
                throw new UnsupportedOperationException("Unsupported binary operator: " + op);
            }
        }
    }

    private boolean emitSoftwareBinOp(BinaryExpression.Operator op, Argument target, Argument right) {
        Register targetWord = asWordRegister(target);
        if (targetWord == null) {
            return false;
        }

        String extensionSymbol;

        switch (op) {
            case ADD -> {
                Register byteSrc = asByteRegister(right);
                if (byteSrc != null) {
                    extensionSymbol = softwareExtensionsManager.requireAddWordByte(targetWord, byteSrc);
                } else {
                    Register wordSrc = asWordRegister(right);
                    if (wordSrc == null) {
                        throw new UnsupportedOperationException("Word ADD requires a byte or word register source: " + right);
                    }
                    extensionSymbol = softwareExtensionsManager.requireAddWordWord(targetWord, wordSrc);
                }
            }
            case SUB -> {
                Register byteSrc = asByteRegister(right);
                if (byteSrc != null) {
                    extensionSymbol = softwareExtensionsManager.requireSubWordByte(targetWord, byteSrc);
                } else {
                    Register wordSrc = asWordRegister(right);
                    if (wordSrc == null) {
                        throw new UnsupportedOperationException("Word SUB requires a byte or word register source: " + right);
                    }
                    extensionSymbol = softwareExtensionsManager.requireSubWordWord(targetWord, wordSrc);
                }
            }
            default -> {
                return false;
            }
        }

        instrf(TokenType.LCALL, CodeGenUtils.labelRef(extensionSymbol));
        return true;
    }

    @Override
    public Void visit(Call call) {
        instrf(TokenType.LCALL, call.getCallee().accept(this));
        return null;
    }

    @Override
    public Void visit(Push push) {
        instrf(TokenType.PUSH, push.getArg().accept(this));
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        if (unary.getOperator() == UnaryExpression.Operator.INCREMENT || unary.getOperator() == UnaryExpression.Operator.DECREMENT) {
            Register targetWord = asWordVirtualRegister(unary.getTarget());
            if (targetWord != null) {
                String extensionSymbol = unary.getOperator() == UnaryExpression.Operator.INCREMENT
                        ? softwareExtensionsManager.requireIncWord(targetWord)
                        : softwareExtensionsManager.requireDecWord(targetWord);
                instrf(TokenType.LCALL, CodeGenUtils.labelRef(extensionSymbol));
                return null;
            }
        }

        var target = unary.getTarget().accept(this);

        switch(unary.getOperator()){
            case NEGATE -> {
                // for negate, we can negate the value and add 1
                instrf(TokenType.NOT, target);
                instrf(TokenType.INC, target);
            }
            case NOT -> {
                instrf(TokenType.NOT, target);
            }
            case INCREMENT -> {
                instrf(TokenType.INC, target);
            }
            case DECREMENT -> {
                instrf(TokenType.DEC, target);
            }
            default -> {
                // The IR lowering pass should have resolved the other unary operators
                throw new UnsupportedOperationException("Unary operator is unsupported by this code generator: " + unary.getOperator());
            }
        }

        return null;
    }

    @Override
    public Argument visit(ImmediateOperand immediateOperand) {
        int value = immediateOperand.getValue();
        return IntUtils.inByteRange(value) ? CodeGenUtils.immByte(value) : CodeGenUtils.immWord(value);
    }

    @Override
    public Argument visit(VirtualRegister vr) {
        return visitRegister(vr);
    }

    private Argument visitRegister(VirtualRegister vr) {
        Register assignedPhysicalRegister = vr.getAssignedPhysicalRegister();
        if(assignedPhysicalRegister.isCompound()){
            Register[] components = assignedPhysicalRegister.getComponents();
            var high = components[0];
            var low = components[1];
            return new Composite(CodeGenUtils.reg(high), CodeGenUtils.reg(low), false);
        }else{
            return CodeGenUtils.reg(assignedPhysicalRegister);
        }
    }

    private Register asByteVirtualRegister(IROperand operand) {
        if (!(operand instanceof VirtualRegister vr)) {
            return null;
        }

        Register reg = vr.getAssignedPhysicalRegister();
        if (reg != null && !reg.isCompound()) {
            return reg;
        }

        return null;
    }

    private Register asWordVirtualRegister(IROperand operand) {
        if (operand instanceof VirtualRegister vr) {
            Register reg = vr.getAssignedPhysicalRegister();
            if (reg != null && reg.isCompound()) {
                return reg;
            }
        }
        return null;
    }

    private Register asByteRegister(Argument argument) {
        if (argument instanceof com.lnc.assembler.parser.argument.Register registerArgument) {
            Register reg = Register.valueOf(registerArgument.reg.name());
            return reg.isCompound() ? null : reg;
        }
        return null;
    }

    private Register asWordRegister(Argument argument) {
        if (!(argument instanceof Composite composite)) {
            return null;
        }

        if (!(composite.high instanceof com.lnc.assembler.parser.argument.Register highArg) ||
                !(composite.low instanceof com.lnc.assembler.parser.argument.Register lowArg)) {
            return null;
        }

        Register high = Register.valueOf(highArg.reg.name());
        Register low = Register.valueOf(lowArg.reg.name());

        for (Register candidate : Register.values()) {
            if (!candidate.isCompound()) {
                continue;
            }

            Register[] components = candidate.getComponents();
            if (components[0] == high && components[1] == low) {
                return candidate;
            }
        }

        return null;
    }

    @Override
    public Argument visit(Location location) {
        return switch (location.locType) {
            case SYMBOL -> CodeGenUtils.labelRef(((StaticSymbolLocation) location).getSymbol().getAsmName());
            case STACK_FRAME -> toStackFrameAddress((StackFrameLocation) location, true);
            case STATIC_DERIVED -> toStaticAddress((StaticDerivedLocation) location, true);
            case DEREF -> {
                var deref = (DerefLocation) location;
                yield CodeGenUtils.deref(deref.getTarget().accept(this));
            }
            default -> throw new IllegalStateException("Unsupported non-lowered location type in code generation: " + location.locType);
        };
    }

    @Override
    public Argument visit(AddressOf addressOf) {
        IROperand operand = addressOf.getOperand();

        if (!(operand instanceof Location location)) {
            throw new IllegalStateException("AddressOf operand is not a location: " + operand);
        }

        return switch (location.locType) {
            case SYMBOL -> CodeGenUtils.labelRef(((StaticSymbolLocation) location).getSymbol().getAsmName());
            case STACK_FRAME -> toStackFrameAddress((StackFrameLocation) location, false);
            case STATIC_DERIVED -> toStaticAddress((StaticDerivedLocation) location, false);
            case DEREF -> ((DerefLocation) location).getTarget().accept(this);
            default -> throw new IllegalStateException("Unsupported address-of target in code generation: " + location.locType);
        };
    }

    private Argument toStackFrameAddress(StackFrameLocation location, boolean dereference) {
        TokenType operator = location.getOperandType() == StackFrameLocation.OperandType.PARAMETER
                ? TokenType.MINUS
                : TokenType.PLUS;

        Argument addr = new RegisterOffset(
                CodeGenUtils.reg(TokenType.BP),
                Token.__internal(operator, operator == TokenType.PLUS ? "+" : "-"),
                (NumericalArgument) CodeGenUtils.immByte(location.getOffset())
        );

        return dereference ? CodeGenUtils.deref(addr) : addr;
    }

    private Argument toStaticAddress(StaticDerivedLocation location, boolean dereference) {
        Argument baseAddress = staticLocationAddress(location.getBase());
        int offset = location.getOffset();

        Argument addr = offset == 0
                ? baseAddress
                : CodeGenUtils.bin(baseAddress, CodeGenUtils.immByte(offset), TokenType.PLUS);

        return dereference ? CodeGenUtils.deref(addr) : addr;
    }

    private Argument staticLocationAddress(StaticLocation location) {
        if (location instanceof StaticSymbolLocation symbolLocation) {
            return CodeGenUtils.labelRef(symbolLocation.getSymbol().getAsmName());
        }

        if (location instanceof StaticDerivedLocation derivedLocation) {
            Argument baseAddress = staticLocationAddress(derivedLocation.getBase());
            int offset = derivedLocation.getOffset();
            return offset == 0
                    ? baseAddress
                    : CodeGenUtils.bin(baseAddress, CodeGenUtils.immByte(offset), TokenType.PLUS);
        }

        throw new IllegalStateException("Unsupported static location kind: " + location.getClass().getSimpleName());
    }

    @Override
    protected void visit(IRBlock block) {
        label(block.toString());
        super.visit(block);
    }

    @Override
    public void visit(IRUnit unit) {

        if(unit.getFunctionDeclaration().declarator.storageQualifier().isExport()){
            currentOutput.exportLabel(unit.getFunctionDeclaration().name.lexeme);
        }

        // visit the function body
        super.visit(unit);

        label("_ret");
        instrf(TokenType.RET);
    }

    private void variable(String asmName, int size) {
        label(asmName);
        currentOutput.append(EncodedData.of(new byte[size]));
    }

    private void label(String lexeme) {
        currentOutput.addLabel(lexeme);
    }

    private void instrf(TokenType opcode, Argument... args) {
        currentOutput.append(CodeGenUtils.instr(opcode, args));
    }

    public List<CompilerOutput> getOutputs() {
        return outputs;
    }
}
