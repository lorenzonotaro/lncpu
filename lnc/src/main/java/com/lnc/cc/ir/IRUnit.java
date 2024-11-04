package com.lnc.cc.ir;

import com.lnc.cc.ast.FunctionDeclaration;
import com.lnc.cc.common.FlatSymbolTable;
import com.lnc.cc.common.Scope;
import com.lnc.cc.common.Symbol;

public class IRUnit {
    private final FunctionDeclaration functionDeclaration;

    private final IRBlock startBlock;

    private IRBlock currentBlock = null;

    private final FlatSymbolTable symbolTable;

    private int blockCounter = 0;

    private int currentLoopLevel = 0;

    private final VirtualRegisterManager vrManager;

    public IRUnit(FunctionDeclaration functionDeclaration) {
        this.startBlock = currentBlock = new IRBlock(this, blockCounter++);
        this.functionDeclaration = functionDeclaration;
        this.symbolTable = FlatSymbolTable.flatten(functionDeclaration.getScope());
        this.functionDeclaration.unit = this;
        vrManager = new VirtualRegisterManager();
    }

    public FlatSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public Symbol resolveSymbol(Scope scope, String symbolName) {
        return symbolTable.resolveSymbol(scope, symbolName);
    }

    VirtualRegisterManager getVrManager() {
        return vrManager;
    }

    public void emit(IRInstruction instruction) {
        if (currentBlock == null) {
            currentBlock = startBlock;
        }

        instruction.setLoopNestedLevel(currentLoopLevel);

        currentBlock.emit(instruction);
    }

    public IRBlock newBlock() {
        return new IRBlock(this, blockCounter++);
    }


    public void setCurrentBlock(IRBlock block){
        currentBlock = block;
    }

    public void enterLoop(){
        currentLoopLevel++;
    }

    public void exitLoop(){
        currentLoopLevel--;
    }


    public IRBlock getCurrentBlock() {
        return currentBlock;
    }

    public IRBlock getStartBlock() {
        return startBlock;
    }

    public FunctionDeclaration getFunctionDeclaration() {
        return functionDeclaration;
    }

    public VirtualRegisterManager getVRManager() {
        return vrManager;
    }
}
