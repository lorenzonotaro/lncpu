package com.lnc.cc.anaylsis;


import com.lnc.cc.ast.FunctionDeclaration;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final FunctionDeclaration context;
    private final Scope parent;

    private final Map<String, Symbol> symbols = new HashMap<>();

    public Scope(FunctionDeclaration context, Scope parent){
        this.context = context;
        this.parent = parent;
    }

    public Scope(FunctionDeclaration context){
        this.context = context;
        this.parent = null;
    }

    public Scope getRoot(){
        if(parent == null){
            return this;
        }
        return parent.getRoot();
    }

    public boolean isRoot(){
        return parent == null;
    }

    public Scope getParent(){
        return parent;
    }

    public void define(Symbol symbol){
        Symbol existing = symbols.get(symbol.getName());
        if(existing != null && !existing.isForward() && !symbol.isForward()){
            throw new RuntimeException("symbol '%s' already defined here: '%s'".formatted(existing.getName(), existing.getToken().formatLocation()));
        }
        symbols.put(symbol.getName(), symbol);
    }

    public Symbol resolve(String name){
        Symbol symbol = symbols.get(name);
        if(symbol != null){
            return symbol;
        }
        if(parent != null){
            return parent.resolve(name);
        }
        return null;
    }

    public FunctionDeclaration getContext() {
        return context;
    }
}
