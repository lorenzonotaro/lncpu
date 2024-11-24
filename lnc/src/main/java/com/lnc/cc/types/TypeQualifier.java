package com.lnc.cc.types;

import com.lnc.cc.parser.LncParser;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

public record TypeQualifier(boolean isExtern, boolean isStatic, boolean isDpage) {
    public static final TypeQualifier NONE = new TypeQualifier(false, false, false);

    public static final TokenType[] VALID_TOKENS = new TokenType[] {
        TokenType.EXTERN,
        TokenType.STATIC,
        TokenType.DPAGE
    };

    public static TypeQualifier parse(LncParser parser) {
        boolean isExtern = false;
        boolean isStatic = false;
        boolean isDpage = false;

        while(parser.check(VALID_TOKENS)){
            Token token = parser.advance();
            switch(token.type){
                case EXTERN:
                    if(isExtern) throw new CompileException("duplicate 'extern' qualifier", token);
                    isExtern = true;
                break;
                case STATIC:
                    if(isStatic) throw new CompileException("duplicate 'static' qualifier", token);
                    isStatic = true;
                break;
                case DPAGE:
                    if(isDpage) throw new CompileException("duplicate 'dpage' qualifier", token);
                    isDpage = true;
                break;
                default:
                    throw new CompileException("Invalid type qualifier: " + token, token);
            }
        }

        return new TypeQualifier(isExtern, isStatic, isDpage);
    }

    public boolean isNone() {
        return !isExtern && !isStatic && !isDpage;
    }
}
