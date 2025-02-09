package com.lnc.assembler.parser;

import com.lnc.assembler.common.IEncodeable;
import com.lnc.assembler.common.LabelInfo;

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
