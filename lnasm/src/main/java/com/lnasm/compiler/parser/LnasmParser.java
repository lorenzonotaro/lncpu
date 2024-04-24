package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.AbstractLineParser;
import com.lnasm.compiler.common.Encodeable;
import com.lnasm.compiler.common.EncodedData;
import com.lnasm.compiler.lexer.Token;
import com.lnasm.compiler.parser.ast.Argument;
import com.lnasm.compiler.parser.ast.Matcher;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LnasmParser extends AbstractLineParser<Set<Block>> {

    // The starting character of a sublabel.
    private static final String SUBLABEL_INITIATOR = "_";

    // What is used internally to store sublabels (parent label + SUBLABEL_SEPARATOR + sublabel).
    // The character should be an invalid identifier character, so that sublabels aren't accessible in any way outside of their scope.
    public static final String SUBLABEL_SEPARATOR = "$";
    private final Set<Block> blocks;
    private Block currentBlock;
    private String currentParentLabel = null;
    private final Map<String, Short> labels;

    public LnasmParser(List<Token[]> tokens) {
        super(tokens);
        blocks = new HashSet<>();
        labels = new HashMap<>();
        this.currentParentLabel = null;
    }

    @Override
    protected void parseLine() {
        Encodeable instr;
        if ((instr = doParseLine()) != null) {
            if (!currentBlock.addInstruction(instr)) {
                throw error(previous(), "exceeded maximum code size (65536)");
            }
        }
    }

    @Override
    protected void endParse(){
        if(currentBlock != null) {
            blocks.add(currentBlock);
        }
    }

    private Encodeable doParseLine() {
        if (match(Token.Type.DIR_ORG)) {
            newBlock(previous(), consume("expected number", Token.Type.INTEGER));
            return null;
        } else if (currentBlock == null)
            throw error(peek(), "initial code segment not specified");
        else if (match(Token.Type.DIR_DATA)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (!isAtEnd()) {
                Token t = consume("expected encodeable constant value(s) (strings, bytes)", Token.Type.STRING, Token.Type.INTEGER);
                switch (t.type) {
                    case STRING:
                        byte[] strBytes = ((String) t.literal).getBytes(StandardCharsets.US_ASCII);
                        baos.write(strBytes, 0, strBytes.length);
                        break;
                    case INTEGER:
                        byte bVal = ensureByte(t, (Integer) t.literal);
                        baos.write(bVal);
                }
            }
            return new EncodedData(baos.toByteArray());

        }else if (match(Token.Type.DIR_RES)) {
            Token amount = consume("expected integer", Token.Type.INTEGER);
            return new EncodedData(new byte[(Integer) amount.literal]);
        } else return instruction();
    }

    private Encodeable instruction() {
        if (match(Token.Type.IDENTIFIER)) {
            label();
            return null;
        }
        Token t = advance();
        Argument[] arguments = arguments();
        Set<Matcher> matchers = Matcher.getMatchers();
        for (Matcher matcher : matchers) {
            if (matcher.getKeyword() == t.type && matcher.matches(arguments)) {
                return matcher.make(this, t, arguments);
            }
        }
        throw error(t, "invalid syntax");
    }

    private void label() {
        Token label = previous();
        consume("expected ':' after label name", Token.Type.COLON);

        String labelName = label.lexeme;

        if(label.lexeme.startsWith(SUBLABEL_INITIATOR) && this.currentParentLabel != null) //this label is a sublabel
            labelName = this.currentParentLabel + SUBLABEL_SEPARATOR + labelName;
        else if(!label.lexeme.startsWith(SUBLABEL_INITIATOR)) this.currentParentLabel = labelName;

        if(labels.containsKey(labelName))
            throw error(label, "Duplicate label '" + labelName + "'");
        labels.put(labelName, (short) (currentBlock.startAddress + currentBlock.codeSize));
    }

    private Argument[] arguments() {
        List<Argument> args = new ArrayList<>();
        while (!isAtEnd()) {
            args.add(argument());
            match(Token.Type.COMMA);
        }
        return args.toArray(new Argument[0]);
    }

    private Argument argument() {
        return dereference();
    }

    private Argument dereference() {
        if (match(Token.Type.L_SQUARE_BRACKET)) {
            Argument arg = longAddress();
            consume("expected closing ']'", Token.Type.R_SQUARE_BRACKET);
            return new Argument.Dereference(arg);
        }

        return longAddress();
    }

    private Argument longAddress() {
        Argument cs = register();
        if (match(Token.Type.COLON)) {
            return new Argument.LongAddress(cs, register());
        }
        return cs;
    }

    private Argument register() {
        Token t = peek();
        switch (t.type) {
            case RA:
            case RB:
            case RC:
            case RD:
            case SP:
            case SS:
                advance();
                return new Argument.Register(t);
        }
        return literal();
    }

    private Argument literal() {
        if (match(Token.Type.INTEGER)) {
            Token prev = previous();
            if(inByteRange((Integer) prev.literal))
                return new Argument.Byte(prev);
            else if(inShortRange((Integer) prev.literal)) return new Argument.Word(prev);
            throw error(prev, "constant value out of range");
        } else if (match(Token.Type.IDENTIFIER))
            return new Argument.LabelRef(previous());
        throw error(peek(), "unexpected token");
    }

    private void newBlock(Token orgToken, Token originToken) {
        short startAddress = ensureShort(originToken, (Integer) originToken.literal);
        if (currentBlock != null)
            blocks.add(currentBlock);
        currentBlock = new Block(orgToken, startAddress);
        currentParentLabel = null;
    }


    public static byte ensureByte(Token t, int i) {
        if (!inByteRange(i))
            throw error(t, "Argument out of range (expected 1-byte integer)");
        return (byte) i;
    }

    private static boolean inByteRange(int i) {
        return i >= -128 && i < 256;
    }


    public static short ensureShort(Token t, int i) {
        if (!inShortRange(i))
            throw error(t, "Argument out of range (expected 2-byte integer)");
        return (short) i;
    }

    private static boolean inShortRange(int i) {
        return i >= -32768 && i < 65536;
    }


    @Override
    public Set<Block> getResult() {
        return blocks;
    }


    public Map<String, Short> getLabels() {
        return labels;
    }

    public String getCurrentParentLabel() {
        return currentParentLabel;
    }
}
