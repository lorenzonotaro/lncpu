package com.lnc.cc.parser;

public enum VarDeclRules {
    EXTERNAL_DECL(true, true, true, false, false),
    PARAMETER_DECL(false, false, false, true, false),
    INTERNAL_DECL(true, true, false, false, true),
    STRUCT_MEMBER_DECL(false, false, false, false, true)
    ;
    public final boolean allowInitializer;
    public final boolean allowStatic;
    public final boolean allowNonStaticNearFar;
    public final boolean isParameter;
    public final boolean expectSemicolon;

    VarDeclRules(boolean allowInitializer, boolean allowStatic, boolean allowNonStaticNearFar, boolean isParameter, boolean expectSemicolon) {
        this.allowInitializer = allowInitializer;
        this.allowStatic = allowStatic;
        this.allowNonStaticNearFar = allowNonStaticNearFar;
        this.isParameter = isParameter;
        this.expectSemicolon = expectSemicolon;
    }
}
