package com.lnc.cc.ir;

import com.lnc.cc.ast.FunctionDeclaration;
import com.lnc.cc.common.FlatSymbolTable;
import com.lnc.cc.common.Scope;
import com.lnc.cc.common.Symbol;

import java.util.LinkedList;
import java.util.List;

public class IRUnit {
    private final FunctionDeclaration functionDeclaration;

    private final IRBlock startBlock;

    private IRBlock currentBlock = null;

    private final FlatSymbolTable symbolTable;

    private int virtualRegisterCounter = 0, blockCounter = 0;

    public IRUnit(FunctionDeclaration functionDeclaration) {
        this.startBlock = currentBlock = new IRBlock(this, blockCounter++);
        this.functionDeclaration = functionDeclaration;
        this.symbolTable = FlatSymbolTable.flatten(functionDeclaration.getScope());
    }

    public FlatSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public Symbol resolveSymbol(Scope scope, String symbolName) {
        return symbolTable.resolveSymbol(scope, symbolName);
    }

    public VirtualRegister createVirtualRegister() {
        return new VirtualRegister(virtualRegisterCounter++);
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
