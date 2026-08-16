package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.*;

import java.util.*;

final class IRUnitSnapshot {
    private final IRUnit source;
    private final IRUnit copy;
    private final IdentityHashMap<IRBlock, IRBlock> blocks = new IdentityHashMap<>();
    private final IdentityHashMap<VirtualRegister, VirtualRegister> registers = new IdentityHashMap<>();
    private final IdentityHashMap<IROperand, IROperand> operands = new IdentityHashMap<>();

    private IRUnitSnapshot(IRUnit source) {
        this.source = source;
        this.copy = IRUnit.emptySnapshotShell(source);
    }

    static IRUnit copyOf(IRUnit source) {
        return new IRUnitSnapshot(source).copy();
    }

    private IRUnit copy() {
        List<IRBlock> roots = new ArrayList<>();
        roots.add(source.getEntryBlock());
        roots.add(source.getCurrentBlock());
        for (LoopInfo loop : source.snapshotLoopStack()) {
            roots.add(loop.continueTarget());
            roots.add(loop.breakTarget());
        }
        List<IRBlock> sourceBlocks = collectBlocks(roots);
        for (IRBlock block : sourceBlocks) {
            blocks.put(block, new IRBlock(copy, block.getId(), block.getLoopDepth()));
        }

        VirtualRegisterManager manager = new VirtualRegisterManager();
        source.getVirtualRegisterManager().getAllRegisters().stream()
                .sorted(Comparator.comparingInt(VirtualRegister::getRegisterNumber))
                .forEach(register -> {
                    VirtualRegister cloned = new VirtualRegister(register.getRegisterNumber(), register.getTypeSpecifier());
                    cloned.setRegisterClass(register.getRegisterClass());
                    cloned.setAssignedPhysicalRegister(register.getAssignedPhysicalRegister());
                    registers.put(register, cloned);
                    manager.addSnapshotRegister(cloned);
                });

        for (IRBlock block : sourceBlocks) {
            IRBlock clonedBlock = blocks.get(block);
            for (IRInstruction instruction : block) {
                IRInstruction cloned = cloneInstruction(instruction);
                cloned.setIndex(instruction.getIndex());
                cloned.setSourceToken(instruction.getSourceToken());
                clonedBlock.emit(cloned);
            }
        }

        IRUnit.LocalMappingInfo mappings = cloneMappings(source.getLocalMappingInfo());
        IRUnit.FrameInfo frame = source.getFrameInfo() == null ? null : new IRUnit.FrameInfo(
                source.getFrameInfo().forcedLocalsSize(),
                source.getFrameInfo().spillSpaceSize(),
                new HashMap<>(source.getFrameInfo().localOffsets())
        );
        List<LoopInfo> loops = source.snapshotLoopStack().stream()
                .map(loop -> new LoopInfo(blocks.get(loop.continueTarget()), blocks.get(loop.breakTarget())))
                .toList();
        copy.restoreSnapshotState(
                blocks.get(source.getEntryBlock()),
                blocks.get(source.getCurrentBlock()),
                source.snapshotBlockCounter(),
                manager,
                loops,
                source.getSpillSpaceSize(),
                source.getUsedRegisters() == null ? null : new LinkedHashSet<>(source.getUsedRegisters()),
                frame,
                mappings
        );
        copy.computeReversePostOrderAndCFG();
        return copy;
    }

    private List<IRBlock> collectBlocks(Collection<IRBlock> roots) {
        List<IRBlock> ordered = new ArrayList<>();
        Set<IRBlock> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<IRBlock> pending = new ArrayDeque<>();
        roots.stream().filter(Objects::nonNull).forEach(pending::addLast);
        while (!pending.isEmpty()) {
            IRBlock block = pending.pop();
            if (!seen.add(block)) continue;
            ordered.add(block);
            List<IRBlock> targets = new ArrayList<>(block.getLastInstructionTargets());
            if (block.getLast() instanceof CondJump jump && jump.getContinueTo() != null) {
                targets.add(jump.getContinueTo());
            }
            for (int index = targets.size() - 1; index >= 0; index--) pending.push(targets.get(index));
        }
        return ordered;
    }

    private IRInstruction cloneInstruction(IRInstruction instruction) {
        if (instruction instanceof Move move) return new Move(op(move.getSource()), op(move.getDest()), move.isRegParamDemotion());
        if (instruction instanceof Bin bin) return new Bin(op(bin.getDest()), op(bin.getLeft()), op(bin.getRight()), bin.getOperator());
        if (instruction instanceof Unary unary) return new Unary(op(unary.getTarget()), op(unary.getOperand()), unary.getOperator());
        if (instruction instanceof Push push) return new Push(op(push.getArg()));
        if (instruction instanceof Pop pop) return new Pop(op(pop.getArg()));
        if (instruction instanceof Ret ret) return new Ret(ret.getValue() == null ? null : op(ret.getValue()));
        if (instruction instanceof Call call) {
            IROperand[] args = call.getArguments() == null ? null : Arrays.stream(call.getArguments()).map(this::op).toArray(IROperand[]::new);
            return new Call(call.getReturnTarget() == null ? null : vr(call.getReturnTarget()), op(call.getCallee()), args);
        }
        if (instruction instanceof Goto jump) return new Goto(blocks.get(jump.getTarget()));
        if (instruction instanceof CondJump jump) return new CondJump(
                jump.getCond(), op(jump.getLeft()), op(jump.getRight()), blocks.get(jump.getTarget()),
                blocks.get(jump.getFalseTarget()), blocks.get(jump.getContinueTo()));
        throw new IllegalArgumentException("Unsupported IR instruction snapshot: " + instruction.getClass().getName());
    }

    private IROperand op(IROperand operand) {
        if (operand == null) return null;
        IROperand existing = operands.get(operand);
        if (existing != null) return existing;
        IROperand cloned;
        if (operand instanceof VirtualRegister register) cloned = vr(register);
        else if (operand instanceof ImmediateOperand immediate) cloned = new ImmediateOperand(immediate.getValue(), immediate.getTypeSpecifier());
        else if (operand instanceof StackFrameLocation stack) cloned = new StackFrameLocation(stack.getTypeSpecifier(), stack.getOperandType(), stack.getOffset());
        else if (operand instanceof StaticSymbolLocation symbol) cloned = new StaticSymbolLocation(symbol.getSymbol());
        else if (operand instanceof StaticDerivedLocation derived) cloned = new StaticDerivedLocation((StaticLocation) op(derived.getBase()), derived.getOffset(), derived.getTypeSpecifier());
        else if (operand instanceof DerefLocation deref) cloned = new DerefLocation(op(deref.getTarget()), deref.getTypeSpecifier());
        else if (operand instanceof ArrayIndexLocation array) cloned = new ArrayIndexLocation((Location) op(array.getBase()), op(array.getIndex()), array.getType(), array.getStride());
        else if (operand instanceof StructMemberAccess member) cloned = new StructMemberAccess((Location) op(member.getBase()), member.getField());
        else if (operand instanceof AddressOf address) cloned = new AddressOf((Location) op(address.getOperand()));
        else if (operand instanceof SizedCast cast) cloned = new SizedCast(op(cast.getOperand()), cast.getTypeSpecifier(), cast.getByteSelection());
        else if (operand instanceof ComposeOperand compose) cloned = new ComposeOperand(op(compose.high), op(compose.low), compose.getTypeSpecifier());
        else if (operand instanceof VaPop vaPop) cloned = new VaPop(vr(vaPop.getTempVr()), vaPop.getTypeSpecifier());
        else throw new IllegalArgumentException("Unsupported IR operand snapshot: " + operand.getClass().getName());
        cloned.type = operand.type;
        operands.put(operand, cloned);
        return cloned;
    }

    private VirtualRegister vr(VirtualRegister register) {
        VirtualRegister cloned = registers.get(register);
        if (cloned == null) throw new IllegalStateException("Virtual register is not owned by snapshot source: r" + register.getRegisterNumber());
        operands.putIfAbsent(register, cloned);
        return cloned;
    }

    private IRUnit.LocalMappingInfo cloneMappings(IRUnit.LocalMappingInfo sourceMappings) {
        if (sourceMappings == null) return null;
        HashMap<String, IROperand> mappings = new HashMap<>();
        sourceMappings.mappings().forEach((name, operand) -> mappings.put(name, op(operand)));
        HashMap<String, VirtualRegister> parameters = new HashMap<>();
        sourceMappings.originalRegParamMappings().forEach((name, register) -> parameters.put(name, vr(register)));
        return new IRUnit.LocalMappingInfo(mappings, parameters, sourceMappings.forcedStackFrameLocalsSize());
    }
}
