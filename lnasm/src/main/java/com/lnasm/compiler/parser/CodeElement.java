package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.IEncodeable;
import com.lnasm.compiler.common.LabelInfo;
import com.lnasm.compiler.common.Token;

import java.util.*;

public abstract class CodeElement implements IEncodeable {

    private List<LabelInfo> labels = new ArrayList<>();

    public List<LabelInfo> getLabels() {
        return labels;
    }

    public void setLabels(List<LabelInfo> labels) {
        this.labels = labels;
    }
}
