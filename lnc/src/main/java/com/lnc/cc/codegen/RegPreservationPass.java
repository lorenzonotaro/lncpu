package com.lnc.cc.codegen;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.RegisterId;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.assembler.parser.argument.Register;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.optimization.IRPass;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.*;
import java.util.stream.Collectors;

public class RegPreservationPass extends AbstractAsmLevelLinearPass{
    private final Set<String> returnRegisters;

    public RegPreservationPass(IRUnit unit) {
        super();
        this.returnRegisters = unit.getFunctionType().getParameterMapping()
                .stream()
                .filter(p -> !p.onStack())
                .flatMap(p -> p.regClass().getRegisters().stream()
                        .flatMap(r -> Arrays.stream(r.getComponents())))
                .map(com.lnc.cc.codegen.Register::toString)
                .collect(Collectors.toSet());
    }

    @Override
    public Boolean visit(EncodedData encodedData) {
        return false;
    }

    @Override
    public Boolean visit(Instruction instruction) {
        return false;
    }

    @Override
    public boolean runPass(LinkedList<CodeElement> code) {
        var discoveryPass = new DiscoveryPass();
        discoveryPass.runPass(code);

        var registers = discoveryPass.registers;
        registers.removeIf(r -> returnRegisters.contains(r.toString()));

        var registersList = new ArrayList<>(registers);

        AsmCursor cursor = new AsmCursor(code);

        cursor.next();

        cursor.insertSequenceBeforeCurrent(registersList.stream().map(
                r -> new Instruction(Token.__internal(TokenType.PUSH, "push"), new Argument[]{
                        new Register(Token.__internal(TokenType.valueOf(r.toString()), r.toString()))
                })).toList());

        Collections.reverse(registersList);

        var restorationPass = new RestorationPass(registersList);

        restorationPass.runPass(code);

        return true;
    }

    private static class RestorationPass extends AbstractAsmLevelLinearPass {

        private final LinkedList<RegisterId> registers;

        public RestorationPass(ArrayList<RegisterId> registers) {
            this.registers = new LinkedList<>(registers);
        }

        @Override
        public Boolean visit(EncodedData encodedData) {
            return false;
        }

        @Override
        public Boolean visit(Instruction instruction) {
            if (instruction.getOpcode().type == TokenType.RET) {
                for (RegisterId reg : registers) {
                    this.getCursor().insertBeforeCurrent(new Instruction(Token.__internal(TokenType.POP, "pop"), new Argument[]{
                            new Register(Token.__internal(TokenType.valueOf(reg.toString()), reg.toString()))
                    }));
                }
            }
            return false;
        }
    }


    private static class DiscoveryPass extends AbstractAsmLevelLinearPass{

        private final Set<RegisterId> registers = new LinkedHashSet<>();

        @Override
        public Boolean visit(EncodedData encodedData) {
            return false;
        }

        @Override
        public Boolean visit(Instruction instruction) {

            registers.addAll(getModifiedRegisters(instruction));

            return false;
        }

        private Collection<RegisterId> getModifiedRegisters(Instruction instruction) {
            return switch (instruction.getOpcode().type) {
                case ADD, SUB, AND, OR, XOR, SHL, SHR, NOT, INC, DEC, POP -> {
                    var arg = instruction.getArguments()[0];
                    yield arg.type == Argument.Type.REGISTER
                            ? Collections.singleton(((Register) arg).reg)
                            : Collections.emptySet();
                }
                case MOV -> {
                    var arg = instruction.getArguments()[1];

                    yield arg.type == Argument.Type.REGISTER
                            ? Collections.singleton(((Register) arg).reg)
                            : Collections.emptySet();
                }
                case SWAP -> // all arguments
                        Arrays.stream(instruction.getArguments())
                                .filter(arg -> arg.type == Argument.Type.REGISTER)
                                .map(arg -> ((Register) arg).reg)
                                .collect(Collectors.toSet());
                default -> Collections.emptySet();
            };
        }


    }
}
