package com.lnc.cc.types;

import com.lnc.cc.parser.LncParser;
import com.lnc.common.frontend.TokenType;

public record StorageQualifier(boolean isExtern, boolean isStatic, boolean isExport, StorageLocation pointerKind)
{
    public static final StorageQualifier NONE = new StorageQualifier(false, false, false,  StorageLocation.NEAR);

    public static final TokenType[] VALID_TOKENS = new TokenType[] {
        TokenType.EXTERN,
        TokenType.STATIC,
        TokenType.EXPORT,
        TokenType.NEAR,
            TokenType.FAR,
    };

    public static StorageQualifier parse(LncParser parser){
        boolean isExtern = false;
        boolean isStatic = false;
        boolean isExport = false;
        boolean hasNear = false;
        boolean hasFar = false;
        StorageLocation pointerKind = StorageLocation.NEAR;
        while(parser.check(VALID_TOKENS)){
            TokenType tokenType = parser.advance().type;
            switch(tokenType){
                case EXTERN -> {
                    if(isExtern) throw new RuntimeException("duplicate 'extern' qualifier");
                    isExtern = true;
                }
                case STATIC -> {
                    if(isStatic) throw new RuntimeException("duplicate 'static' qualifier");
                    isStatic = true;
                }
                case EXPORT -> {
                    if(isExport) throw new RuntimeException("duplicate 'export' qualifier");
                    isExport = true;
                }
                case NEAR -> {
                    if(hasNear) throw new RuntimeException("duplicate 'near' qualifier");
                    if(hasFar) throw new RuntimeException("cannot have both 'near' and 'far' qualifiers");
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
        return new StorageQualifier(isExtern, isStatic, isExport, pointerKind);
    }

    public boolean isNone() {
        return NONE.equals(this);
    }
}
