package com.lnc.cc.anaylsis;

import com.lnc.cc.types.Declarator;
import com.lnc.cc.types.TypeSpecifier;

import java.util.HashMap;
import java.util.Map;

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
