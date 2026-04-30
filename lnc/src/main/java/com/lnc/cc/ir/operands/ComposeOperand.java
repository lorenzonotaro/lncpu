package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;

import java.util.List;
import java.util.stream.Stream;

public class ComposeOperand extends IROperand {
    public IROperand high;
    public IROperand low;
    private final TypeSpecifier typeSpecifier;

    public ComposeOperand(IROperand high, IROperand low, TypeSpecifier typeSpecifier) {
        super(Type.COMPOSE);
        this.high = high;
        this.low = low;
        this.typeSpecifier = typeSpecifier;
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return typeSpecifier;
    }

    @Override
    public String toString() {
        return "(" + high + " << 8 + " + low + ")";
    }

    @Override
    public List<VirtualRegister> getVRReads() {
        return Stream.of(
                high instanceof VirtualRegister ? List.of((VirtualRegister) high) : high.getVRReads(),
                low instanceof VirtualRegister ? List.of((VirtualRegister) low) : low.getVRReads()
        ).flatMap(List::stream).toList();
    }
}
