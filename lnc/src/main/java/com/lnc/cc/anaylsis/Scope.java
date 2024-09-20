package com.lnc.cc.anaylsis;


public class Scope {
    private final Scope parent;

    public Scope(Scope parent){
        this.parent = parent;
    }

    public Scope(){
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
}
