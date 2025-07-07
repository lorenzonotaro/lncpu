package com.lnc.assembler.parser;

import com.lnc.common.frontend.AbstractLineParser;
import com.lnc.common.IntUtils;
import com.lnc.assembler.common.LabelInfo;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.parser.argument.*;
import com.lnc.assembler.parser.argument.Byte;
import com.lnc.common.frontend.TokenType;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LnasmParser extends AbstractLineParser<LnasmParseResult> {

    public static final String SUBLABEL_INITIATOR = "_";

    // What is used internally to store sublabels (parent label + SUBLABEL_SEPARATOR + sublabel).
    // The character should be an invalid identifier character, so that sublabels aren't accessible in any way outside of their scope.
    public static final String SUBLABEL_SEPARATOR = "$";

    public LnasmParser(List<Token[]> preprocessedLines) {
        super(preprocessedLines);
    }

    private Token currentBlockSectionToken = null;
    private LinkedList<CodeElement> currentInstructions = new LinkedList<>();

    private List<LabelInfo> currentInstructionLabels = new ArrayList<>();

    private String currentParentLabel = null;

    private final List<LnasmParsedBlock> blocks = new ArrayList<>();

    @Override
    protected void parseLine() {
        if (!section()) {
            if (currentBlockSectionToken == null || currentInstructions == null) {
                throw error(peek(), "no section declared");
            } else if(match(TokenType.IDENTIFIER)){
                Token labelToken = previous();
                String label = labelToken.lexeme;
                consume("expected ':' after label name", TokenType.COLON);
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
        if (match(TokenType.DIR_SECTION)) {
            var nameToken = consume("expected section name", TokenType.IDENTIFIER);

            if (currentBlockSectionToken != null) {
                // end of block
                blocks.add(new LnasmParsedBlock(currentBlockSectionToken, currentInstructions));
            }

            currentBlockSectionToken = nameToken;
            currentParentLabel = null;
            currentInstructions = new LinkedList<>();

            return true;
        }
        return false;
    }

    private CodeElement directive() {
        if (match(TokenType.DIR_DATA)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (check(TokenType.INTEGER, TokenType.STRING)) {
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
        } else if (match(TokenType.DIR_RES)) {
            Token t = consume("expected integer", TokenType.INTEGER);
            int value = (Integer) t.literal;
            if (value < 0) {
                throw error(t, "value must be positive");
            }
            return EncodedData.of(new byte[value]);
        }
        return instruction();
    }

    private CodeElement instruction() {
        Token opcode = consume("expected instruction identifier", TokenType.LNASM_INSTRUCTIONSET);
        List<Argument> arguments = new ArrayList<>();

        if(!isAtEnd())
            do{
                arguments.add(argument());
            }while(match(TokenType.COMMA));

        return new Instruction(opcode, arguments.toArray(new Argument[0]));
    }

    private Argument argument() {
        return dereference();
    }

    private Argument dereference() {
        if(match(TokenType.L_SQUARE_BRACKET)){
            Argument inner = bitwiseLogic();
            consume("expected closing bracket", TokenType.R_SQUARE_BRACKET);
            return new Dereference(inner);
        }
        return bitwiseLogic();
    }

    private Argument bitwiseLogic(){
        Argument left = bitShift();

        while(match(TokenType.AMPERSAND, TokenType.BITWISE_OR, TokenType.BITWISE_XOR)){
            Token operator = previous();
            left = new BinaryOp(left, bitShift(), operator);
        }

        return left;
    }

    private Argument bitShift(){
        Argument left = addition();

        while(match(TokenType.BITWISE_LEFT, TokenType.BITWISE_RIGHT)){
            Token operator = previous();
            left = new BinaryOp(left, addition(), operator);
        }

        return left;
    }

    private Argument addition() {
        Argument left = multiplication();

        while(match(TokenType.PLUS, TokenType.MINUS)){
            Token operator = previous();

            if (left.type == Argument.Type.REGISTER){
                var right = multiplication();

                if (right instanceof NumericalArgument){
                    left = new RegisterOffset(left, operator, (NumericalArgument) right);
                }else{
                    left = new BinaryOp(left, right, operator);
                }
            }else {
                left = new BinaryOp(left, multiplication(), operator);
            }
        }

        return left;
    }

    private Argument multiplication() {
        Argument left = composite();

        while(match(TokenType.STAR, TokenType.SLASH)){
            Token operator = previous();
            left = new BinaryOp(left, composite(), operator);
        }

        return left;
    }

    private Argument composite() {
        Argument left = numberCast();
        if(match(TokenType.COLON)) {
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

        if(match(TokenType.DOUBLE_COLON)){
            Token castToken = previous();
            Token targetType = consume("expected target type", TokenType.IDENTIFIER);
            return new NumberCast(arg, castToken, targetType);
        }

        return arg;
    }



    private Argument primary() {
        if(check(TokenType.IDENTIFIER)){
            return new LabelRef(advance());
        }else if(check(TokenType.RA, TokenType.RB, TokenType.RC, TokenType.RD, TokenType.SS, TokenType.SP, TokenType.BP, TokenType.DS)) {
            return new Register(advance());
        }else if(check(TokenType.INTEGER)){
            Token t = advance();
            if(IntUtils.inByteRange((Integer) t.literal)){
                return new Byte(t);
            }else if(IntUtils.inShortRange((Integer) t.literal)){
                return new Word(t);
            }else{
                throw error(t, "value out of range");
            }
        }else if(check(TokenType.L_PAREN)){
            advance();
            Argument inner = argument();
            consume("expected closing parentheses", TokenType.R_PAREN);
            return inner;
        }
        throw error(peek(), "expected argument");
    }


    @Override
    protected void endParse() {
        if(!currentInstructions.isEmpty())
            blocks.add(new LnasmParsedBlock(currentBlockSectionToken, currentInstructions));
    }

    @Override
    public LnasmParseResult getResult() {
        return new LnasmParseResult(blocks);
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
