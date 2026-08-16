package com.lnc.cc.codegen;

import com.lnc.LNC;
import com.lnc.cc.ast.BinaryExpression;
import com.lnc.cc.ast.BlockStatement;
import com.lnc.cc.ast.FunctionDeclaration;
import com.lnc.cc.ast.Statement;
import com.lnc.cc.common.Scope;
import com.lnc.cc.ir.Bin;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.ir.Move;
import com.lnc.cc.ir.Ret;
import com.lnc.cc.ir.operands.ImmediateOperand;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.types.Declarator;
import com.lnc.cc.types.I8Type;
import com.lnc.cc.types.StorageQualifier;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;
import org.junit.Test;

import static org.junit.Assert.*;

public class GraphColoringRegisterAllocatorDfsTest {
    @Test
    public void dfsIsDeterministicAndNeverWorseThanGreedy() {
        GraphColoringRegisterAllocator.AllocationInfo greedy = allocate(pressureUnit(), "greedy", 16, 5000);
        GraphColoringRegisterAllocator.AllocationInfo first = allocate(pressureUnit(), "dfs", 16, 5000);
        GraphColoringRegisterAllocator.AllocationInfo second = allocate(pressureUnit(), "dfs", 16, 5000);

        assertTrue(first.terminalMetric().compareTo(greedy.terminalMetric()) <= 0);
        assertEquals(first.terminalMetric(), second.terminalMetric());
        assertEquals(first.terminalMetric().spillSequence(), second.terminalMetric().spillSequence());
        assertTrue(first.searchDiagnostics().statesExplored() > 1);
        assertFalse(first.searchDiagnostics().truncatedByDepth());
        assertFalse(first.searchDiagnostics().truncatedByStates());
    }

    @Test
    public void depthAndStateCapsRetainGreedyIncumbentAndReportTruncation() {
        GraphColoringRegisterAllocator.AllocationInfo greedy = allocate(pressureUnit(), "greedy", 16, 5000);
        GraphColoringRegisterAllocator.AllocationInfo depthCapped = allocate(pressureUnit(), "dfs", 0, 5000);
        GraphColoringRegisterAllocator.AllocationInfo stateCapped = allocate(pressureUnit(), "dfs", 16, 1);

        assertEquals(greedy.terminalMetric(), depthCapped.terminalMetric());
        assertEquals(greedy.terminalMetric(), stateCapped.terminalMetric());
        assertTrue(depthCapped.searchDiagnostics().truncatedByDepth());
        assertTrue(stateCapped.searchDiagnostics().truncatedByStates());
    }

    private static GraphColoringRegisterAllocator.AllocationInfo allocate(IRUnit unit,
                                                                           String strategy,
                                                                           int depth,
                                                                           int states) {
        Object oldStrategy = LNC.settings.set("--reg-alloc-strategy", strategy);
        Object oldDepth = LNC.settings.set("--reg-alloc-dfs-max-depth", (double) depth);
        Object oldStates = LNC.settings.set("--reg-alloc-dfs-max-states", (double) states);
        try {
            return GraphColoringRegisterAllocator.run(unit);
        } finally {
            LNC.settings.set("--reg-alloc-strategy", oldStrategy);
            LNC.settings.set("--reg-alloc-dfs-max-depth", oldDepth);
            LNC.settings.set("--reg-alloc-dfs-max-states", oldStates);
        }
    }

    private static IRUnit pressureUnit() {
        FunctionDeclaration declaration = new FunctionDeclaration(
                new Declarator(StorageQualifier.NONE, new I8Type()),
                Token.__internal(TokenType.IDENTIFIER, "dfs_pressure"),
                new com.lnc.cc.ast.VariableDeclaration[0],
                false,
                new BlockStatement(new Statement[0])
        );
        declaration.setScope(Scope.createRoot("dfs_pressure"));
        IRUnit unit = new IRUnit(declaration);
        unit.compileLocalMappings();

        VirtualRegister[] values = new VirtualRegister[6];
        for (int index = 0; index < values.length; index++) {
            values[index] = unit.getVrManager().getRegister(new I8Type());
            unit.emit(new Move(new ImmediateOperand(index + 1, new I8Type()), values[index]));
        }
        VirtualRegister left = unit.getVrManager().getRegister(new I8Type());
        VirtualRegister right = unit.getVrManager().getRegister(new I8Type());
        VirtualRegister merged = unit.getVrManager().getRegister(new I8Type());
        VirtualRegister result = unit.getVrManager().getRegister(new I8Type());
        unit.emit(new Bin(left, values[0], values[1], BinaryExpression.Operator.ADD));
        unit.emit(new Bin(right, values[2], values[3], BinaryExpression.Operator.ADD));
        unit.emit(new Bin(merged, left, right, BinaryExpression.Operator.ADD));
        unit.emit(new Bin(result, merged, values[4], BinaryExpression.Operator.ADD));
        unit.emit(new Bin(result, result, values[5], BinaryExpression.Operator.ADD));
        unit.emit(new Ret(result));
        return unit;
    }
}
