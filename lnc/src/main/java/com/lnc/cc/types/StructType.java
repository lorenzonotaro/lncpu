package com.lnc.cc.types;

public class StructType extends TypeSpecifier {

    private final String name;

    public StructType(String name){
        super(Type.STRUCT);
        this.name = name;
    }

    @Override
    public String toString(){
        return "struct " + name;
    }
}
