package com.lnc.cc.ir;

import com.lnc.assembler.common.IEncodeable;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.cc.ast.FunctionDeclaration;
import com.lnc.cc.codegen.Register;
import com.lnc.cc.common.FlatSymbolTable;
import com.lnc.cc.common.Scope;
import com.lnc.cc.common.BaseSymbol;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.types.FunctionType;

import java.util.*;

public class IRUnit implements Iterable<IRBlock>{
    private final FunctionDeclaration functionDeclaration;
    private final FunctionType functionType;

    private IRBlock startBlock;

    private IRBlock currentBlock = null;

    private final FlatSymbolTable symbolTable;

    private int blockCounter = 0;

    private final VirtualRegisterManager vrManager;

    private final Stack<LoopInfo> loopStack;
    private int spillSpaceSize;
    private Set<Register> usedRegisters;

    private FrameInfo frameInfo;

    private ParameterOperandMapping parameterOperandMapping;

    public IRUnit(FunctionDeclaration functionDeclaration) {
        this.startBlock = currentBlock = new IRBlock(this, blockCounter++);
        this.functionDeclaration = functionDeclaration;
        this.symbolTable = FlatSymbolTable.flatten(functionDeclaration.getScope());
        this.functionDeclaration.unit = this;
        this.loopStack = new Stack<>();
        this.functionType = FunctionType.of(functionDeclaration);
        vrManager = new VirtualRegisterManager();
    }

    public FunctionType getFunctionType() {
        return functionType;
    }

    public FlatSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public BaseSymbol resolveSymbol(Scope scope, String symbolName) {
        return symbolTable.resolveSymbol(scope, symbolName);
    }

    public VirtualRegisterManager getVrManager() {
        return vrManager;
    }

    public void emit(IRInstruction instruction) {
        if (currentBlock == null) {
            currentBlock = startBlock;
        }

        instruction.setLoopNestedLevel(loopStack.size());

        currentBlock.emit(instruction);
    }

    public IRBlock newBlock() {
        return new IRBlock(this, blockCounter++);
    }


    public void setCurrentBlock(IRBlock block){
        currentBlock = block;
    }

    public void continueTo(IRBlock block){
        if (currentBlock == null){
            throw new IllegalStateException("Current block is null, cannot continue to another block.");
        }
        if (currentBlock == block) {
            throw new IllegalStateException("Cannot continue to the same block: " + block);
        }

        if (block == null) {
            throw new IllegalArgumentException("Cannot continue to a null block.");
        }

        currentBlock.emit(new Goto(block));
        setCurrentBlock(block);
    }

    public void enterLoop(LoopInfo info){
        loopStack.push(info);
    }

    public LoopInfo exitLoop(){
        return loopStack.pop();
    }

    public LoopInfo getCurrentLoopInfo(){
        return loopStack.isEmpty() ? null : loopStack.peek();
    }


    public IRBlock getCurrentBlock() {
        return currentBlock;
    }

    public IRBlock getEntryBlock() {
        return startBlock;
    }

    public FunctionDeclaration getFunctionDeclaration() {
        return functionDeclaration;
    }

    @Override
    public Iterator<IRBlock> iterator() {
        return computeReversePostOrderAndCFG().iterator();
    }

    @Override
    public String toString(){
        IRPrinter printer = new IRPrinter();
        printer.visit(this);
        return printer.getResult();
    }

    public void setEntryBlock(IRBlock newBlock) {
        if (newBlock == null) {
            throw new IllegalArgumentException("New entry block cannot be null.");
        }
        if (startBlock == newBlock) {
            throw new IllegalArgumentException("New entry block cannot be the same as the current entry block.");
        }

        // Update the entry block reference
        startBlock = newBlock;
    }

    public List<IRBlock> computeReversePostOrderAndCFG() {
        List<IRBlock> postOrder = new ArrayList<>();
        Set<IRBlock> visited = new HashSet<>();
        dfsCFG(startBlock, visited, postOrder);

        postOrder.forEach(b -> {
            b.getSuccessors().forEach(s -> s.getPredecessors().add(b));
        });

        Collections.reverse(postOrder); // RPO = reversed post-order
        return postOrder;
    }


    private void dfsCFG(
            IRBlock block,
            Set<IRBlock> visited,
            List<IRBlock> postOrder
    ) {
        if (!visited.add(block)) return;

        block.getSuccessors().clear();
        block.getPredecessors().clear();

        for (IRBlock succ : block.getLastInstructionTargets()) {
            block.getSuccessors().add(succ);
            dfsCFG(succ, visited, postOrder);
        }

        postOrder.add(block);
    }

    public VirtualRegisterManager getVirtualRegisterManager() {
        return vrManager;
    }

    public void setSpillSpaceSize(int spillSpaceSize) {
        this.spillSpaceSize = spillSpaceSize;
    }

    public void setUsedRegisters(Set<Register> usedRegisters) {
        this.usedRegisters = usedRegisters;
    }

    public int getSpillSpaceSize() {
        return spillSpaceSize;
    }

    public Set<Register> getUsedRegisters() {
        return usedRegisters;
    }

    public int getTotalStackFrameSize() {
        return spillSpaceSize /* + localsSize */;
    }

    public void setParameterOperandMapping(ParameterOperandMapping parameterMapping) {
        this.parameterOperandMapping = parameterMapping;
    }

    public ParameterOperandMapping getParameterOperandMapping() {
        return parameterOperandMapping;
    }

    public record FrameInfo(
            int localsSize,
            int spillSpaceSize,
            Map<String, Integer> localOffsets) {

        public int allocSize() {
            return localsSize + spillSpaceSize;
        }
    }

    public FrameInfo getFrameInfo() {

        return frameInfo;
    }

    public void compileFrameInfo() {
        Map<String, Integer> localOffsets = new HashMap<>();

        int currentOffset = spillSpaceSize;
        for (BaseSymbol symbol : symbolTable.getSymbols().values()) {
            if (symbol.isForward() || symbol.isParameter()) {
                continue; // Skip forward declarations and parameters
            }

            localOffsets.put(symbol.getName(), currentOffset);

            currentOffset += symbol.getType().allocSize();
        }

        this.frameInfo = new FrameInfo(
                currentOffset, // localsSize
                spillSpaceSize, // spillSpaceSize
                localOffsets // localOffsets
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IRUnit irUnit)) return false;
        return Objects.equals(functionDeclaration, irUnit.functionDeclaration);
    }
}
