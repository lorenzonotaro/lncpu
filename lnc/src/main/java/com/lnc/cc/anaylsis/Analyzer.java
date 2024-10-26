package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.AST;

public class Analyzer {

    private final LocalResolver localResolver;
    private final TypeChecker typeChecker;

    public Analyzer(AST ast) {
        localResolver = new LocalResolver(ast);
        typeChecker = new TypeChecker(ast);
    }


    public boolean analyze() {
        if(!localResolver.visit()){
            return false;
        }

        return typeChecker.visit();
    }
}
