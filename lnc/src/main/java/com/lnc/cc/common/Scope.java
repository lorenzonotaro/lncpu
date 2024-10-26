package com.lnc.cc.common;


import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final Scope parent;

    private final Map<String, Symbol> symbols = new HashMap<>();

    public Scope(Scope parent){
        this.parent = parent;
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
}
