package com.lnc.cc.common;

import com.lnc.common.frontend.Token;

import java.util.Map;

public class FlatSymbolTable {
    private final Map<String, Symbol> symbols;
    private final String name;

    private FlatSymbolTable(String name) {
        this.name = name;
        this.symbols = new java.util.HashMap<>();
    }

    public static FlatSymbolTable flatten(Scope scope) {
        if(!scope.isRoot()){
            throw new IllegalArgumentException("scope must be root");
        }

        FlatSymbolTable table = new FlatSymbolTable(scope.getRootName());

        flattenRecursively(scope, table);

        return table;
    }

    private static void flattenRecursively(Scope scope, FlatSymbolTable table) {
        for (Symbol symbol : scope.getSymbols().values()) {
            var prev = table.symbols.put("__" + scope.getId() + "__" + symbol.getName(), symbol);
            if(prev != null){
                throw new IllegalStateException("unequivocal symbol name '%s', from scopes '%s' and '%s'"
                        .formatted(symbol.getName(), prev.getScope().getId(), symbol.getScope().getId()));
            }
            symbol.setFlatSymbolName("__" + scope.getId() + "__" + symbol.getName());
        }

        for (Scope child : scope.getChildren()) {
            flattenRecursively(child, table);
        }
    }

    public void visit(){

        System.out.printf("Symbol table '%s'%n", name);

        for (Map.Entry<String, Symbol> entry : symbols.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Symbol> entry : symbols.entrySet()) {
            sb.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
        }

        return sb.toString();
    }

    public Symbol resolveSymbol(Scope scope, String symbolName) {
        Scope sc = scope;

        while(sc != null){
            Symbol symbol = symbols.get("__" + sc.getId() + "__" + symbolName);
            if(symbol != null){
                return symbol;
            }
            sc = sc.getParent();
        }

        return null;
    }
}
