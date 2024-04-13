package com.lnasm.compiler.linker;

import com.lnasm.compiler.CompileException;
import com.lnasm.compiler.lexer.Token;
import com.lnasm.compiler.parser.Block;
import com.lnasm.compiler.parser.Parser;

import java.util.*;

public abstract class AbstractLinker {

    private final Map<String, Short> labels;

    AbstractLinker(Map<String, Short> labels){
        this.labels = labels;
    }

    public abstract byte[] link(Set<Block> blocks);

    public short resolveLabel(String labelName, Token token) {
        if(labels.containsKey(labelName))
            return labels.get(labelName);
        throw new CompileException("unresolved label '" + labelName + "'", token);
    }

    public short resolveLabel(String parentLabel, String subLabel, Token token){
        if(parentLabel != null && subLabel.startsWith("_") && labels.containsKey(parentLabel + Parser.SUBLABEL_SEPARATOR + subLabel))
            return labels.get(parentLabel + Parser.SUBLABEL_SEPARATOR + subLabel);
        return resolveLabel(subLabel, token);
    }
}
