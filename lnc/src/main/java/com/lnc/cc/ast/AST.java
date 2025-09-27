package com.lnc.cc.ast;


import com.lnc.cc.common.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Abstract Syntax Tree (AST) for a program. This class serves
 * as the main container for declarations and maintains a global scope.
 */
public class AST {

    private final Scope globalScope = Scope.createRoot("");

    private final List<Declaration> declarations = new ArrayList<>();

    public void addDeclaration(Declaration declaration){
        declarations.add(declaration);
    }

    public Scope getGlobalScope() {
        return globalScope;
    }

    public List<Declaration> getDeclarations() {
        return declarations;
    }
}
