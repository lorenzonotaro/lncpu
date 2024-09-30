package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.AST;
import com.lnc.cc.ast.Declaration;

public class Analyzer {

    private final AST ast;
    private final LocalResolver localResolver;
    private final TypeResolver typeResolver;

    public Analyzer(AST ast) {
        this.ast = ast;
        localResolver = new LocalResolver(ast);
        typeResolver = new TypeResolver(ast);
    }


    public boolean analize() {
        if(!localResolver.resolveLocals()){
            return false;
        }

        return typeResolver.resolveTypes();
    }
}
