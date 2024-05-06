package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.parser.Block;
import com.lnasm.compiler.parser.ParseResult;

import java.util.*;

public abstract class AbstractLinker {

    private final LinkerConfig config;

    AbstractLinker(LinkerConfig config){
        this.config = config;
    }

    public abstract byte[] link(ParseResult parseResult);

    public abstract short resolveLabel(String labelName, Token token);

    public LinkerConfig getConfig() {
        return config;
    }
}
