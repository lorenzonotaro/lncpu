package com.lnc.assembler.parser;

import com.lnc.assembler.common.IEncodeable;
import com.lnc.assembler.common.LabelInfo;
import com.lnc.cc.codegen.CodeElementVisitor;
import com.lnc.common.ExtendedListIterator;

import java.util.*;

public abstract class CodeElement implements IEncodeable {

    private List<LabelInfo> labels = new ArrayList<>();

    public List<LabelInfo> getLabels() {
        return labels;
    }

    public void setLabels(List<LabelInfo> labels) {
        this.labels = labels;
    }

    public abstract <T> T accept(CodeElementVisitor<T> visitor, ExtendedListIterator<CodeElement> iterator);

    public void clearLabels() {
        setLabels(new ArrayList<>());
    }

    @Override
    public abstract String toString();
}
