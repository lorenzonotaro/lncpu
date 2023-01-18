package com.lnasm.compiler;

import com.lnasm.LNASM;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Preprocessor {

    boolean needsReeprocessing = false;
    private final List<List<Token>> lines;
    Map<String, List<Token>> defines = new HashMap<>();

    Preprocessor(List<List<Token>> lines){
        this.lines = lines;
    }

    boolean preprocess() {
        boolean success = true;
        do {
            needsReeprocessing = false;
            for (ListIterator<List<Token>> iterator = lines.listIterator(); iterator.hasNext(); ) {
                try{
                    processLine(iterator);
                }catch(CompileException e){
                    iterator.remove();
                    e.log();
                    success = false;
                }
            }
        } while (needsReeprocessing);
        return success;
    }

    private void processLine(ListIterator<List<Token>> iterator) {
        List<Token> line = iterator.next();
        Token macroToken;
        if (!line.isEmpty()) {
            if ((macroToken = line.get(0)).type.equals(Token.Type.MACRO_DEFINE)) {
                if(line.size() >= 2){
                    String macroName = line.get(1).lexeme;
                    if (defines.containsKey(macroName))
                        throw new CompileException("duplicate macro name '" + macroName + "'", macroToken);
                    defines.put(macroName, line.subList(2, line.size()));
                    iterator.remove();
                    needsReeprocessing = true;
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
                        List<Line> lines = LNASM.getLinesFromFile(Path.of(fileName).toRealPath().toString());
                        Lexer lexer = new Lexer(macroToken);
                        if(lexer.parse(lines)){
                            iterator.set(lexer.getLines().stream().flatMap(List::stream).collect(Collectors.toList()));
                        } else throw new CompileException("%include filed for file '" + fileName + "'", macroToken);
                    } catch (IOException e) {
                       throw new CompileException("unable to resolve file '" + fileName + "'", macroToken);
                    }
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else {
                iterator.set(line.stream().flatMap(l -> {
                    List<Token> macroValue = null;
                    boolean isMacroIdent = l.type == Token.Type.IDENTIFIER && (macroValue = defines.get(l.lexeme)) != null;
                    if (isMacroIdent) {
                        needsReeprocessing = true;
                        return macroValue.stream().map(t -> new Token(t, l));
                    } else return Stream.of(l);
                }).collect(Collectors.toList()));
            }
        }
    }

    public List<Token[]> getLines() {
        return lines.stream().map(l -> l.toArray(new Token[0])).collect(Collectors.toList());
    }
}
