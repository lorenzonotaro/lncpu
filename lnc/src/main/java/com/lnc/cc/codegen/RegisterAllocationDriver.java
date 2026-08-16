package com.lnc.cc.codegen;

import com.lnc.LNC;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.common.Logger;

import java.util.*;
import java.util.stream.Collectors;

final class RegisterAllocationDriver {
    private RegisterAllocationDriver() {
    }

    static GraphColoringRegisterAllocator.AllocationInfo run(IRUnit unit) {
        GraphColoringRegisterAllocator.clearRegAllocIterFiles(unit);
        String strategy = LNC.settings.get("--reg-alloc-strategy", String.class);
        return switch (strategy) {
            case "greedy" -> runGreedyStrategy(unit);
            case "dfs" -> runDfsStrategy(unit);
            default -> throw new IllegalArgumentException("Unknown register-allocation strategy: " + strategy);
        };
    }

    private static GraphColoringRegisterAllocator.AllocationInfo runGreedyStrategy(IRUnit unit) {
        Terminal terminal = greedy(unit, true);
        GraphColoringRegisterAllocator.SearchDiagnostics diagnostics = new GraphColoringRegisterAllocator.SearchDiagnostics(
                0, false, false, terminal.metric(), terminal.metric()
        );
        return terminal.info(diagnostics);
    }

    private static GraphColoringRegisterAllocator.AllocationInfo runDfsStrategy(IRUnit original) {
        Terminal greedy = greedy(original.snapshotForRegisterAllocation(), true);
        int maxDepth = settingInt("--reg-alloc-dfs-max-depth");
        int maxStates = settingInt("--reg-alloc-dfs-max-states");
        if (maxDepth < 0 || maxStates < 1) {
            throw new IllegalArgumentException("DFS register-allocation limits require depth >= 0 and states >= 1");
        }

        Terminal incumbent = greedy;
        Deque<SearchState> pending = new ArrayDeque<>();
        pending.push(SearchState.initial(original.snapshotForRegisterAllocation()));
        int statesExplored = 0;
        boolean truncatedByDepth = false;

        while (!pending.isEmpty() && statesExplored < maxStates) {
            SearchState state = pending.pop();
            statesExplored++;
            IRUnit inspectedUnit = state.unit().snapshotForRegisterAllocation();
            Round round = inspect(inspectedUnit, state.eligibleRegisterNumbers(), false, state.rounds(), null);
            if (round.choices().isEmpty()) {
                Terminal terminal = terminal(inspectedUnit, state, round);
                if (terminal.metric().compareTo(incumbent.metric()) < 0) incumbent = terminal;
                continue;
            }
            if (state.depth() >= maxDepth) {
                truncatedByDepth = true;
                continue;
            }

            List<GraphColoringRegisterAllocator.SpillChoice> choices = round.choices();
            for (int index = choices.size() - 1; index >= 0; index--) {
                pending.push(branch(state, choices.get(index)));
            }
        }

        boolean truncatedByStates = !pending.isEmpty();
        Terminal chosen = truncatedByDepth || truncatedByStates ? greedy : incumbent;
        original.commitRegisterAllocationSnapshot(chosen.unit());
        GraphColoringRegisterAllocator.SearchDiagnostics diagnostics = new GraphColoringRegisterAllocator.SearchDiagnostics(
                statesExplored,
                truncatedByDepth,
                truncatedByStates,
                greedy.metric(),
                chosen.metric()
        );
        if (LNC.settings.get("--reg-alloc-dfs-diagnostics", Boolean.class)) {
            Logger.out("reg-alloc-dfs: unit=%s states=%d truncated-depth=%s truncated-states=%s greedy=%s chosen=%s"
                    .formatted(
                            original.getFunctionDeclaration().name.lexeme,
                            statesExplored,
                            truncatedByDepth,
                            truncatedByStates,
                            greedy.metric(),
                            chosen.metric()
                    ));
        }
        return chosen.info(diagnostics);
    }

    private static Terminal greedy(IRUnit unit, boolean outputSteps) {
        Set<Integer> eligible = initialEligibleNumbers(unit);
        SpillSlotAssigner slots = new SpillSlotAssigner();
        List<List<Integer>> sequence = new ArrayList<>();
        InterferenceGraph.Node previous = null;
        int maxRounds = settingInt("--reg-alloc-max-iter");

        for (int roundIndex = 0; roundIndex < maxRounds; roundIndex++) {
            Round round = inspect(unit, eligible, outputSteps, roundIndex, previous);
            if (round.choices().isEmpty()) {
                unit.setSpillSpaceSize(slots.getTotalSlots());
                unit.setUsedRegisters(round.usedRegisters());
                GraphColoringRegisterAllocator.TerminalMetric metric = RegisterAllocationMetrics.measure(unit, sequence);
                return new Terminal(unit, round.graph(), round.liveness(), metric);
            }
            GraphColoringRegisterAllocator.SpillChoice choice = round.choices().get(0);
            Set<VirtualRegister> spilled = round.allocator().getSpilledVirtualRegisters(choice);
            GraphColoringRegisterAllocator.applySpillChoice(unit, round.liveness(), round.graph(), spilled, slots);
            eligible.removeAll(choice.registerNumbers());
            sequence.add(choice.registerNumbers());
            unit.setSpillSpaceSize(slots.getTotalSlots());
            previous = round.allocator().getSpillCandidate();
        }
        throw new RuntimeException("Exceeded maximum iterations for register allocation for unit "
                + unit.getFunctionDeclaration().name.lexeme);
    }

    private static SearchState branch(SearchState parent, GraphColoringRegisterAllocator.SpillChoice choice) {
        IRUnit unit = parent.unit().snapshotForRegisterAllocation();
        SpillSlotAssigner slots = parent.slots().copyFor(unit.getVirtualRegisterManager().getAllRegisters());
        Round round = inspect(unit, parent.eligibleRegisterNumbers(), false, parent.rounds(), null);
        GraphColoringRegisterAllocator.SpillChoice mappedChoice = round.choices().stream()
                .filter(choice::equals)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("DFS spill choice changed while cloning branch: " + choice));
        Set<VirtualRegister> spilled = round.allocator().getSpilledVirtualRegisters(mappedChoice);
        GraphColoringRegisterAllocator.applySpillChoice(unit, round.liveness(), round.graph(), spilled, slots);
        unit.setSpillSpaceSize(slots.getTotalSlots());
        Set<Integer> eligible = new LinkedHashSet<>(parent.eligibleRegisterNumbers());
        eligible.removeAll(mappedChoice.registerNumbers());
        List<List<Integer>> sequence = new ArrayList<>(parent.sequence());
        sequence.add(mappedChoice.registerNumbers());
        return new SearchState(unit, eligible, slots, parent.depth() + 1, parent.rounds() + 1, List.copyOf(sequence));
    }

    private static Round inspect(IRUnit unit,
                                 Set<Integer> eligibleNumbers,
                                 boolean outputStep,
                                 int roundIndex,
                                 InterferenceGraph.Node previous) {
        unit.getVirtualRegisterManager().clearAssignedPhysicalRegisters();
        LivenessInfo liveness = LivenessInfo.computeBlockLiveness(unit);
        InterferenceGraph graph = InterferenceGraph.buildInterferenceGraph(unit);
        if (LNC.settings.get("--print-ig", Boolean.class)) {
            InterferenceGraphVisualizer.setGraph(graph.getVirtualNodes());
        }
        if (outputStep) {
            String outputPath = LNC.settings.get("--output-reg-alloc-iter", String.class);
            if (!outputPath.isEmpty()) {
                GraphColoringRegisterAllocator.saveStep(unit, roundIndex, graph, previous, outputPath);
            }
        }
        Set<VirtualRegister> eligible = unit.getVirtualRegisterManager().getAllRegisters().stream()
                .filter(register -> eligibleNumbers.contains(register.getRegisterNumber()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        GraphColoringRegisterAllocator allocator = new GraphColoringRegisterAllocator(graph, eligible, unit, liveness);
        allocator.allocate();
        return new Round(
                graph,
                liveness,
                allocator,
                allocator.getSpillChoices(),
                new LinkedHashSet<>(allocator.getUsedRegisters())
        );
    }

    private static Terminal terminal(IRUnit inspectedUnit, SearchState state, Round round) {
        inspectedUnit.setSpillSpaceSize(state.slots().getTotalSlots());
        inspectedUnit.setUsedRegisters(round.usedRegisters());
        return new Terminal(
                inspectedUnit,
                round.graph(),
                round.liveness(),
                RegisterAllocationMetrics.measure(inspectedUnit, state.sequence())
        );
    }

    private static Set<Integer> initialEligibleNumbers(IRUnit unit) {
        Set<VirtualRegister> protectedRegisters = GraphColoringRegisterAllocator.constrainedCallReturnTargets(unit);
        return unit.getVirtualRegisterManager().getAllRegisters().stream()
                .filter(register -> !protectedRegisters.contains(register))
                .map(VirtualRegister::getRegisterNumber)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static int settingInt(String name) {
        return LNC.settings.get(name, Double.class).intValue();
    }

    private record Round(InterferenceGraph graph,
                         LivenessInfo liveness,
                         GraphColoringRegisterAllocator allocator,
                         List<GraphColoringRegisterAllocator.SpillChoice> choices,
                         Set<Register> usedRegisters) {
    }

    private record SearchState(IRUnit unit,
                               Set<Integer> eligibleRegisterNumbers,
                               SpillSlotAssigner slots,
                               int depth,
                               int rounds,
                               List<List<Integer>> sequence) {
        private static SearchState initial(IRUnit unit) {
            return new SearchState(unit, initialEligibleNumbers(unit), new SpillSlotAssigner(), 0, 0, List.of());
        }
    }

    private record Terminal(IRUnit unit,
                            InterferenceGraph graph,
                            LivenessInfo liveness,
                            GraphColoringRegisterAllocator.TerminalMetric metric) {
        private GraphColoringRegisterAllocator.AllocationInfo info(
                GraphColoringRegisterAllocator.SearchDiagnostics diagnostics) {
            return new GraphColoringRegisterAllocator.AllocationInfo(graph, liveness, metric, diagnostics);
        }
    }
}
