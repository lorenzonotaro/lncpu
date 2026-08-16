package com.lnc.cc.codegen;

import com.lnc.cc.ir.IRBlock;
import com.lnc.cc.ir.IRInstruction;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.ir.Move;
import com.lnc.cc.ir.operands.StackFrameLocation;

import java.util.List;

final class RegisterAllocationMetrics {
    private RegisterAllocationMetrics() {
    }

    static GraphColoringRegisterAllocator.TerminalMetric measure(IRUnit unit, List<List<Integer>> sequence) {
        int spillBase = unit.getLocalMappingInfo().forcedStackFrameLocalsSize();
        int spillEnd = spillBase + unit.getSpillSpaceSize();
        long weightedTraffic = 0;
        int spillInstructions = 0;
        for (IRBlock block : unit.computeReversePostOrderAndCFG()) {
            long weight = loopWeight(block.getLoopDepth());
            for (IRInstruction instruction : block) {
                int traffic = spillTraffic(instruction, spillBase, spillEnd);
                if (traffic == 0) continue;
                long weightedTouches = weight > Long.MAX_VALUE / traffic ? Long.MAX_VALUE : weight * traffic;
                weightedTraffic = saturatingAdd(weightedTraffic, weightedTouches);
                spillInstructions++;
            }
        }
        return new GraphColoringRegisterAllocator.TerminalMetric(
                true,
                weightedTraffic,
                spillInstructions,
                unit.getSpillSpaceSize(),
                unit.getSpillSpaceSize(),
                sequence.size(),
                sequence
        );
    }

    private static int spillTraffic(IRInstruction instruction, int start, int end) {
        if (!(instruction instanceof Move move)) return 0;
        int traffic = isSpillSlot(move.getSource(), start, end) ? 1 : 0;
        return traffic + (isSpillSlot(move.getDest(), start, end) ? 1 : 0);
    }

    private static boolean isSpillSlot(Object operand, int start, int end) {
        return operand instanceof StackFrameLocation stack
                && stack.getOperandType() == StackFrameLocation.OperandType.LOCAL
                && stack.getOffset() >= start
                && stack.getOffset() < end;
    }

    private static long loopWeight(int depth) {
        long weight = 1;
        for (int index = 0; index < depth; index++) {
            if (weight > Long.MAX_VALUE / 10) return Long.MAX_VALUE;
            weight *= 10;
        }
        return weight;
    }

    private static long saturatingAdd(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
