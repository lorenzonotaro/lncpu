package com.lnc.cc.ir;

import com.lnc.cc.ast.FunctionDeclaration;
import com.lnc.cc.codegen.Register;
import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.common.FlatSymbolTable;
import com.lnc.cc.common.Scope;
import com.lnc.cc.common.BaseSymbol;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.Location;
import com.lnc.cc.ir.operands.StackFrameOperand;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.types.FunctionType;

import java.util.*;

public class IRUnit implements Iterable<IRBlock>{
    private final FunctionDeclaration functionDeclaration;
    private final FunctionType functionType;

    private IRBlock startBlock;

    private IRBlock currentBlock = null;

    private final FlatSymbolTable symbolTable;

    private int blockCounter = 1;

    private final VirtualRegisterManager vrManager;

    private final ArrayDeque<LoopInfo> loopStack;
    private int spillSpaceSize;
    private Set<Register> usedRegisters;

    private FrameInfo frameInfo;
    private LocalMappingInfo localMappingInfo;

    public IRUnit(FunctionDeclaration functionDeclaration) {
        this.functionDeclaration = functionDeclaration;
        this.symbolTable = FlatSymbolTable.flatten(functionDeclaration.getScope());
        this.functionDeclaration.unit = this;
        this.loopStack = new ArrayDeque<>();
        this.functionType = FunctionType.of(functionDeclaration);
        vrManager = new VirtualRegisterManager();
        this.startBlock = currentBlock = new IRBlock(this, blockCounter++, getCurrentLoopDepth());
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

        currentBlock.emit(instruction);
    }

    public IRBlock newBlock() {
        return new IRBlock(this, blockCounter++, getCurrentLoopDepth());
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

    public int getCurrentLoopDepth() {
        return loopStack.size();
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


    public LocalMappingInfo getLocalMappingInfo() {
        return localMappingInfo ;
    }

    public void prependEntryBlock(List<IRInstruction> list) {
        if(getEntryBlock() == null) {
            throw new IllegalStateException("Entry block is not set, cannot prepend instructions.");
        }

        var prevEntry = getEntryBlock();

        if(prevEntry.getId() == 0){
            throw new IllegalStateException("Entry block already set");
        }

        IRBlock newEntry = new IRBlock(this, 0, getCurrentLoopDepth());

        newEntry.emitAll(list);

        newEntry.emit(new Goto(prevEntry));

        this.setEntryBlock(newEntry);
    }

    public void compileLocalMappings() {
        var mappings = new HashMap<String, IROperand>();
        int forcedStackFrameLocalsSize = 0;

        // parameters
        var parameters = CallingConvention.mapCallArguments(getFunctionDeclaration().parameters);
        for (var parameter : parameters){
            IROperand operand;
            if(parameter.onStack()){
                int offset = parameter.stackOffset();
                operand = new StackFrameOperand(parameter.type(), StackFrameOperand.OperandType.PARAMETER, offset);
            }else{
                VirtualRegister originalReg = vrManager.getRegister(parameter.type());
                originalReg.setRegisterClass(parameter.regClass());
                operand = originalReg;
            }
            mappings.put(parameter.name(), operand);
        }

        // locals
        for(var entry : symbolTable.getSymbols().entrySet()){
            var name = entry.getKey();
            var symbol = entry.getValue();

            if(symbol.getTypeQualifier().isExtern() || symbol.isParameter())
                continue;

            if(symbol.isStatic()){
                mappings.put(symbol.getName(), new Location(symbol));
            }else if(symbol.canResideInRegister()){
                var vr = vrManager.getRegister(symbol.getTypeSpecifier());
                vr.setRegisterClass(symbol.getTypeSpecifier().allocSize() == 1 ? RegisterClass.ANY : RegisterClass.WORD);
                mappings.put(symbol.getName(), vr);
            }else{
                // If the symbol is not a parameter and not static, we create a StackFrameOperand
                int offset = forcedStackFrameLocalsSize;
                IROperand operand = new StackFrameOperand(symbol.getTypeSpecifier(), StackFrameOperand.OperandType.LOCAL, offset);
                mappings.put(symbol.getName(), operand);
                forcedStackFrameLocalsSize += symbol.getTypeSpecifier().allocSize();
            }
        }

        this.localMappingInfo = new LocalMappingInfo(mappings, forcedStackFrameLocalsSize);
    }

    public void compileFrameInfo() {
        int forcedLocalsSize = localMappingInfo.forcedStackFrameLocalsSize;
        int spillSpaceSize = getSpillSpaceSize();

        Map<String, Integer> localOffsets = new HashMap<>();
        for (var entry : localMappingInfo.mappings().entrySet()) {
            if (entry.getValue() instanceof StackFrameOperand stackFrameOperand) {
                localOffsets.put(entry.getKey(), stackFrameOperand.getOffset());
            }
        }

        this.frameInfo = new FrameInfo(forcedLocalsSize, spillSpaceSize, localOffsets);
    }

    public record FrameInfo(
            int forcedLocalsSize,
            int spillSpaceSize,
            Map<String, Integer> localOffsets) {

        public int allocSize() {
            return forcedLocalsSize + spillSpaceSize;
        }
    }

    public FrameInfo getFrameInfo() {
        return frameInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IRUnit irUnit)) return false;
        return Objects.equals(functionDeclaration, irUnit.functionDeclaration);
    }

    public record LocalMappingInfo(HashMap<String, IROperand> mappings, int forcedStackFrameLocalsSize) {
    }
}
