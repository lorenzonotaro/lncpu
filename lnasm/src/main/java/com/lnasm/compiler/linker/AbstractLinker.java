package com.lnasm.compiler.linker;

import com.lnasm.compiler.parser.ParseResult;

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
