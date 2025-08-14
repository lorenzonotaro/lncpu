package com.lnc.cc.ir;

import java.util.*;

public abstract class GraphicalIRVisitor implements IIRInstructionVisitor<Void> {

    private final TraversalOrder traversalOrder;

    public static enum TraversalOrder {
        REVERSE_POST_ORDER_ONLY,
        CUSTOM_ENQUEUE_WITH_SUCCESSORS,
    }

    private GraphTraversalContext context;
    private IRInstruction currentInstruction;
    private IRUnit unit;

    static class GraphTraversalContext {
        private final boolean allowEnqueue;
        Deque<IRBlock> worklist = new ArrayDeque<>();
        Set<IRBlock> visited = new HashSet<>();
        private boolean scheduledThisBlock = false;

        private GraphTraversalContext(boolean allowEnqueue) {
            this.allowEnqueue = allowEnqueue;
        }

        void resetSchedule() {
            scheduledThisBlock = false;
        }

        boolean scheduledAny() {
            return scheduledThisBlock;
        }

        void enqueue(IRBlock block) {

            if(!allowEnqueue){
                throw new IllegalStateException("enqueue() called on a context that does not allow enqueueing blocks.");
            }

            if (visited.contains(block) || worklist.contains(block)) return;
            worklist.push(block);
            scheduledThisBlock = true;
        }

        void enqueueLast(IRBlock block) {
            if(!allowEnqueue){
                throw new IllegalStateException("enqueue() called on a context that does not allow enqueueing blocks.");
            }
            if (visited.contains(block) || worklist.contains(block)) return;
            worklist.addLast(block);
            scheduledThisBlock = true;
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
        this(TraversalOrder.CUSTOM_ENQUEUE_WITH_SUCCESSORS);
    }

    protected GraphicalIRVisitor(TraversalOrder traversalOrder) {
        this.traversalOrder = traversalOrder;
    }

    protected void reset() {
        this.context = null;
        this.unit = null;
        this.currentInstruction = null;
    }

    protected void visit(IRBlock block){
        for (currentInstruction = block.first; currentInstruction != null;) {
            IRInstruction current = currentInstruction;
            current.accept(this);

            // If the current instruction wasn't replaced or removed, move to the next one
            if (currentInstruction == current) {
                currentInstruction = current.next;
            }
            // else, currentInstruction was already set to a new instruction
        }
    }

    public void visit(IRUnit unit){

        this.unit = unit;

        var rpo = unit.computeReversePostOrderAndCFG();

        this.context = new GraphTraversalContext(traversalOrder == TraversalOrder.CUSTOM_ENQUEUE_WITH_SUCCESSORS);
        if(traversalOrder == TraversalOrder.CUSTOM_ENQUEUE_WITH_SUCCESSORS){
            context.enqueue(unit.getEntryBlock());

            while (!context.worklist.isEmpty()) {
                IRBlock block = context.next();

                if (!context.markVisited(block)) continue;

                // allow the block (its instructions) to schedule successors explicitly
                context.resetSchedule();
                visit(block);

                // If nothing was explicitly scheduled, fall back to enqueuing all successors
                if (!context.scheduledAny()) {
                    for (IRBlock succ : block.getSuccessors()) {
                        context.enqueueLast(succ);
                    }
                }
            }
        }else{
            // visit exactly in RPO
            for (IRBlock block : rpo) {
                if (context.markVisited(block)) {
                    visit(block);
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
        currentInstruction.remove();
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
