package com.lnc.cc.parser;

enum VarDeclRules {
    EXTERNAL_DECL(true, true, true, false, false),
    PARAMETER_DECL(false, false, false, true, false),
    INTERNAL_DECL(true, true, false, false, true),
    STRUCT_MEMBER_DECL(false, false, false, false, true)
    ;
    final boolean allowInitializer;
    final boolean allowStatic;
    final boolean allowNonStaticNearFar;
    final boolean isParameter;
    final boolean expectSemicolon;

    VarDeclRules(boolean allowInitializer, boolean allowStatic, boolean allowNonStaticNearFar, boolean isParameter, boolean expectSemicolon) {
        this.allowInitializer = allowInitializer;
        this.allowStatic = allowStatic;
        this.allowNonStaticNearFar = allowNonStaticNearFar;
        this.isParameter = isParameter;
        this.expectSemicolon = expectSemicolon;
    }
}
