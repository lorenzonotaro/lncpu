package com.lnc.cc.codegen;

import com.lnc.assembler.common.LabelInfo;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.cc.ir.IRUnit;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.sql.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

public final class CompilerOutput {
    private final LinkedList<CodeElement> code;
    private final SectionInfo sectionInfo;
    private final IRUnit unit;
    private ArrayList<LabelInfo> labels;

    public CompilerOutput(IRUnit unit, SectionInfo sectionInfo) {
        this.sectionInfo = sectionInfo;
        this.code = new LinkedList<>();
        this.labels = new ArrayList<>();
        this.unit = unit;
        if(unit != null) {
            this.addLabel(unit.getFunctionDeclaration().name.lexeme);
        }
    }

    public void append(CodeElement codeElement) {
        if(labels != null){
            codeElement.setLabels(labels);
            labels = null;
        }
        code.add(codeElement);
    }

    public void addLabel(String lexeme) {
        if (labels == null) {
            labels = new ArrayList<>();
        }
        labels.add(new LabelInfo(Token.__internal(TokenType.IDENTIFIER, lexeme)));
    }

    public LinkedList<CodeElement> code() {
        return code;
    }

    public IRUnit unit() {
        return unit;
    }

    public SectionInfo sectionInfo() {
        return sectionInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CompilerOutput) obj;
        return Objects.equals(this.code, that.code) &&
                Objects.equals(this.sectionInfo, that.sectionInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, sectionInfo);
    }

    @Override
    public String toString() {
        return "CompilerOutput[" +
                "code=" + code + ", " +
                "sectionInfo=" + sectionInfo + ']';
    }

}
