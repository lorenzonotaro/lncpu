package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.AST;

public class Analyzer {

    private final AST ast;
    private final LocalResolver localResolver;
    private final TypeChecker typeChecker;

    public Analyzer(AST ast) {
        this.ast = ast;
        localResolver = new LocalResolver(ast);
        typeChecker = new TypeChecker(ast);
    }


    public boolean analize() {
        if(!localResolver.visit()){
            return false;
        }

        return typeChecker.visit();
    }
}
