package com.lnc.cc.codegen;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.RegisterId;
import com.lnc.assembler.parser.argument.*;
import com.lnc.assembler.parser.argument.Register;
import com.lnc.cc.ir.CallingConvention;
import com.lnc.cc.ir.IRUnit;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.*;
import java.util.stream.Collectors;

public class StackFramePreservationPass extends AbstractAsmLevelLinearPass{
    private final Set<String> returnRegisters;

    private final IRUnit.FrameInfo frameInfo;
    private final int stackParamsSize;

    public StackFramePreservationPass(IRUnit unit) {
        super();
        this.stackParamsSize = unit.getFunctionType().getParameterMapping()
                .stream()
                .filter(CallingConvention.ParamLocation::onStack)
                .mapToInt(CallingConvention.ParamLocation::size)
                .sum();
        this.returnRegisters = CallingConvention.returnRegisterFor(unit.getFunctionType().returnType).getRegisters().stream().map(Enum::toString).collect(Collectors.toSet());
        this.frameInfo = unit.getFrameInfo();
    }

    @Override
    public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public boolean runPass(LinkedList<CodeElement> code) {
        var discoveryPass = new DiscoveryPass();
        discoveryPass.runPass(code);

        var registers = discoveryPass.registers;
        registers.removeIf(r -> returnRegisters.contains(r.toString()));

        var registersList = new ArrayList<>(registers);


        var iterator = new ExtendedListIterator<>(code);

        iterator.next();

        iterator.addSequenceBeforeCurrent(registersList.stream().map(
                r -> new Instruction(Token.__internal(TokenType.PUSH, "push"), new Argument[]{
                        new Register(Token.__internal(TokenType.valueOf(r.toString()), r.toString()))
                })).toList());

        if(frameInfo.allocSize() > 0){
            // Allocate stack space for the frame
            iterator.addSequenceBeforeCurrent(List.of(new Instruction(
                    Token.__internal(TokenType.PUSH, "push"), new Argument[]{
                    CodeGenUtils.reg(TokenType.BP)
            }),
            new Instruction(
                    Token.__internal(TokenType.MOV, "mov"), new Argument[]{
                    CodeGenUtils.reg(TokenType.SP),
                    CodeGenUtils.reg(TokenType.BP)
            }),
            new Instruction(
                    Token.__internal(TokenType.ADD, "add"), new Argument[]{
                    CodeGenUtils.reg(TokenType.SP),
                    CodeGenUtils.immByte(frameInfo.allocSize())
            })));
        }

        Collections.reverse(registersList);

        var restorationPass = new RestorationPass(registersList, stackParamsSize, frameInfo);

        restorationPass.runPass(code);

        return true;
    }

    private static class RestorationPass extends AbstractAsmLevelLinearPass {

        private final LinkedList<RegisterId> registers;
        private final int stackParamsSize;
        private final IRUnit.FrameInfo frameInfo;

        public RestorationPass(ArrayList<RegisterId> registers, int stackParamsSize, IRUnit.FrameInfo frameInfo) {
            this.registers = new LinkedList<>(registers);
            this.stackParamsSize = stackParamsSize;
            this.frameInfo = frameInfo;
        }

        @Override
        public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
            return false;
        }

        @Override
        public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {
            if (instruction.getOpcode().type == TokenType.RET) {

                var list = new ArrayList<Instruction>();

                if (frameInfo.allocSize() > 0){
                    list.addAll(List.of(
                            new Instruction(Token.__internal(TokenType.MOV, "mov"), new Argument[]{
                                    CodeGenUtils.reg(TokenType.BP),
                                    CodeGenUtils.reg(TokenType.SP)
                            }),
                            new Instruction(Token.__internal(TokenType.POP, "pop"), new Argument[]{
                                    CodeGenUtils.reg(TokenType.BP)
                            })
                    ));
                }

                for (RegisterId reg : registers) {
                    list.add(new Instruction(Token.__internal(TokenType.POP, "pop"), new Argument[]{
                            new Register(Token.__internal(TokenType.valueOf(reg.toString()), reg.toString()))
                    }));
                }

                if(!list.isEmpty()){
                    var labels = instruction.getLabels();
                    instruction.clearLabels();
                    list.get(0).setLabels(labels);
                    iterator.addSequenceBeforeCurrent(list);
                }

                if(stackParamsSize > 0){
                    instruction.setArguments(
                            new Argument[]{
                                    CodeGenUtils.immByte(stackParamsSize)
                            }
                    );
                }
            }else{
                Argument[] arguments = instruction.getArguments();
                for (int i = 0; i < arguments.length; i++) {
                    var arg = arguments[i];

                    if(arg.type == Argument.Type.DEREFERENCE){
                        var deref = (Dereference) arg;
                        if(deref.inner.type == Argument.Type.REGISTER_OFFSET){
                            var regOffsetOp = (RegisterOffset) deref.inner;
                            if(regOffsetOp.register.reg == RegisterId.BP){
                                // this is a stack frame dereference
                                // if the op is MINUS, we know that it's a stack parameter, we need to adjust based on how many registers we saved
                                if(regOffsetOp.getOperator().type == TokenType.MINUS) {
                                    if(regOffsetOp.offset.type == Argument.Type.BYTE){
                                        var byteArg = (com.lnc.assembler.parser.argument.Byte) regOffsetOp.offset;
                                        byteArg.value += (byte) (registers.size() + (frameInfo.allocSize() > 0 ? 1 : 0));
                                    }else{
                                        // error
                                        throw new IllegalStateException("Unexpected argument type for stack parameter dereference: " + regOffsetOp.offset.type);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
    }


    private static class DiscoveryPass extends AbstractAsmLevelLinearPass{

        private final Set<RegisterId> registers = new LinkedHashSet<>();

        @Override
        public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
            return false;
        }

        @Override
        public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {

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
