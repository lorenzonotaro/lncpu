package com.lnc.cc.ast;


import com.lnc.cc.common.Scope;

import java.util.ArrayList;
import java.util.List;

public class AST {

    private final Scope globalScope = new Scope(null);

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
