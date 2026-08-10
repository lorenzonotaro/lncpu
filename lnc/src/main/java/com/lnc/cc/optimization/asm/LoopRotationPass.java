package com.lnc.cc.optimization.asm;

import com.lnc.assembler.common.LabelInfo;
import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.assembler.parser.argument.LabelRef;
import com.lnc.assembler.parser.argument.Register;
import com.lnc.assembler.parser.argument.ReservedSpace;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Rotates counted loops so the back edge becomes a conditional jump instead of an unconditional one.
 *
 * A loop whose header is a pure test followed by a conditional jump into the body compiles to
 * {@code L: test; jcc BODY; ...exit... BODY: ...body...; goto L}. Every iteration therefore pays
 * {@code goto}(3) + {@code test} + {@code jcc}(3). Duplicating the header test in front of the back
 * edge lets the taken path skip the {@code goto} entirely:
 *
 * <pre>
 *   BODY: ...body...
 *         test          ; copy
 *         jcc BODY      ; copy - taken path costs test+jcc and never executes the goto
 *         goto L        ; original back edge, now only reached on the final (exiting) iteration
 * </pre>
 *
 * The original {@code goto L} is deliberately kept rather than retargeted, which avoids
 * synthesising a new label: on the exiting iteration control reaches {@code L}, re-runs the same
 * pure test over unmodified operands, and takes the same not-taken branch. Saves 3 cycles per
 * taken back edge at a cost of 5 cycles on the single exiting iteration and 4 bytes per loop.
 *
 * The duplicated test must be side-effect free, which is why only {@code cmp} and the idempotent
 * {@code and R, R} / {@code or R, R} zero-tests are accepted.
 */
public class LoopRotationPass extends AbstractAsmLevelLinearPass {

    private List<CodeElement> snapshot;
    private boolean rewritten;

    @Override
    public boolean runPass(LinkedList<CodeElement> code) {
        this.snapshot = List.copyOf(code);
        this.rewritten = false;
        return super.runPass(code);
    }

    @Override
    public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public Boolean visit(ReservedSpace reservedSpace, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {
        if (rewritten || instruction.getOpcode().type != TokenType.GOTO) {
            return false;
        }

        Argument[] gotoArgs = instruction.getArguments();
        if (gotoArgs.length != 1 || !(gotoArgs[0] instanceof LabelRef headerRef)) {
            return false;
        }

        int gotoPos = snapshot.indexOf(instruction);
        if (gotoPos < 0) {
            return false;
        }

        int headerPos = findLabelBefore(headerRef.labelToken.lexeme, gotoPos);
        if (headerPos < 0 || crossesTopLevelLabel(headerPos, gotoPos)) {
            return false;
        }

        if (!(snapshot.get(headerPos) instanceof Instruction test) || !isPureTest(test)) {
            return false;
        }
        if (headerPos + 1 >= snapshot.size()
                || !(snapshot.get(headerPos + 1) instanceof Instruction cond)
                || !isConditionalJump(cond)) {
            return false;
        }

        int bodyPos = findLabelBefore(((LabelRef) cond.getArguments()[0]).labelToken.lexeme, gotoPos);
        if (bodyPos <= headerPos + 1 || bodyPos > gotoPos) {
            return false;
        }

        if (alreadyRotated(gotoPos, test, cond)) {
            return false;
        }

        Instruction testCopy = new Instruction(test.getOpcode(), test.getArguments());
        Instruction condCopy = new Instruction(cond.getOpcode(), cond.getArguments());
        testCopy.setLabels(instruction.getLabels());
        instruction.setLabels(new ArrayList<>());

        iterator.addSequenceBeforeCurrent(List.of(testCopy, condCopy));
        rewritten = true;
        return true;
    }

    private boolean alreadyRotated(int gotoPos, Instruction test, Instruction cond) {
        if (gotoPos < 2) {
            return false;
        }
        return snapshot.get(gotoPos - 2) instanceof Instruction previousTest
                && snapshot.get(gotoPos - 1) instanceof Instruction previousCond
                && sameInstruction(previousTest, test)
                && sameInstruction(previousCond, cond);
    }

    private static boolean sameInstruction(Instruction a, Instruction b) {
        return a.getOpcode().type == b.getOpcode().type
                && Arrays.equals(a.getArguments(), b.getArguments());
    }

    private int findLabelBefore(String lexeme, int beforePos) {
        for (int i = beforePos; i >= 0; i--) {
            for (LabelInfo label : snapshot.get(i).getLabels()) {
                if (label.extractSubLabelName().equals(lexeme)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean crossesTopLevelLabel(int fromPos, int toPos) {
        for (int i = fromPos + 1; i <= toPos; i++) {
            for (LabelInfo label : snapshot.get(i).getLabels()) {
                if (!label.name().contains("$")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPureTest(Instruction instruction) {
        Argument[] args = instruction.getArguments();
        if (args.length != 2) {
            return false;
        }
        return switch (instruction.getOpcode().type) {
            case CMP -> true;
            case AND, OR -> args[0] instanceof Register a
                    && args[1] instanceof Register b
                    && a.reg == b.reg;
            default -> false;
        };
    }

    private static boolean isConditionalJump(Instruction instruction) {
        return switch (instruction.getOpcode().type) {
            case JC, JN, JZ -> instruction.getArguments().length == 1
                    && instruction.getArguments()[0] instanceof LabelRef;
            default -> false;
        };
    }
}
