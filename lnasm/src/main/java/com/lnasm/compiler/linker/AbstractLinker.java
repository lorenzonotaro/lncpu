package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.parser.Block;
import java.util.*;

public abstract class AbstractLinker {


    private final LinkerConfig config;

    AbstractLinker(LinkerConfig config){

        this.config = config;
    }

    public abstract byte[] link(Set<Block> blocks);

    public short resolveLabel(String labelName, Token token) {
        return 0;
    }

    public short resolveLabel(String parentLabel, String subLabel, Token token){
        return 0;
    }

    public LinkerConfig getConfig() {
        return config;
    }
}
