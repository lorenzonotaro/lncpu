package com.lnc.cc.types;

import com.lnc.cc.parser.LncParser;
import com.lnc.cc.parser.VarDeclRules;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

public record StorageQualifier(boolean isExtern, boolean isStatic, boolean isExport, StorageLocation storageLocation)
{
    public static final StorageQualifier NONE = new StorageQualifier(false, false, false,  StorageLocation.NEAR);

    public static final TokenType[] VALID_TOKENS = new TokenType[] {
        TokenType.EXTERN,
        TokenType.STATIC,
        TokenType.EXPORT,
        TokenType.NEAR,
            TokenType.FAR,
    };

    public static StorageQualifier parse(LncParser parser, VarDeclRules rules){
        boolean isExtern = false;
        boolean isStatic = false;
        boolean isExport = false;
        boolean hasNear = false;
        boolean hasFar = false;
        StorageLocation pointerKind = StorageLocation.NEAR;
        while(parser.check(VALID_TOKENS)){
            Token token = parser.advance();
            TokenType tokenType = token.type;
            switch(tokenType){
                case EXTERN -> {
                    if(isExtern) throw new CompileException("duplicate 'extern' qualifier", token);
                    isExtern = true;
                }
                case STATIC -> {
                    if(isStatic) throw new CompileException("duplicate 'static' qualifier", token);
                    if(!rules.allowStatic)
                        throw new CompileException("'static' qualifier not allowed in this context", token);
                    isStatic = true;
                }
                case EXPORT -> {
                    if(isExport) throw new CompileException("duplicate 'export' qualifier", token);
                    isExport = true;
                }
                case NEAR -> {
                    if(hasNear) throw new CompileException("duplicate 'near' qualifier", token);
                    if(hasFar) throw new CompileException("cannot have both 'near' and 'far' qualifiers", token);
                    hasNear = true;
                    pointerKind = StorageLocation.NEAR;
                }
                case FAR -> {
                    if(hasFar) throw new RuntimeException("duplicate 'far' qualifier");
                    if(hasNear) throw new RuntimeException("cannot have both 'near' and 'far' qualifiers");
                    hasFar = true;
                    pointerKind = StorageLocation.FAR;
                }
            }
        }
        if((hasFar || hasNear) && !isStatic && !rules.allowNonStaticNearFar){
            throw new RuntimeException("cannot have 'near' or 'far' qualifiers without 'static' qualifier");
        }
        return new StorageQualifier(isExtern, isStatic, isExport, pointerKind);
    }

    public boolean isNone() {
        return NONE.equals(this);
    }
}
