package com.lnc.assembler.linker;

import com.lnc.assembler.parser.ParseResult;

public abstract class AbstractLinker<T> {

    private final LinkerConfig config;

    AbstractLinker(LinkerConfig config){
        this.config = config;
    }

    public abstract boolean link(ParseResult parseResult);

    public abstract T getResult();

    public LinkerConfig getConfig() {
        return config;
    }
}
