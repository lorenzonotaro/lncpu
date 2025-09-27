package com.lnc.assembler.linker;

import com.lnc.assembler.parser.LnasmParseResult;

import java.util.Set;

/**
 * Represents an abstract base class for creating linkers.
 * This class defines the structure and behavior required for any linker implementation,
 * including methods for linking parsed results and retrieving linking outputs.
 *
 * @param <T> the type of the result produced by the linker implementation
 */
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
