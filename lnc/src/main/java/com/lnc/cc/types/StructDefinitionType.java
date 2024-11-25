package com.lnc.cc.types;

import com.lnc.cc.ast.VariableDeclaration;
import com.lnc.common.frontend.Token;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StructDefinitionType{

    private final Token defToken;
    private final List<VariableDeclaration> fields;
    private Map<String, StructFieldEntry> fieldMap;

    public StructDefinitionType(Token defToken, List<VariableDeclaration> fields) {
        this.defToken = defToken;
        this.fields = fields;
    }

    public int typeSize() {
        return fields.stream().mapToInt(v -> v.declarator.typeSpecifier().typeSize()).sum();
    }

    public int allocSize() {
        return fields.stream().mapToInt(v -> v.declarator.typeSpecifier().allocSize()).sum();
    }

    public List<VariableDeclaration> getFields() {
        return fields;
    }

    public boolean isComplete() {
        return fieldMap != null;
    }

    public void setFieldMap(Map<String, StructFieldEntry> fieldMap) {
        this.fieldMap = fieldMap;
    }

    @Override
    public String toString() {
        return "struct " + defToken.lexeme;
    }

    public Token getDefinitionToken() {
        return defToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StructDefinitionType that = (StructDefinitionType) o;
        return defToken.equals(that.defToken) && fields.equals(that.fields) && Objects.equals(fieldMap, that.fieldMap);
    }

    @Override
    public int hashCode() {
        int result = defToken.hashCode();
        result = 31 * result + fields.hashCode();
        result = 31 * result + Objects.hashCode(fieldMap);
        return result;
    }

    public StructFieldEntry getField(String lexeme) {
        return fieldMap.get(lexeme);
    }
}
