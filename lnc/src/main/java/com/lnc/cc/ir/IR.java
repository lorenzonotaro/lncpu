package com.lnc.cc.ir;

import com.lnc.cc.common.FlatSymbolTable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents an Intermediate Representation (IR) consisting of a collection of IR units
 * and an associated global flat symbol table. Provides functionalities for accessing
 * these components and managing the relationship between symbol tables.
 */
public class IR {
    private final List<IRUnit> units;
    private final FlatSymbolTable symbolTable;

    public IR(List<IRUnit> units, FlatSymbolTable globalSymbolTable) {
        this.units = units;
        this.symbolTable = globalSymbolTable;

        for (var unit : units) {
            symbolTable.join(unit.getSymbolTable());
        }
    }

    protected IR(IR ir){
        this.units = ir.units;
        this.symbolTable = ir.symbolTable;
    }

    public List<IRUnit> units() {
        return units;
    }

    public FlatSymbolTable symbolTable() {
        return symbolTable;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (IR) obj;
        return this.units.equals(that.units) &&
                Objects.equals(this.symbolTable, that.symbolTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(units, symbolTable);
    }

}
