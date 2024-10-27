package com.lnc.cc.ir;

import com.lnc.cc.ast.FunctionDeclaration;
import com.lnc.cc.common.FlatSymbolTable;
import com.lnc.cc.common.Scope;
import com.lnc.cc.common.Symbol;
import com.lnc.common.frontend.Token;

import java.util.LinkedList;
import java.util.List;

public class IRBlock {
    private final FunctionDeclaration functionDeclaration;

    private final List<IRInstruction> instructions = new LinkedList<>();

    private final FlatSymbolTable symbolTable;

    private int virtualRegisterCounter = 0;

    public IRBlock(FunctionDeclaration functionDeclaration) {
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
        instructions.add(instruction);
    }
}
