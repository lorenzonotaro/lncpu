package com.lnc.cc.common;

import java.util.Map;

public class FlatSymbolTable {
    private final Map<String, BaseSymbol> symbols;

    private final Map<Object, ConstantSymbol> constants;

    private final String name;

    private FlatSymbolTable(String name) {
        this.name = name;
        this.symbols = new java.util.HashMap<>();
        this.constants = new java.util.HashMap<>();
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
        for (BaseSymbol symbol : scope.getSymbols().values()) {

            var prev = table.symbols.put(scope.getScopePrefix() + symbol.getName(), symbol);
            if(prev != null){
                throw new IllegalStateException("unequivocal symbol name '%s', from scopes '%s' and '%s'"
                        .formatted(symbol.getName(), prev.getScope().getId(), symbol.getScope().getId()));
            }

            if(!symbol.isParameter())
                symbol.setFlatSymbolName(scope.getScopePrefix() + symbol.getName());
        }

        int constantCounter = 0;
        for (Map.Entry<Object, BaseSymbol> entry : scope.getConstants().entrySet()) {
            var constant = (ConstantSymbol) entry.getValue();

            if(!table.constants.containsKey(entry.getKey())) {
                table.constants.put(entry.getKey(), constant);
            }
            constant.setFlatSymbolName(scope.getScopePrefix() + "const_" + constantCounter++);
        }

        for (Scope child : scope.getChildren()) {
            flattenRecursively(child, table);
        }
    }

    public void visit(){

        System.out.printf("Symbol table '%s'%n", name);

        for (BaseSymbol entry : symbols.values()) {
            System.out.println(entry);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, BaseSymbol> entry : symbols.entrySet()) {
            sb.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
        }

        return sb.toString();
    }

    public BaseSymbol resolveSymbol(Scope scope, String symbolName) {
        Scope sc = scope;

        while(sc != null){
            BaseSymbol symbol = symbols.get(sc.getScopePrefix() + symbolName);
            if(symbol != null){
                return symbol;
            }
            sc = sc.getParent();
        }

        return null;
    }

    public BaseSymbol resolveConstant(Object object) {

        BaseSymbol symbol = constants.get(object);
        if(symbol == null){
            throw new IllegalStateException("Constant '%s' not found".formatted(object));
        }
        return symbol;
    }

    public void join(FlatSymbolTable symbolTable) {

        for (Map.Entry<String, BaseSymbol> entry : symbolTable.symbols.entrySet()) {
            var prev = symbols.put(entry.getKey(), entry.getValue());
            if(prev != null && !prev.getScope().equals(entry.getValue().getScope())){
                throw new IllegalStateException("unequivocal symbol name '%s', from scopes '%s' and '%s'"
                        .formatted(entry.getValue().getName(), prev.getScope().getId(), entry.getValue().getScope().getId()));
            }
        }
    }

    public Map<String, BaseSymbol> getSymbols() {
        return symbols;
    }

    public Map<Object, ConstantSymbol> getConstants() {
        return constants;
    }
}
