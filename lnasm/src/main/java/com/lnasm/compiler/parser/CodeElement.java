package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.IEncodeable;
import com.lnasm.compiler.common.LabelInfo;
import com.lnasm.compiler.common.Token;

import java.util.HashSet;
import java.util.Set;

public abstract class CodeElement implements IEncodeable {

    private Set<LabelInfo> labels = new HashSet<>();

    public Set<LabelInfo> getLabels() {
        return labels;
    }

    public void setLabels(Set<LabelInfo> labels) {
        this.labels = labels;
    }
}
