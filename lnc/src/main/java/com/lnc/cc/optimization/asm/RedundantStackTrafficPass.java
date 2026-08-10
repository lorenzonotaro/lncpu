package com.lnc.cc.optimization.asm;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.RegisterId;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.assembler.parser.argument.Dereference;
import com.lnc.assembler.parser.argument.Register;
import com.lnc.assembler.parser.argument.RegisterOffset;
import com.lnc.assembler.parser.argument.ReservedSpace;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.TokenType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Removes stack-slot loads and stores that are provably no-ops.
 *
 * The spill inserter works one register touch at a time, so a value that is stored to its slot and
 * read straight back produces sequences like {@code mov RC,[BP+4] / mov [BP+4],RC} where the reload
 * cannot change RC. Each such instruction costs 4 cycles and 3 bytes.
 *
 * The pass tracks, over a straight-line run, which stack slot each register still mirrors. Anything
 * that could invalidate that relationship - a label, control flow, a call, a push/pop, a write to
 * BP/SP, or a store through any non-BP-relative address that might alias the frame - resets the
 * state, so only provably dead accesses are dropped.
 */
public class RedundantStackTrafficPass extends AbstractAsmLevelLinearPass {

    private final Map<RegisterId, String> slotMirroredByRegister = new HashMap<>();

    @Override
    public boolean runPass(LinkedList<CodeElement> code) {
        slotMirroredByRegister.clear();
        return super.runPass(code);
    }

    @Override
    public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
        slotMirroredByRegister.clear();
        return false;
    }

    @Override
    public Boolean visit(ReservedSpace reservedSpace, ExtendedListIterator<CodeElement> iterator) {
        slotMirroredByRegister.clear();
        return false;
    }

    @Override
    public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {
        if (!instruction.getLabels().isEmpty()) {
            slotMirroredByRegister.clear();
        }

        Argument[] args = instruction.getArguments();

        if (instruction.getOpcode().type == TokenType.MOV && args.length == 2) {
            String storedSlot = stackSlotOf(args[1]);
            if (storedSlot != null && args[0] instanceof Register source) {
                return applyStore(instruction, iterator, storedSlot, source.reg);
            }

            String loadedSlot = stackSlotOf(args[0]);
            if (loadedSlot != null && args[1] instanceof Register destination) {
                return applyLoad(instruction, iterator, loadedSlot, destination.reg);
            }
        }

        applyUnmodelledEffects(instruction);
        return false;
    }

    private Boolean applyStore(Instruction instruction, ExtendedListIterator<CodeElement> iterator,
                               String slot, RegisterId source) {
        if (slot.equals(slotMirroredByRegister.get(source))) {
            return removeInstruction(instruction, iterator);
        }
        slotMirroredByRegister.values().removeIf(slot::equals);
        slotMirroredByRegister.put(source, slot);
        return false;
    }

    private Boolean applyLoad(Instruction instruction, ExtendedListIterator<CodeElement> iterator,
                              String slot, RegisterId destination) {
        if (slot.equals(slotMirroredByRegister.get(destination))) {
            return removeInstruction(instruction, iterator);
        }
        slotMirroredByRegister.put(destination, slot);
        return false;
    }

    private Boolean removeInstruction(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {
        var labels = instruction.getLabels();
        iterator.removeCurrent();
        if (!labels.isEmpty() && iterator.hasNext()) {
            iterator.peek().getLabels().addAll(0, labels);
        }
        return true;
    }

    private void applyUnmodelledEffects(Instruction instruction) {
        Argument[] args = instruction.getArguments();

        switch (instruction.getOpcode().type) {
            case CMP -> {
                return;
            }
            case MOV -> {
                if (args.length == 2 && writesOnlyRegister(args[1]) && !readsOrWritesMemory(args[0])) {
                    invalidateRegister(args[1]);
                    return;
                }
            }
            case ADD, SUB, AND, OR, XOR -> {
                if (args.length == 2 && writesOnlyRegister(args[0]) && !readsOrWritesMemory(args[1])) {
                    invalidateRegister(args[0]);
                    return;
                }
            }
            case NOT, INC, DEC, SHL, SHR -> {
                if (args.length == 1 && writesOnlyRegister(args[0])) {
                    invalidateRegister(args[0]);
                    return;
                }
            }
            default -> {
            }
        }

        slotMirroredByRegister.clear();
    }

    private void invalidateRegister(Argument argument) {
        slotMirroredByRegister.remove(((Register) argument).reg);
    }

    private static boolean writesOnlyRegister(Argument argument) {
        return argument instanceof Register register && register.reg.generalPurpose;
    }

    private static boolean readsOrWritesMemory(Argument argument) {
        return argument.type == Argument.Type.DEREFERENCE;
    }

    private static String stackSlotOf(Argument argument) {
        return argument instanceof Dereference dereference
                && dereference.inner instanceof RegisterOffset offset
                && offset.register.reg == RegisterId.BP
                ? offset.toString()
                : null;
    }
}
