package com.lnc.cc.types;

import com.lnc.cc.ast.FunctionDeclaration;
import com.lnc.cc.ir.CallingConvention;

import java.util.Arrays;
import java.util.List;

public class FunctionType extends TypeSpecifier {

    public final FunctionDeclaration functionDeclaration;
    public final TypeSpecifier returnType;
    public final TypeSpecifier[] parameterTypes;
    private final List<CallingConvention.ParamLocation> parameterMapping;

    private FunctionType(FunctionDeclaration functionDeclaration, TypeSpecifier returnType) {
        super(Type.FUNCTION);
        this.functionDeclaration = functionDeclaration;
        this.returnType = returnType;
        this.parameterTypes = Arrays.stream(functionDeclaration.parameters).map(p -> p.declarator.typeSpecifier()).toArray(TypeSpecifier[]::new);
        this.parameterMapping = CallingConvention.mapCallArguments(functionDeclaration.parameters);
    }

    public static FunctionType of(FunctionDeclaration functionDeclaration) {
        return new FunctionType(functionDeclaration, functionDeclaration.declarator.typeSpecifier());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType).append(" (*)(");
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameterTypes[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FunctionType that = (FunctionType) o;
        return returnType.equals(that.returnType) && Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        int result = returnType.hashCode();
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    @Override
    public int typeSize() {
        return 2;
    }

    public List<CallingConvention.ParamLocation> getParameterMapping() {
        return parameterMapping;
    }
}
