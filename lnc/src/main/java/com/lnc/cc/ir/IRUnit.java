package com.lnc.cc.ir;

import com.lnc.cc.ast.FunctionDeclaration;
import com.lnc.cc.common.FlatSymbolTable;
import com.lnc.cc.common.Scope;
import com.lnc.cc.common.BaseSymbol;

import java.util.*;

public class IRUnit implements Iterable<IRBlock>{
    private final FunctionDeclaration functionDeclaration;

    private final IRBlock startBlock;

    private IRBlock currentBlock = null;

    private final FlatSymbolTable symbolTable;

    private int blockCounter = 0;

    private final VirtualRegisterManager vrManager;

    private final Stack<LoopInfo> loopStack;

    public IRUnit(FunctionDeclaration functionDeclaration) {
        this.startBlock = currentBlock = new IRBlock(this, blockCounter++);
        this.functionDeclaration = functionDeclaration;
        this.symbolTable = FlatSymbolTable.flatten(functionDeclaration.getScope());
        this.functionDeclaration.unit = this;
        this.loopStack = new Stack<>();
        vrManager = new VirtualRegisterManager();
    }

    public FlatSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public BaseSymbol resolveSymbol(Scope scope, String symbolName) {
        return symbolTable.resolveSymbol(scope, symbolName);
    }

    VirtualRegisterManager getVrManager() {
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
        setCurrentBlock(currentBlock);
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

    public VirtualRegisterManager getVRManager() {
        return vrManager;
    }

    @Override
    public Iterator<IRBlock> iterator() {
        return new Iterator<IRBlock>() {
            private final Set<IRBlock> visited = new HashSet<>();
            private final Deque<IRBlock> stack = new ArrayDeque<>();
            private IRBlock nextBlock;

            {
                stack.push(getEntryBlock());
                advance();
            }

            private void advance() {
                nextBlock = null;
                while (!stack.isEmpty()) {
                    IRBlock b = stack.pop();
                    if (visited.add(b)) {
                        // push successors in *reverse* order so we visit
                        // them in natural order when popping
                        List<IRBlock> succs = b.getSuccessors();
                        for (int i = succs.size() - 1; i >= 0; i--) {
                            stack.push(succs.get(i));
                        }
                        nextBlock = b;
                        return;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                return nextBlock != null;
            }

            @Override
            public IRBlock next() {
                if (nextBlock == null) throw new NoSuchElementException();
                IRBlock result = nextBlock;
                advance();
                return result;
            }
        };
    }

    public void computeSuccessorsAndPredecessors() {

    }
}
