package com.lnc.assembler.linker;

import com.lnc.assembler.common.*;
import com.lnc.common.frontend.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LinkerConfigParser extends FullSourceParser<LinkerConfig> {
    private LinkerConfig linkerConfig;

    public LinkerConfigParser(List<Token[]> lines) {
        this(lines.stream().flatMap(Arrays::stream).toArray(Token[]::new));
    }

    public LinkerConfigParser(Token[] tokens) {
        super(tokens);
    }

    @Override
    public boolean parse() {

        try{
            consume("expected 'SECTIONS'", TokenType.SECTIONS);
            var sections = sections();
            this.linkerConfig = new LinkerConfig(sections);
        }catch(CompileException e){
            e.log();
            return false;
        }

        return true;
    }

    private SectionInfo[] sections() {
        consume("expected '['", TokenType.L_SQUARE_BRACKET);

        var sections = new ArrayList<SectionInfo>();

        do{
            var si = sectionInfo();
            sections.add(si);
            if(check(TokenType.SEMICOLON))
                advance();
        }while(!check(TokenType.R_SQUARE_BRACKET));

        consume("expected '['", TokenType.R_SQUARE_BRACKET);

        return sections.toArray(new SectionInfo[0]);
    }

    private SectionInfo sectionInfo() {
        var sectionName = consume("expected section name", TokenType.IDENTIFIER);
        consume("expected ':'", TokenType.COLON);

        var name = sectionName.lexeme;
        var start = -1;
        LinkTarget target = null;
        LinkMode mode = null;
        Boolean multiWriteAllowed = null;
        Boolean dataPage = null;
        Boolean virtual = null;

        do{
            var propName = consume("expected property name", TokenType.IDENTIFIER);
            Token propValue;

            if(propName.lexeme.equals("datapage") || propName.lexeme.equals("virtual") || propName.lexeme.equals("multi")){
                if(match(TokenType.EQUALS)) {
                    propValue = consume("expected property value", TokenType.IDENTIFIER);
                }else propValue = new Token(TokenType.IDENTIFIER, "true", "true", previous().location);
            }else{
                consume("expected '='", TokenType.EQUALS);
                propValue = consume("expected property value", TokenType.IDENTIFIER, TokenType.INTEGER);
            }


            if(propName.lexeme.equals("start")){
                if(start != -1)
                    throw error(propName, "duplicate property 'start'");
                start = (int) propValue.literal;

                if(start < 0 || start > 0xFFFF)
                    throw error(propValue, "start address out of range (0-0xFFFF)");

            }else if(propName.lexeme.equals("target")){
                if(target != null)
                    throw error(propName, "duplicate property 'target'");

                try {
                    target = LinkTarget.valueOf(propValue.lexeme.toUpperCase());
                }catch(IllegalArgumentException e){
                    throw error(propValue, "unknown section target");
                }
            }else if(propName.lexeme.equals("mode")){
                if(mode != null)
                    throw error(propName, "duplicate property 'mode'");

                try {
                    mode = LinkMode.from(propValue.lexeme.toUpperCase());
                }catch(IllegalArgumentException e) {
                    throw error(propValue, "unknown section mode");
                }
            }else if(propName.lexeme.equals("multi")){
                if (multiWriteAllowed != null)
                    throw error(propName, "duplicate property 'multi'");
                if(propValue.lexeme.equals("true")){
                    multiWriteAllowed = true;
                }else if(propValue.lexeme.equals("false")){
                    multiWriteAllowed = false;
                }else{
                    throw error(propValue, "invalid value for 'multi' property (true/false)");
                }
            }else if(propName.lexeme.equals("datapage")) {
                if (dataPage != null)
                    throw error(propName, "duplicate property 'datapage'");
                if (propValue.lexeme.equals("true")) {
                    dataPage = true;
                } else if (propValue.lexeme.equals("false")) {
                    dataPage = false;
                } else {
                    throw error(propValue, "invalid value for 'datapage' property (true/false)");
                }
            }else if(propName.lexeme.equals("virtual")){
                if(virtual != null)
                    throw error(propName, "duplicate property 'virtual'");
                if(propValue.lexeme.equals("true")){
                    virtual = true;
                }else if(propValue.lexeme.equals("false")){
                    virtual = false;
                }else{
                    throw error(propValue, "invalid value for 'virtual' property (true/false)");
                }
            }else{
                throw error(propName, "unknown property");
            }

            if(check(TokenType.COMMA))
                advance();

        }while(!check(TokenType.SEMICOLON, TokenType.R_SQUARE_BRACKET));

        try {
            return new SectionInfo(name, start, target, mode, multiWriteAllowed != null && multiWriteAllowed, dataPage != null && dataPage, virtual != null && virtual);
        } catch (IllegalArgumentException e) {
            throw error(sectionName, "invalid section: " + e.getMessage());
        }
    }


    @Override
    public LinkerConfig getResult() {
        return linkerConfig;
    }
}
