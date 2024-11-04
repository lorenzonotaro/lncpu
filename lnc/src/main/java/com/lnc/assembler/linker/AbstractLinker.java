package com.lnc.assembler.linker;

import com.lnc.assembler.parser.LnasmParseResult;

public abstract class AbstractLinker<T> {

    private final LinkerConfig config;

    AbstractLinker(LinkerConfig config){
        this.config = config;
    }

    public abstract boolean link(LnasmParseResult parseResult);

    public abstract T getResult();

    public LinkerConfig getConfig() {
        return config;
    }
}
