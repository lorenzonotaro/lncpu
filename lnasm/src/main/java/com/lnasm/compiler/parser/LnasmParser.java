package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.AbstractLineParser;
import com.lnasm.compiler.common.IntUtils;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.linker.LinkerLabelSectionLocator;
import com.lnasm.compiler.parser.argument.*;
import com.lnasm.compiler.parser.argument.Byte;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LnasmParser extends AbstractLineParser<ParseResult> {

    private static final String SUBLABEL_INITIATOR = "_";

    // What is used internally to store sublabels (parent label + SUBLABEL_SEPARATOR + sublabel).
    // The character should be an invalid identifier character, so that sublabels aren't accessible in any way outside of their scope.
    public static final String SUBLABEL_SEPARATOR = "$";

    public LnasmParser(List<Token[]> preprocessedLines) {
        super(preprocessedLines);
    }

    private String currentBlockSectionName;
    private List<CodeElement> currentInstructions = new ArrayList<>();

    private Set<String> currentInstructionLabels = new HashSet<>();

    private String currentParentLabel = null;

    private final List<ParsedBlock> blocks = new ArrayList<>();

    @Override
    protected void parseLine() {
        if (!section()) {
            if (currentBlockSectionName == null || currentInstructions == null) {
                throw error(peek(), "no section declared");
            } else if(match(Token.Type.IDENTIFIER)){
                String label = previous().lexeme;
                consume("expected ':' after label name", Token.Type.COLON);
                if(currentParentLabel != null && label.startsWith(SUBLABEL_INITIATOR)){
                    label = currentParentLabel + SUBLABEL_SEPARATOR + label;
                }else{
                    currentParentLabel = label;
                }
                currentInstructionLabels.add(label);
            }else{
                CodeElement codeElement = directive();
                if(codeElement != null){
                    codeElement.setLabels(currentInstructionLabels);
                    currentInstructions.add(codeElement);
                    currentInstructionLabels = new HashSet<>();
                }
            }
        }
        if (!isAtEnd())
            throw error(peek(), "unexpected token");
    }

    private boolean section() {
        if (match(Token.Type.DIR_SECTION)) {
            var nameToken = consume("expected section name", Token.Type.IDENTIFIER);

            if (currentBlockSectionName != null) {
                // end of block
                blocks.add(new ParsedBlock(currentBlockSectionName, currentInstructions.toArray(new CodeElement[0])));
            }

            currentBlockSectionName = nameToken.lexeme;
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
            Argument inner = composite();
            consume("expected closing bracket", Token.Type.R_SQUARE_BRACKET);
            return new Dereference(inner);
        }
        return composite();
    }

    private Argument composite() {
        Argument left = primary();
        if(match(Token.Type.COLON)) {
            Argument right = primary();
            if(left.type == Argument.Type.BYTE && right.type == Argument.Type.BYTE){
                Byte lByte = (Byte) left;
                Byte rByte = (Byte) right;
                return new Word(left.token, ((int) lByte.value << 8) | rByte.value);
            }
        }
        return left;
    }

    private Argument primary() {
        if(check(Token.Type.IDENTIFIER)){
            return new LabelRef(advance());
        }else if(check(Token.Type.RA, Token.Type.RB, Token.Type.RC, Token.Type.RD, Token.Type.SS, Token.Type.SP)) {
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
        }
        throw error(peek(), "expected argument");
    }


    @Override
    protected void endParse() {
        blocks.add(new ParsedBlock(currentBlockSectionName, currentInstructions.toArray(new CodeElement[0])));
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
