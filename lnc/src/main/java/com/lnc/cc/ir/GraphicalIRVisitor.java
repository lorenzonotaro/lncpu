package com.lnc.cc.ir;

import java.util.*;

public abstract class GraphicalIRVisitor implements IIRInstructionVisitor<Void> {

    private GraphTraversalContext context;

    private IRInstruction currentInstruction;
    private IRUnit unit;

    static class GraphTraversalContext {
        Deque<IRBlock> worklist = new ArrayDeque<>();
        Set<IRBlock> visited = new HashSet<>();

        void enqueue(IRBlock block) {
            if (visited.contains(block) || worklist.contains(block)) return;
            worklist.push(block);
        }

        public IRBlock next() {
            return worklist.pop();
        }

        public boolean isEmpty() {
            return worklist.isEmpty();
        }

        public boolean markVisited(IRBlock block) {
            return visited.add(block);
        }
    }

    protected GraphicalIRVisitor() {
    }


    protected void visit(IRBlock block){
        for (currentInstruction = block.first; currentInstruction != null;) {
            IRInstruction current = currentInstruction;
            IRInstruction next = current.next;

            current.accept(this);

            if (currentInstruction == current) {
                currentInstruction = next;
            }else if(currentInstruction.next != next){
                // If the instructions after the current instruction have been modified,
                // we need to continue from the next instruction
                currentInstruction = current.next;
            }
        }
    }

    public void visit(IRUnit unit){

        this.unit = unit;

        unit.computeReversePostOrderAndCFG();

        this.context = new GraphTraversalContext();
        context.enqueue(unit.getEntryBlock());

        while (!context.worklist.isEmpty()) {
            IRBlock block = context.next();

            if (!context.markVisited(block)) continue;

            visit(block);

            // Optionally enqueue all successors by default
            for (IRBlock succ : block.getSuccessors()) {
                {
                    context.enqueue(succ);
                }
            }
        }
    }

    protected final void replaceAndContinue(IRInstruction replacement) {
        currentInstruction.replaceWith(replacement);
        currentInstruction = replacement;
    }

    protected final void deleteAndContinue() {
        IRInstruction next = currentInstruction.next;
        currentInstruction.delete();
        currentInstruction = next;
    }

    protected IRUnit getUnit() {
        return unit;
    }

    protected final void enqueue(IRBlock block) {
        context.enqueue(block);
    }

    public IRInstruction getCurrentInstruction() {
        return currentInstruction;
    }

}
