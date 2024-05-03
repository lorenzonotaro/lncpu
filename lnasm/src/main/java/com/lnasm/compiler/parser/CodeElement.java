package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.IEncodeable;

import java.util.HashSet;
import java.util.Set;

public abstract class CodeElement implements IEncodeable {

    private Set<String> labels = new HashSet<>();

    public Set<String> getLabels() {
        return labels;
    }

    public void setLabels(Set<String> labels) {
        this.labels = labels;
    }
}
