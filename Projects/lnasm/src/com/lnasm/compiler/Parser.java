package com.lnasm.compiler;

import com.lnasm.compiler.ast.Argument;
import com.lnasm.compiler.ast.Matcher;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class Parser {
    private final Set<Block> blocks;
    private List<Token[]> lines;
    private Token[] currentLine;
    private int index;

    private Block currentBlock;

    private final Map<String, Short> labels;

    Parser() {
        blocks = new HashSet<>();
        labels = new HashMap<>();
    }

    boolean parse(List<Token[]> tokens) {
        reset(tokens);

        boolean success = true;

        for (Token[] line : lines) {
            if (line.length == 0) continue;
            try {
                currentLine = line;
                index = 0;
                Encodeable instr;
                if ((instr = parseLine()) != null) {
                    if (!currentBlock.addInstruction(instr)) {
                        throw new CompileException("exceeded maximum code size (65536)", previous());
                    }
                }
            } catch (CompileException e) {
                e.log();
                success = false;
            }
        }
        blocks.add(currentBlock);

        return success;
    }

    private Encodeable parseLine() {
        if (match(Token.Type.DIR_ORG)) {
            newBlock(consume("expected number", Token.Type.INTEGER));
            return null;
        } else if (currentBlock == null)
            throw new CompileException("initial code segment not specified", peek());
        else if (match(Token.Type.DIR_DATA)) {
            byte[] bytes = null;
            while (!isAtEnd()) {
                Token t = consume("expected encodeable constant value(s) (strings, bytes)", Token.Type.STRING, Token.Type.INTEGER);
                switch (t.type) {
                    case STRING:
                        bytes = ((String) t.literal).getBytes(StandardCharsets.US_ASCII);
                        break;
                    case INTEGER:
                        byte bVal = ensureByte(t, (Integer) t.literal);
                        bytes = new byte[]{bVal};
                }
            }
            return new EncodedData(bytes);
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
                return matcher.make(arguments);
            }
        }
        throw new CompileException("invalid syntax", t);
    }

    private void label() {
        Token label = previous();
        consume("expected ':' after label name", Token.Type.COLON);

        if(labels.containsKey(label.lexeme))
            throw new CompileException("Duplicate label '" + label.lexeme + "'", label);
        labels.put(label.lexeme, (short) (currentBlock.startAddress + currentBlock.codeSize));
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
            throw new CompileException("constant value out of range", prev);
        } else if (match(Token.Type.IDENTIFIER))
            return new Argument.LabelRef(previous());
        throw new CompileException("unexpected token", peek());
    }
    private void reset(List<Token[]> lines) {
        this.lines = lines;
        this.index = 0;
        blocks.clear();
    }

    private void newBlock(Token t) {
        short startAddress = ensureShort(t, (Integer) t.literal);
        if (currentBlock != null)
            blocks.add(currentBlock);
        currentBlock = new Block(previous(), startAddress);
    }

    private boolean isAtEnd() {
        return index >= currentLine.length;
    }

    private boolean match(Token.Type type) {
        if (!isAtEnd() && peek().type == type) {
            advance();
            return true;
        }
        return false;
    }


    private Token consume(String errorMsg, Token.Type... types) {
        if (check(types)) return advance();
        else throw error(peek(), errorMsg);
    }

    private CompileException error(Token token, String errorMsg) {
        return new CompileException(errorMsg, token);
    }

    private boolean check(Token.Type... types) {
        if (isAtEnd()) return false;
        Token.Type peekType = peek().type;
        return Arrays.stream(types).anyMatch(t -> t == peekType);
    }

    private Token advance() {
        if (isAtEnd())
            throw error(previous(), "unexpected end of line");
        return currentLine[index++];
    }

    private Token previous() {
        return currentLine[index - 1];
    }

    private Token peek() {
        if (isAtEnd()) {
            throw error(previous(), "unexpected end of line");
        }
        return currentLine[index];
    }

    public static byte ensureByte(Token t, int i) {
        if (!inByteRange(i))
            throw new CompileException("Argument out of range (expected 1-byte integer)", t);
        return (byte) i;
    }

    private static boolean inByteRange(int i) {
        return i >= -128 && i < 256;
    }


    public static short ensureShort(Token t, int i) {
        if (!inShortRange(i))
            throw new CompileException("Argument out of range (expected 2-byte integer)", t);
        return (short) i;
    }

    private static boolean inShortRange(int i) {
        return i >= -32768 && i < 65536;
    }


    public Set<Block> getBlocks() {
        return blocks;
    }


    public Map<String, Short> getLabels() {
        return labels;
    }
}
