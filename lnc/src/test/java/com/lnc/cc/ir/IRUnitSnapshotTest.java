package com.lnc.cc.ir;

import com.lnc.cc.ast.BlockStatement;
import com.lnc.cc.ast.FunctionDeclaration;
import com.lnc.cc.ast.Statement;
import com.lnc.cc.codegen.Register;
import com.lnc.cc.common.Scope;
import com.lnc.cc.ir.operands.DerefLocation;
import com.lnc.cc.ir.operands.ImmediateOperand;
import com.lnc.cc.ir.operands.SizedCast;
import com.lnc.cc.ir.operands.StackFrameLocation;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.types.Declarator;
import com.lnc.cc.types.I8Type;
import com.lnc.cc.types.PointerType;
import com.lnc.cc.types.StorageLocation;
import com.lnc.cc.types.StorageQualifier;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class IRUnitSnapshotTest {
    @Test
    public void snapshotRemapsVirtualsBranchesAndNestedOperands() {
        IRUnit original = unit();
        VirtualRegister address = original.getVrManager().getRegister(new PointerType(new I8Type(), false, StorageLocation.NEAR));
        VirtualRegister result = original.getVrManager().getRegister(new I8Type());
        IRBlock thenBlock = original.newBlock();
        IRBlock elseBlock = original.newBlock();
        IRBlock continuation = original.newBlock();

        SizedCast nested = new SizedCast(new DerefLocation(address, new I8Type()), new I8Type());
        original.emit(new Move(nested, result));
        original.emit(new CondJump(
                CondJump.Cond.NE,
                result,
                new ImmediateOperand(0, new I8Type()),
                thenBlock,
                elseBlock,
                continuation
        ));
        thenBlock.emit(new Goto(continuation));
        elseBlock.emit(new Goto(continuation));
        continuation.emit(new Ret(result));

        IRUnit snapshot = original.snapshotForRegisterAllocation();
        Move copiedMove = (Move) snapshot.getEntryBlock().getFirst();
        CondJump copiedJump = (CondJump) copiedMove.getNext();
        VirtualRegister copiedAddress = (VirtualRegister) ((DerefLocation) ((SizedCast) copiedMove.getSource()).getOperand()).getTarget();
        VirtualRegister copiedResult = (VirtualRegister) copiedMove.getDest();

        assertNotSame(original.getEntryBlock(), snapshot.getEntryBlock());
        assertNotSame(address, copiedAddress);
        assertNotSame(result, copiedResult);
        assertEquals(address.getRegisterNumber(), copiedAddress.getRegisterNumber());
        assertNotSame(thenBlock, copiedJump.getTarget());
        assertNotSame(elseBlock, copiedJump.getFalseTarget());
        assertNotSame(continuation, copiedJump.getContinueTo());
        assertSame(snapshot, copiedJump.getTarget().getUnit());
    }

    @Test
    public void snapshotMutationDoesNotAffectOriginalAllocationState() {
        IRUnit original = unit();
        VirtualRegister value = original.getVrManager().getRegister(new I8Type());
        StackFrameLocation slot = new StackFrameLocation(new I8Type(), StackFrameLocation.OperandType.LOCAL, 3);
        original.emit(new Move(value, slot));
        original.emit(new Ret(null));
        original.setSpillSpaceSize(2);
        original.setUsedRegisters(Set.of(Register.RA));

        IRUnit snapshot = original.snapshotForRegisterAllocation();
        Move copiedMove = (Move) snapshot.getEntryBlock().getFirst();
        VirtualRegister copiedValue = (VirtualRegister) copiedMove.getSource();
        StackFrameLocation copiedSlot = (StackFrameLocation) copiedMove.getDest();
        copiedValue.setAssignedPhysicalRegister(Register.RB);
        copiedSlot.setOffset(9);
        snapshot.setSpillSpaceSize(7);
        snapshot.setUsedRegisters(Set.of(Register.RC));

        assertNull(value.getAssignedPhysicalRegister());
        assertEquals(3, slot.getOffset());
        assertEquals(2, original.getSpillSpaceSize());
        assertEquals(Set.of(Register.RA), original.getUsedRegisters());
    }

    @Test
    public void snapshotRemapsDisconnectedCurrentAndLoopBlocks() {
        IRUnit original = unit();
        IRBlock current = original.newBlock();
        IRBlock continueTarget = original.newBlock();
        IRBlock breakTarget = original.newBlock();
        original.setCurrentBlock(current);
        original.enterLoop(new LoopInfo(continueTarget, breakTarget));

        IRUnit snapshot = original.snapshotForRegisterAllocation();

        assertNotNull(snapshot.getCurrentBlock());
        assertEquals(current.getId(), snapshot.getCurrentBlock().getId());
        assertNotSame(current, snapshot.getCurrentBlock());
        assertNotSame(continueTarget, snapshot.getCurrentLoopInfo().continueTarget());
        assertNotSame(breakTarget, snapshot.getCurrentLoopInfo().breakTarget());
        assertSame(snapshot, snapshot.getCurrentLoopInfo().continueTarget().getUnit());
        assertSame(snapshot, snapshot.getCurrentLoopInfo().breakTarget().getUnit());

        original.commitRegisterAllocationSnapshot(snapshot);

        assertSame(original, original.getCurrentBlock().getUnit());
        assertSame(original, original.getCurrentLoopInfo().continueTarget().getUnit());
        assertSame(original, original.getCurrentLoopInfo().breakTarget().getUnit());
    }

    @Test
    public void committedSnapshotRehomesWinningGraphToOriginalUnit() {
        IRUnit original = unit();
        VirtualRegister value = original.getVrManager().getRegister(new I8Type());
        original.emit(new Ret(value));
        IRUnit winner = original.snapshotForRegisterAllocation();
        VirtualRegister winningValue = (VirtualRegister) ((Ret) winner.getEntryBlock().getFirst()).getValue();
        winningValue.setAssignedPhysicalRegister(Register.RD);

        original.commitRegisterAllocationSnapshot(winner);

        assertSame(original, original.getEntryBlock().getUnit());
        assertSame(original, original.getFunctionDeclaration().unit);
        VirtualRegister committedValue = (VirtualRegister) ((Ret) original.getEntryBlock().getFirst()).getValue();
        assertEquals(Register.RD, committedValue.getAssignedPhysicalRegister());
        assertNotSame(value, committedValue);
    }

    private static IRUnit unit() {
        FunctionDeclaration declaration = new FunctionDeclaration(
                new Declarator(StorageQualifier.NONE, new I8Type()),
                Token.__internal(TokenType.IDENTIFIER, "snapshot_test"),
                new com.lnc.cc.ast.VariableDeclaration[0],
                false,
                new BlockStatement(new Statement[0])
        );
        declaration.setScope(Scope.createRoot("snapshot_test"));
        IRUnit unit = new IRUnit(declaration);
        unit.compileLocalMappings();
        return unit;
    }
}
