package com.lnasm.compiler;

import com.lnasm.LNASM;
import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.lexer.Lexer;
import com.lnasm.compiler.common.Line;
import com.lnasm.compiler.common.Location;
import com.lnasm.compiler.common.Token;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Preprocessor {

    boolean needsReprocessing = false;
    private final List<List<Token>> lines;
    Map<String, List<Token>> defines = new HashMap<>();

    Preprocessor(List<List<Token>> lines){
        this.lines = lines;
    }

    boolean preprocess() {
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
            if (macroToken.type.equals(Token.Type.MACRO_DEFINE)) {
                if(line.size() >= 2){
                    String macroName = line.get(1).lexeme;
                    if (defines.containsKey(macroName))
                        throw new CompileException("duplicate macro name '" + macroName + "'", macroToken);
                    defines.put(macroName, line.subList(2, line.size()));
                    iterator.remove();
                    needsReprocessing = true;
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if (macroToken.type.equals(Token.Type.MACRO_UNDEFINE)) {
                if(line.size() == 2) {
                    String macroName = line.get(1).lexeme;
                    if (!defines.containsKey(macroName)) {
                        throw new CompileException("undefined macro" + macroName + "'" + macroName + "'", macroToken);
                    }
                    defines.remove(macroName);
                    iterator.remove();
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if (macroToken.type.equals(Token.Type.MACRO_INCLUDE)){
                if(line.size() == 2){
                    String fileName = (String) line.get(1).literal;
                    try {
                        List<Line> lines = LNASM.getLinesFromFile(resolvePath(fileName, line.get(1).location));
                        Lexer lexer = new Lexer(); //TODO: file locations aren't accurate? see immediate mode compilation
                        if(lexer.parse(lines)){
                            iterator.remove();
                            addLines(new LinkedList<>(lexer.getLines()), iterator);
                        } else throw new CompileException("%include failed for file '" + fileName + "'", macroToken);
                    } catch (IOException e) {
                       throw new CompileException("unable to resolve file '" + fileName + "'", macroToken);
                    }
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if(macroToken.type.equals(Token.Type.MACRO_IFDEF)){
                if(line.size() == 2){
                    String macroName = line.get(1).lexeme;
                    boolean keep = defines.containsKey(macroName);
                    iterator.remove();
                    consumeUntilEndif(line, iterator, keep);
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if(macroToken.type.equals(Token.Type.MACRO_IFNDEF)){
                if(line.size() == 2){
                    String macroName = line.get(1).lexeme;
                    boolean keep = !defines.containsKey(macroName);
                    iterator.remove();
                    consumeUntilEndif(line, iterator, keep);
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if(macroToken.type.equals(Token.Type.MACRO_ENDIF)){
                throw new CompileException("unexpected %endif", macroToken);
            }else if(macroToken.type.equals(Token.Type.MACRO_ERROR)){
                if(line.size() == 2 && line.get(1).type == Token.Type.STRING){
                    throw new CompileException(line.get(1).literal.toString(), macroToken);
                }else throw new CompileException("invalid macro syntax", macroToken);
            }else {
                iterator.set(line.stream().flatMap(l -> {
                    List<Token> macroValue = null;
                    boolean isMacroIdent = l.type == Token.Type.IDENTIFIER && (macroValue = defines.get(l.lexeme)) != null;
                    if (isMacroIdent) {
                        needsReprocessing = true;
                        return macroValue.stream().map(t -> new Token(t, l));
                    } else return Stream.of(l);
                }).collect(Collectors.toList()));
            }
        }
    }

    private String resolvePath(String fileName, Location includerLocation) throws IOException {
        // 1. Check if we can resolve the path starting from the current file's path
        Path resolvedPath = Path.of(includerLocation.filepath).toAbsolutePath().getParent().resolve(fileName);

        if(resolvedPath.toFile().exists()){
            return resolvedPath.toString();
        }

        // 2. Check if we can resolve the path starting from the current working directory
        resolvedPath = Path.of(fileName);
        if(resolvedPath.toFile().exists()){
            return resolvedPath.toString();
        }

        // 3. Check if we can resolve the path starting from the include directories
        for(String includeDir : LNASM.includeDirs){
            resolvedPath = Path.of(includeDir).resolve(fileName);
            if(resolvedPath.toFile().exists()){
                return resolvedPath.toString();
            }
        }

        // 4. If we can't resolve the path, throw an exception
        throw new IOException("file not found");
    }

    private void consumeUntilEndif(List<Token> openingLine, ListIterator<List<Token>> iterator, boolean keep){
        LinkedList<List<Token>> lines = new LinkedList<>();
        boolean closed = false;
        List<Token> line =  openingLine;
        while(iterator.hasNext()){
            line = iterator.next();
            if(line.isEmpty())
                continue;
            Token macroToken = line.get(0);
            iterator.remove();
            if(macroToken.type.equals(Token.Type.MACRO_ENDIF)){
                closed = true;
                break;
            }else{
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
