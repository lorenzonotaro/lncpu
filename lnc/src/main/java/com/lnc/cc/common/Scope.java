package com.lnc.cc.common;


import java.util.*;

public class Scope {
    private final Scope parent;

    private final int depth;

    private final List<Scope> children = new ArrayList<>();

    private final Map<String, Symbol> symbols = new HashMap<>();

    private final List<Symbol> parameters = new ArrayList<>();

    private final Map<Integer, Integer> childrenAtDepth = new HashMap<>();


    private final String rootName, id;

    private Scope(String rootName){
        this.parent = null;
        this.depth = 0;
        this.rootName = rootName;
        this.id = rootName;
    }

    public Scope(Scope parent, int depth, String childId) {
        this.parent = parent;
        this.depth = depth;
        this.rootName = parent.rootName;
        this.id = rootName + (depth + childId);
    }

    public Scope getRoot(){
        if(parent == null){
            return this;
        }
        return parent.getRoot();
    }

    public boolean isRoot(){
        return parent == null;
    }

    public Scope getParent(){
        return parent;
    }

    public void define(Symbol symbol, boolean isParameter){
        Symbol existing = symbols.get(symbol.getName());

        symbol.setScope(this);

        if(existing != null && !existing.isForward() && !symbol.isForward()){
            throw new RuntimeException("symbol '%s' already defined here: '%s'".formatted(existing.getName(), existing.getToken().formatLocation()));
        }

        if (isParameter) {
            parameters.add(symbol);
        }

        symbols.put(symbol.getName(), symbol);
    }


    public Symbol resolve(String name){
        Symbol symbol = symbols.get(name);
        if(symbol != null){
            return symbol;
        }
        if(parent != null){
            return parent.resolve(name);
        }
        return null;
    }

    public static Scope createRoot(String rootName){
        return new Scope(rootName);
    }

    public Scope createChild(){
        int childrenAtDepth = this.childrenAtDepth.getOrDefault(depth + 1, 0);
        Scope child = new Scope(this, depth + 1, genChildIdForDepth(childrenAtDepth));
        this.childrenAtDepth.put(depth + 1, childrenAtDepth + 1);
        children.add(child);
        return child;
    }

    private static String genChildIdForDepth(int childrenAtDepth) {
        // generates an alphanumerical id for the child scope (a, b, c, ... z, aa, ab, ac, ... az, ba, bb, ...)
        StringBuilder sb = new StringBuilder();
        do {
            sb.append((char)('a' + (childrenAtDepth % 26)));
            childrenAtDepth /= 26;
        }while(childrenAtDepth > 0);
        return sb.reverse().toString();
    }

    public String getId() {
        return id;
    }

    public void visit(int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print("  ");
        }
        System.out.println("Scope: " + id);

        for (Symbol symbol : symbols.values()) {
            for (int i = 0; i < indent; i++) {
                System.out.print("  ");
            }
            System.out.println("  Symbol: " + symbol.getName() + " [" + symbol.getType() + "]");
        }

        for (Scope child : children) {
            child.visit(indent + 1);
        }
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Scope scope = (Scope) obj;
        return id.equals(scope.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public Map<String, Symbol> getSymbols() {
        return symbols;
    }

    public List<Scope> getChildren() {
        return children;
    }

    public String getRootName() {
        return rootName;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public String getScopePrefix() {
        return id.isEmpty() ? "" : "__" + id + "__";
    }
}
