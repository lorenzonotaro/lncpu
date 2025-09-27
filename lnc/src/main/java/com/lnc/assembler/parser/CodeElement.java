package com.lnc.assembler.parser;

import com.lnc.assembler.common.IEncodeable;
import com.lnc.assembler.common.LabelInfo;
import com.lnc.cc.codegen.CodeElementVisitor;
import com.lnc.common.ExtendedListIterator;

import java.util.*;

/**
 * Represents an abstract base class for any element that can be processed in a code structure.
 * This class provides functionality to manage labels associated with a code element,
 * and defines an interface for traversal and visitor patterns.
 *
 * Implementing classes are required to provide specific behavior for encoding,
 * determining size, and accepting visitors.
 */
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
