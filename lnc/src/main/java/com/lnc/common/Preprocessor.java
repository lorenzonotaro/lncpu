package com.lnc.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.lnc.LNC;
import com.lnc.assembler.linker.LinkerConfig;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.LexerConfig;
import com.lnc.common.frontend.LineByLineLexer;
import com.lnc.common.frontend.FullSourceLexer;
import com.lnc.common.frontend.Location;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

public class Preprocessor {

    private enum Mode { LINE_BASED, FULL_SOURCE }

    boolean needsReprocessing = false;
    private final List<List<Token>> lines;
    private final LexerConfig macroIncludeConfig;
    Map<String, List<Token>> defines = new HashMap<>();
    private final LinkerConfig linkerConfig;
    private final Mode mode;
    
    private Preprocessor(List<List<Token>> lines, LexerConfig macroIncludeConfig, LinkerConfig linkerConfig) {
        this.lines = lines;
        this.macroIncludeConfig = macroIncludeConfig;
        this.linkerConfig = linkerConfig;
        this.mode = Mode.LINE_BASED;

        defines.put("__VERSION__", List.of(Token.__internal(TokenType.STRING, LNC.PROGRAM_VERSION)));
    }

    public static Preprocessor lnasm(List<List<Token>> lines, LexerConfig macroIncludeConfig, LinkerConfig linkerConfig) {
        return new Preprocessor(lines, macroIncludeConfig, linkerConfig);
    }

    public static Preprocessor lnc(List<Token> tokens, LexerConfig macroIncludeConfig) {
        return new Preprocessor(groupByLine(tokens), macroIncludeConfig, null);
    }

    private static List<List<Token>> groupByLine(List<Token> tokens) {
        List<List<Token>> grouped = new ArrayList<>();
        List<Token> current = null;
        int currentLine = Integer.MIN_VALUE;
        for (Token t : tokens) {
            int ln = t.location.lineNumber;
            if (current == null || ln != currentLine) {
                current = new ArrayList<>();
                grouped.add(current);
                currentLine = ln;
            }
            current.add(t);
        }
        return grouped;
    }

    public boolean preprocess() {
        boolean success = true;
        for (ListIterator<List<Token>> iterator = lines.listIterator(); iterator.hasNext(); ) {
            try{
                processLine(iterator);
            }catch(CompileException e){
                iterator.remove();
                e.log();
                success = false;
            }
        }
        return success;
    }

    private void processLine(ListIterator<List<Token>> iterator) {
        List<Token> line = iterator.next();
        Token macroToken;
        if (!line.isEmpty()) {
            macroToken = line.get(0);
            if (macroToken.type.equals(TokenType.MACRO_DEFINE)) {
                if(line.size() >= 2){
                    String macroName = line.get(1).lexeme;
                    if (defines.containsKey(macroName))
                        throw new CompileException("duplicate macro name '" + macroName + "'", macroToken);
                    defines.put(macroName, line.subList(2, line.size()));
                    iterator.remove();
                    needsReprocessing = true;
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if (macroToken.type.equals(TokenType.MACRO_UNDEFINE)) {
                if(line.size() == 2) {
                    String macroName = line.get(1).lexeme;
                    if (!defines.containsKey(macroName)) {
                        throw new CompileException("undefined macro" + macroName + "'" + macroName + "'", macroToken);
                    }
                    defines.remove(macroName);
                    iterator.remove();
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if (macroToken.type.equals(TokenType.MACRO_INCLUDE)){
                if(line.size() == 2){
                    String fileName = (String) line.get(1).literal;

                    if(!fileName.endsWith(".lnasm")){
                        fileName += ".lnasm";
                    }

                    try {
                        var path = resolvePath(fileName, line.get(1).location, false);
                        List<List<Token>> inc = lexInclude(path, macroToken);
                        iterator.remove();
                        addLines(new LinkedList<>(inc), iterator);
                    } catch (IOException e) {
                       throw new CompileException("unable to resolve file '" + fileName + "'", macroToken);
                    }
                }else if(line.size() == 4 && line.get(1).type == TokenType.LESS_THAN && line.get(2).type == TokenType.IDENTIFIER && line.get(3).type == TokenType.GREATER_THAN){
                    String fileName = line.get(2).lexeme;

                    if(!fileName.endsWith(".lnasm")){
                        fileName += ".lnasm";
                    }

                    try {
                        var path = resolvePath(fileName, line.get(2).location, true);
                        List<List<Token>> inc = lexInclude(path, macroToken);
                        iterator.remove();
                        addLines(new LinkedList<>(inc), iterator);
                    } catch (IOException e) {
                        throw new CompileException("unable to resolve file '" + fileName + "'", macroToken);
                    }

                }else {throw new CompileException("invalid macro syntax", macroToken); }
            } else if(macroToken.type.equals(TokenType.MACRO_IFDEF)){
                Token secondToken;
                if(line.size() == 2){
                    String macroName = line.get(1).lexeme;
                    boolean keep = defines.containsKey(macroName);
                    iterator.remove();
                    consumeUntilEndif(line, iterator, keep);
                }else if(line.size() == 3 && (secondToken = line.get(1)).type == TokenType.IDENTIFIER && secondToken.lexeme.equals("SECTION") && linkerConfig != null){
                    String sectionName = line.get(2).lexeme;
                    boolean keep = linkerConfig.hasSection(sectionName);
                    iterator.remove();
                    consumeUntilEndif(line, iterator, keep);
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if(macroToken.type.equals(TokenType.MACRO_IFNDEF)){
                Token secondToken;
                if(line.size() == 2){
                    String macroName = line.get(1).lexeme;
                    boolean keep = !defines.containsKey(macroName);
                    iterator.remove();
                    consumeUntilEndif(line, iterator, keep);
                }else if(line.size() == 3 && (secondToken = line.get(1)).type == TokenType.IDENTIFIER && secondToken.lexeme.equals("SECTION") && linkerConfig != null){
                    String sectionName = line.get(2).lexeme;
                    boolean keep = !linkerConfig.hasSection(sectionName);
                    iterator.remove();
                    consumeUntilEndif(line, iterator, keep);
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if(macroToken.type.equals(TokenType.MACRO_ENDIF)){
                throw new CompileException("unexpected %endif", macroToken);
            }else if(macroToken.type.equals(TokenType.MACRO_ERROR)){
                if(line.size() == 2 && line.get(1).type == TokenType.STRING){
                    throw new CompileException(line.get(1).literal.toString(), macroToken);
                }else throw new CompileException("invalid macro syntax", macroToken);
            }else {
                iterator.set(line.stream().flatMap(l -> {
                    List<Token> macroValue = null;
                    boolean isMacroIdent = l.type == TokenType.IDENTIFIER && (macroValue = defines.get(l.lexeme)) != null;
                    if (isMacroIdent) {
                        needsReprocessing = true;
                        return macroValue.stream().map(t -> new Token(t, l));
                    } else return Stream.of(l);
                }).collect(Collectors.toList()));
            }
        }
    }

    private Path resolvePath(String fileName, Location includerLocation, boolean includeDirsOnly) throws IOException {
        // 1. Check if we can resolve the path starting from the current file's path
        Path resolvedPath;
        if(!includeDirsOnly){
            resolvedPath = Path.of(includerLocation.filepath).toAbsolutePath().getParent().resolve(fileName);

            if(resolvedPath.toFile().exists()){
                return resolvedPath;
            }

            // 2. Check if we can resolve the path starting from the current working directory
            resolvedPath = Path.of(fileName);
            if(resolvedPath.toFile().exists()){
                return resolvedPath;
            }
        }

        // 3. Check if we can resolve the path starting from the include directories
        for(String includeDir : LNC.includeDirs){
            resolvedPath = Path.of(includeDir).resolve(fileName);
            if(resolvedPath.toFile().exists()){
                return resolvedPath;
            }
        }

        // 4. If we can't resolve the path, throw an exception
        throw new IOException("file not found");
    }

    private List<List<Token>> lexInclude(Path path, Token macroToken) throws IOException {
        String content = Files.readString(path);
        if (mode == Mode.LINE_BASED) {
            LineByLineLexer lexer = new LineByLineLexer(macroToken, macroIncludeConfig); //TODO: file locations aren't accurate? see immediate mode compilation
            if (lexer.parse(content, path)) {
                return lexer.getResult();
            } else {
                throw new CompileException("%include failed for file '" + path.getFileName() + "'", macroToken);
            }
        } else {
            FullSourceLexer lexer = new FullSourceLexer(macroToken, macroIncludeConfig);
            if (lexer.parse(content, path)) {
                return groupByLine(lexer.getResult());
            } else {
                throw new CompileException("%include failed for file '" + path.getFileName() + "'", macroToken);
            }
        }
    }

    private void consumeUntilEndif(List<Token> openingLine, ListIterator<List<Token>> iterator, boolean keep) {
        int ifDepth = 1;
        LinkedList<List<Token>> lines = new LinkedList<>();
        boolean closed = false;
        List<Token> line =  openingLine;
        while(iterator.hasNext()){
            line = iterator.next();
            if(line.isEmpty())
                continue;
            Token macroToken = line.get(0);
            iterator.remove();
            if(macroToken.type.equals(TokenType.MACRO_ENDIF) && ifDepth == 1){
                closed = true;
                break;
            }else{
                if(macroToken.type.equals(TokenType.MACRO_IFDEF) || macroToken.type.equals(TokenType.MACRO_IFNDEF)){
                    ifDepth++;
                }else if(macroToken.type.equals(TokenType.MACRO_ENDIF)){
                    ifDepth--;
                }
                lines.add(line);
            }
        }
        if(!closed){
            iterator.add(Collections.emptyList());
            iterator.previous();
            throw new CompileException("missing %endif", openingLine.get(0));
        }

        if(keep){
            for (Iterator<List<Token>> it = lines.descendingIterator(); it.hasNext(); ) {
                List<Token> _line = it.next();
                iterator.add(_line);
                iterator.previous();
            }
        }
    }

    private void addLines(LinkedList<List<Token>> lines, ListIterator<List<Token>> iterator) {
        for (Iterator<List<Token>> iter = lines.descendingIterator(); iter.hasNext(); ) {
            List<Token> line = iter.next();
            iterator.add(line);
            iterator.previous();
        }
    }

    public List<Token[]> getLines() {
        return lines.stream().map(l -> l.toArray(new Token[0])).collect(Collectors.toList());
    }

    public List<Token> getTokens() {
        return lines.stream().flatMap(List::stream).collect(Collectors.toList());
    }
}
