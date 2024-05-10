package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.parser.ParseResult;

public abstract class AbstractLinker {

    private final LinkerConfig config;

    AbstractLinker(LinkerConfig config){
        this.config = config;
    }

    public abstract byte[] link(ParseResult parseResult);

    public LinkerConfig getConfig() {
        return config;
    }
}
