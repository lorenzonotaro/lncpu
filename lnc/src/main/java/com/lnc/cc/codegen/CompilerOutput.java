package com.lnc.cc.codegen;

import com.lnc.assembler.common.LabelInfo;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.LnasmParser;
import com.lnc.cc.ir.IRUnit;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

public final class CompilerOutput {
    private final LinkedList<CodeElement> code;
    private final SectionInfo sectionInfo;
    private final IRUnit unit;
    private ArrayList<LabelInfo> labels;

    public CompilerOutput(SectionInfo sectionInfo) {
        this.sectionInfo = sectionInfo;
        this.code = new LinkedList<>();
        this.labels = new ArrayList<>();
        this.unit = null; // No IRUnit associated with this output
    }

    public CompilerOutput(IRUnit unit, SectionInfo sectionInfo) {
        this.sectionInfo = sectionInfo;
        this.code = new LinkedList<>();
        this.labels = new ArrayList<>();
        this.unit = unit;
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
        labels.add(new LabelInfo(Token.__internal(TokenType.IDENTIFIER, lexeme), unit != null ? unit.getFunctionDeclaration().name.lexeme + LnasmParser.SUBLABEL_SEPARATOR + lexeme : lexeme));
    }


    public void addUnitLabel() {
        var label = Objects.requireNonNull(unit).getFunctionDeclaration().name.lexeme;
        labels.add(new LabelInfo(Token.__internal(TokenType.IDENTIFIER, label), label));
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
        StringBuilder sb = new StringBuilder();
        sb.append(".section ").append(sectionInfo.getName()).append("\n");

        for (CodeElement element : code) {
            for(var label : element.getLabels()) {
                sb.append(label.extractSubLabelName()).append(":\n");
            }
            sb.append("\t").append(element).append("\n");
        }

        return sb.toString();
    }

}
