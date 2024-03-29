package com.lnasm.compiler;

import com.lnasm.LNASM;

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
                        List<Line> lines = LNASM.getLinesFromFile(Path.of(fileName).toRealPath().toString());
                        Lexer lexer = new Lexer(macroToken);
                        if(lexer.parse(lines)){
                            iterator.remove();
                            for(List<Token> _line : lexer.getLines()){
                                iterator.add(_line);
                            }
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
                    consumeUntilEndif(iterator, keep);
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if(macroToken.type.equals(Token.Type.MACRO_IFNDEF)){
                if(line.size() == 2){
                    String macroName = line.get(1).lexeme;
                    boolean keep = !defines.containsKey(macroName);
                    iterator.remove();
                    consumeUntilEndif(iterator, keep);
                }else throw new CompileException("invalid macro syntax", macroToken);
            } else if(macroToken.type.equals(Token.Type.MACRO_ENDIF)){
                throw new CompileException("unexpected %endif", macroToken);
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

    private void consumeUntilEndif(ListIterator<List<Token>> iterator, boolean keep){
        LinkedList<List<Token>> lines = new LinkedList<>();
        boolean closed = false;
        while(iterator.hasNext()){
            List<Token> line = iterator.next();
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
            throw new CompileException("missing %endif", lines.get(lines.size() - 1).get(0));
        }

        if(keep){
            for (Iterator<List<Token>> it = lines.descendingIterator(); it.hasNext(); ) {
                List<Token> line = it.next();
                iterator.add(line);
                iterator.previous();
            }
        }
    }

    public List<Token[]> getLines() {
        return lines.stream().map(l -> l.toArray(new Token[0])).collect(Collectors.toList());
    }
}
