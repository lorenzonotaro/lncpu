package com.lnc.assembler.parser;

import com.lnc.common.frontend.AbstractLineParser;
import com.lnc.common.IntUtils;
import com.lnc.assembler.common.LabelInfo;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.parser.argument.*;
import com.lnc.assembler.parser.argument.Byte;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LnasmParser extends AbstractLineParser<ParseResult> {

    public static final String SUBLABEL_INITIATOR = "_";

    // What is used internally to store sublabels (parent label + SUBLABEL_SEPARATOR + sublabel).
    // The character should be an invalid identifier character, so that sublabels aren't accessible in any way outside of their scope.
    public static final String SUBLABEL_SEPARATOR = "$";

    public LnasmParser(List<Token[]> preprocessedLines) {
        super(preprocessedLines);
    }

    private Token currentBlockSectionToken = null;
    private List<CodeElement> currentInstructions = new ArrayList<>();

    private List<LabelInfo> currentInstructionLabels = new ArrayList<>();

    private String currentParentLabel = null;

    private final List<ParsedBlock> blocks = new ArrayList<>();

    @Override
    protected void parseLine() {
        if (!section()) {
            if (currentBlockSectionToken == null || currentInstructions == null) {
                throw error(peek(), "no section declared");
            } else if(match(Token.Type.IDENTIFIER)){
                Token labelToken = previous();
                String label = labelToken.lexeme;
                consume("expected ':' after label name", Token.Type.COLON);
                if(currentParentLabel != null && label.startsWith(SUBLABEL_INITIATOR)){
                    label = currentParentLabel + SUBLABEL_SEPARATOR + label;
                }else{
                    currentParentLabel = label;
                }
                currentInstructionLabels.add(new LabelInfo(labelToken, label));
            }else{
                CodeElement codeElement = directive();
                if(codeElement != null){
                    codeElement.setLabels(currentInstructionLabels);
                    currentInstructions.add(codeElement);
                    currentInstructionLabels = new ArrayList<>();
                }
            }
        }
        if (!isAtEnd())
            throw error(peek(), "unexpected token");
    }

    private boolean section() {
        if (match(Token.Type.DIR_SECTION)) {
            var nameToken = consume("expected section name", Token.Type.IDENTIFIER);

            if (currentBlockSectionToken != null) {
                // end of block
                blocks.add(new ParsedBlock(currentBlockSectionToken, currentInstructions.toArray(new CodeElement[0])));
            }

            currentBlockSectionToken = nameToken;
            currentParentLabel = null;
            currentInstructions = new ArrayList<>();

            return true;
        }
        return false;
    }

    private CodeElement directive() {
        if (match(Token.Type.DIR_DATA)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (check(Token.Type.INTEGER, Token.Type.STRING)) {
                Token t = advance();
                switch (t.type) {
                    case INTEGER:
                        intToBytes(baos, t);
                        break;
                    case STRING:
                        String s = (String) t.literal;
                        byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
                        baos.write(bytes, 0, bytes.length);
                        break;
                }
            }
            return EncodedData.of(baos.toByteArray());
        } else if (match(Token.Type.DIR_RES)) {
            Token t = consume("expected integer", Token.Type.INTEGER);
            int value = (Integer) t.literal;
            if (value < 0) {
                throw error(t, "value must be positive");
            }
            return EncodedData.of(new byte[value]);
        }
        return instruction();
    }

    private CodeElement instruction() {
        Token opcode = consume("expected instruction identifier", Token.Type.LNASM_INSTRUCTIONSET);
        List<Argument> arguments = new ArrayList<>();

        if(!isAtEnd())
            do{
                arguments.add(argument());
            }while(match(Token.Type.COMMA));

        return new Instruction(opcode, arguments.toArray(new Argument[0]));
    }

    private Argument argument() {
        return dereference();
    }

    private Argument dereference() {
        if(match(Token.Type.L_SQUARE_BRACKET)){
            Argument inner = bitwiseLogic();
            consume("expected closing bracket", Token.Type.R_SQUARE_BRACKET);
            return new Dereference(inner);
        }
        return bitwiseLogic();
    }

    private Argument bitwiseLogic(){
        Argument left = bitShift();

        while(match(Token.Type.BITWISE_AND, Token.Type.BITWISE_OR, Token.Type.BITWISE_XOR)){
            Token operator = previous();
            left = new BinaryOp(left, bitShift(), operator);
        }

        return left;
    }

    private Argument bitShift(){
        Argument left = addition();

        while(match(Token.Type.BITWISE_LEFT, Token.Type.BITWISE_RIGHT)){
            Token operator = previous();
            left = new BinaryOp(left, addition(), operator);
        }

        return left;
    }

    private Argument addition() {
        Argument left = multiplication();

        while(match(Token.Type.PLUS, Token.Type.MINUS)){
            Token operator = previous();
            left = new BinaryOp(left, multiplication(), operator);
        }

        return left;
    }

    private Argument multiplication() {
        Argument left = composite();

        while(match(Token.Type.STAR, Token.Type.SLASH)){
            Token operator = previous();
            left = new BinaryOp(left, composite(), operator);
        }

        return left;
    }

    private Argument composite() {
        Argument left = numberCast();
        if(match(Token.Type.COLON)) {
            Argument right = primary();
            if(left.type == Argument.Type.BYTE && right.type == Argument.Type.BYTE){
                Byte lByte = (Byte) left;
                Byte rByte = (Byte) right;
                return new Word(left.token, ((int) lByte.value << 8) | rByte.value);
            }
            return new Composite(left, right);
        }
        return left;
    }

    private Argument numberCast() {
        Argument arg = primary();

        if(match(Token.Type.DOUBLE_COLON)){
            Token castToken = previous();
            Token targetType = consume("expected target type", Token.Type.IDENTIFIER);
            return new NumberCast(arg, castToken, targetType);
        }

        return arg;
    }



    private Argument primary() {
        if(check(Token.Type.IDENTIFIER)){
            return new LabelRef(advance());
        }else if(check(Token.Type.RA, Token.Type.RB, Token.Type.RC, Token.Type.RD, Token.Type.SS, Token.Type.SP, Token.Type.DS)) {
            return new Register(advance());
        }else if(check(Token.Type.INTEGER)){
            Token t = advance();
            if(IntUtils.inByteRange((Integer) t.literal)){
                return new Byte(t);
            }else if(IntUtils.inShortRange((Integer) t.literal)){
                return new Word(t);
            }else{
                throw error(t, "value out of range");
            }
        }else if(check(Token.Type.L_PAREN)){
            advance();
            Argument inner = argument();
            consume("expected closing parentheses", Token.Type.R_PAREN);
            return inner;
        }
        throw error(peek(), "expected argument");
    }


    @Override
    protected void endParse() {
        blocks.add(new ParsedBlock(currentBlockSectionToken, currentInstructions.toArray(new CodeElement[0])));
    }

    @Override
    public ParseResult getResult() {
        return new ParseResult(blocks.toArray(new ParsedBlock[0]));
    }

    private static void intToBytes(ByteArrayOutputStream baos, Token token) {
        int value = (Integer) token.literal;
        if (IntUtils.inByteRange(value)) {
            baos.write(value);
        } else if (IntUtils.inShortRange(value)) {
            baos.write((value >> 8) & 0xFF);
            baos.write(value & 0xFF);
        } else {
            throw error(token, "value out of range for word");
        }
    }

}
