package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LinkerConfigParser extends AbstractParser<LinkerConfig> {
    private final Token[] tokens;
    private int index;
    private LinkerConfig linkerConfig;

    public LinkerConfigParser(List<Token[]> lines) {
        this(lines.stream().flatMap(Arrays::stream).toArray(Token[]::new));
    }

    public LinkerConfigParser(Token[] tokens) {
        this.tokens = tokens;
        this.index = 0;
    }

    @Override
    public boolean parse() {

        try{
            consume("expected 'SECTIONS'", Token.Type.SECTIONS);
            var sections = sections();
            this.linkerConfig = new LinkerConfig(sections);
        }catch(CompileException e){
            e.log();
            return false;
        }

        return true;
    }

    private SectionInfo[] sections() {
        consume("expected '['", Token.Type.L_SQUARE_BRACKET);

        var sections = new ArrayList<SectionInfo>();

        do{
            var si = sectionInfo();
            sections.add(si);
            if(check(Token.Type.SEMICOLON))
                advance();
        }while(!check(Token.Type.R_SQUARE_BRACKET));

        consume("expected '['", Token.Type.R_SQUARE_BRACKET);

        return sections.toArray(new SectionInfo[0]);
    }

    private SectionInfo sectionInfo() {
        var sectionName = consume("expected section name", Token.Type.IDENTIFIER);
        consume("expected ':'", Token.Type.COLON);

        var name = sectionName.lexeme;
        var start = -1;
        SectionType type = null;
        SectionMode mode = null;

        do{
            var propName = consume("expected property name", Token.Type.IDENTIFIER);

            consume("expected '='", Token.Type.EQUALS);

            var propValue = consume("expected property value", Token.Type.IDENTIFIER, Token.Type.INTEGER);

            if(propName.lexeme.equals("start")){
                if(start != -1)
                    throw error(propName, "duplicate property 'start'");
                start = (int) propValue.literal;

                if(start < 0 || start > 0xFFFF)
                    throw error(propValue, "start address out of range (0-0xFFFF)");

            }else if(propName.lexeme.equals("type")){
                if(type != null)
                    throw error(propName, "duplicate property 'type'");

                try {
                    type = SectionType.valueOf(propValue.lexeme.toUpperCase());
                }catch(IllegalArgumentException e){
                    throw error(propValue, "unknown section type");
                }
            }else if(propName.lexeme.equals("mode")){
                if(mode != null)
                    throw error(propName, "duplicate property 'mode'");

                try {
                    mode = SectionMode.from(propValue.lexeme.toUpperCase());
                }catch(IllegalArgumentException e) {
                    throw error(propValue, "unknown section mode");
                }
            }else{
                throw error(propName, "unknown property");
            }

            if(check(Token.Type.COMMA))
                advance();

        }while(!check(Token.Type.SEMICOLON, Token.Type.R_SQUARE_BRACKET));

        try {
            return new SectionInfo(name, start, type, mode);
        } catch (IllegalArgumentException e) {
            throw error(sectionName, "invalid section: " + e.getMessage());
        }
    }

    @Override
    protected boolean isAtEnd() {
        return index >= tokens.length;
    }

    @Override
    protected Token advance() {
        if(isAtEnd())
            throw error(previous(), "unexpected end of file");
        return tokens[index++];
    }

    @Override
    protected Token previous() {
        return tokens[index - 1];
    }

    @Override
    protected Token peek() {
        if(isAtEnd())
            throw error(previous(), "unexpected end of file");
        return tokens[index];
    }


    @Override
    public LinkerConfig getResult() {
        return linkerConfig;
    }
}
