package com.lnc.cc.optimization;

public sealed interface PropValue {
    record Unknown() implements PropValue {}
    record ConstInt(int value) implements PropValue {}

    static PropValue unknown() { return new Unknown(); }
    static PropValue constant(int value) { return new ConstInt(value); }

    default boolean isConstant() { return this instanceof ConstInt; }
    default int valueOr(int fallback) {
        return this instanceof ConstInt ci ? ci.value() : fallback;
    }
}
