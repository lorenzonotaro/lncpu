package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.AST;
import com.lnc.cc.ast.FunctionDeclaration;
import com.lnc.cc.common.FlatSymbolTable;

/**
 * The Analyzer class is responsible for analyzing an Abstract Syntax Tree (AST).
 * It performs local resolution of symbols and type checking to validate the AST structure.
 */
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
