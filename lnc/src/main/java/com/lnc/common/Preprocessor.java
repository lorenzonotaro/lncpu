package com.lnc.common;

import com.lnc.LNC;
import com.lnc.common.frontend.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Preprocessor {

    boolean needsReprocessing = false;
    private final List<List<Token>> lines;
    private final LexerConfig macroIncludeConfig;
    Map<String, List<Token>> defines = new HashMap<>();

    public Preprocessor(List<List<Token>> lines, LexerConfig macroIncludeConfig){
        this.lines = lines;
        this.macroIncludeConfig = macroIncludeConfig;

        defines.put("__VERSION__", List.of(Token.__internal(TokenType.STRING, LNC.PROGRAM_VERSION)));
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
                    try {
                        var path = resolvePath(fileName, line.get(1).location);
                        LineByLineLexer lexer = new LineByLineLexer(macroToken, macroIncludeConfig); //TODO: file locations aren't accurate? see immediate mode compilation
                        if(lexer.parse(Files.readString(path), path)){
                            iterator.remove();
                            addLines(new LinkedList<>(lexer.getResult()), iterator);
                        } else throw new CompileException("%include failed for file '" + fileName + "'", macroToken);
                    } catch (IOException e) {
                       throw new CompileException("unable to resolve file '" + fileName + "'", macroToken);
                    }
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if(macroToken.type.equals(TokenType.MACRO_IFDEF)){
                if(line.size() == 2){
                    String macroName = line.get(1).lexeme;
                    boolean keep = defines.containsKey(macroName);
                    iterator.remove();
                    consumeUntilEndif(line, iterator, keep);
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if(macroToken.type.equals(TokenType.MACRO_IFNDEF)){
                if(line.size() == 2){
                    String macroName = line.get(1).lexeme;
                    boolean keep = !defines.containsKey(macroName);
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

    private Path resolvePath(String fileName, Location includerLocation) throws IOException {
        // 1. Check if we can resolve the path starting from the current file's path
        Path resolvedPath = Path.of(includerLocation.filepath).toAbsolutePath().getParent().resolve(fileName);

        if(resolvedPath.toFile().exists()){
            return resolvedPath;
        }

        // 2. Check if we can resolve the path starting from the current working directory
        resolvedPath = Path.of(fileName);
        if(resolvedPath.toFile().exists()){
            return resolvedPath;
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
}
