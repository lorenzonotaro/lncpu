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

    private final VirtualRegisterManager virtualRegisterManager;

    public IRUnit(FunctionDeclaration functionDeclaration) {
        this.startBlock = currentBlock = new IRBlock(this, blockCounter++);
        this.functionDeclaration = functionDeclaration;
        this.symbolTable = FlatSymbolTable.flatten(functionDeclaration.getScope());
        virtualRegisterManager = new VirtualRegisterManager();
    }

    public FlatSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public Symbol resolveSymbol(Scope scope, String symbolName) {
        return symbolTable.resolveSymbol(scope, symbolName);
    }

    VirtualRegisterManager getVirtualRegisterManager() {
        return virtualRegisterManager;
    }

    public void emit(IRInstruction instruction) {
        if (currentBlock == null) {
            currentBlock = startBlock;
        }

        currentBlock.emit(instruction);
    }

    public IRBlock newBlock() {
        return new IRBlock(this, blockCounter++);
    }


    public void setCurrentBlock(IRBlock block){
        currentBlock = block;
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
}
