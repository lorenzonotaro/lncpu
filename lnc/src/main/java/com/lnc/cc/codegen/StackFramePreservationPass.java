package com.lnc.cc.codegen;

import com.lnc.assembler.common.LabelInfo;
import com.lnc.assembler.parser.*;
import com.lnc.assembler.parser.argument.*;
import com.lnc.assembler.parser.argument.Register;
import com.lnc.cc.ir.CallingConvention;
import com.lnc.cc.ir.IRUnit;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The {@code StackFramePreservationPass} class is designed to manage the preservation and
 * restoration of the stack frame during assembly-level code generation.
 *
 * This class ensures that any modifications to the stack or register state across function
 * calls are tracked, stacked, and correctly restored, adhering to the calling conventions
 * required by the assembly language backend. It automates the addition of instructions that
 * save and restore certain registers or stack space to maintain consistency across the
 * function's execution.
 *
 * The pass operates in three key phases:
 * 1. {@code DiscoveryPass}: Collects information about all modified registers during the
 *    function's execution, excluding those used for returning values.
 * 2. Preservation Phase: Generates necessary instructions at the function's entry point to
 *    save these modified registers and allocate stack space for the function's frame.
 * 3. {@code RestorationPass}: Adds necessary instructions to restore saved registers and
 *    stack state at every function return point.
 *
 * Key components include:
 * - Computing the necessary stack allocations based on the calling convention and function
 *   parameters.
 * - Generating assembly instructions for saving and restoring registers.
 * - Adjusting offset values for stack parameter dereferencing to account for the frame's
 *   preserved state.
 *
 * This pass assumes compatibility with the assembly-level linear pass framework and
 * implements the {@code AbstractAsmLevelLinearPass} base class for traversal and modification
 * of code elements.
 */
public class StackFramePreservationPass extends AbstractAsmLevelLinearPass{
    private final Set<String> returnRegisters;

    private final IRUnit.FrameInfo frameInfo;
    private final int stackParamsSize;
    private final String funName;

    public StackFramePreservationPass(IRUnit unit) {
        super();
        this.stackParamsSize = unit.getFunctionType().getParameterMapping()
                .stream()
                .filter(CallingConvention.ParamLocation::onStack)
                .mapToInt(CallingConvention.ParamLocation::size)
                .sum();
        RegisterClass registerClass = CallingConvention.returnRegisterFor(unit.getFunctionType().returnType);
        this.returnRegisters = registerClass == null ? Set.of() : registerClass.getRegisters().stream().map(Enum::toString).collect(Collectors.toSet());
        this.frameInfo = unit.getFrameInfo();
        this.funName = unit.getFunctionDeclaration().name.lexeme;
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

        var firstInstr = iterator.next();

        var list = new ArrayList<>(registersList.stream().map(
                r -> new Instruction(Token.__internal(TokenType.PUSH, "push"), new Argument[]{
                        new Register(Token.__internal(TokenType.valueOf(r.toString()), r.toString()))
                })).toList());

        if(frameInfo.allocSize() > 0 || stackParamsSize > 0) {
            // Allocate stack space for the frame
            list.addAll(List.of(new Instruction(
                    Token.__internal(TokenType.PUSH, "push"), new Argument[]{
                    CodeGenUtils.reg(TokenType.BP)
            }),
            new Instruction(
                    Token.__internal(TokenType.MOV, "mov"), new Argument[]{
                    CodeGenUtils.reg(TokenType.SP),
                    CodeGenUtils.reg(TokenType.BP)
            })));

            if(frameInfo.allocSize() > 0) {
                list.add(new Instruction(
                        Token.__internal(TokenType.ADD, "add"), new Argument[]{
                        CodeGenUtils.reg(TokenType.SP),
                        CodeGenUtils.immByte(frameInfo.allocSize())
                }));
            }
        }

        if(!list.isEmpty()){
            iterator.addSequenceBeforeCurrent(list);
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

                if (frameInfo.allocSize() > 0 || stackParamsSize > 0) {
                    if(frameInfo.allocSize() > 0) {
                        list.add(
                                new Instruction(Token.__internal(TokenType.MOV, "mov"), new Argument[]{
                                        CodeGenUtils.reg(TokenType.BP),
                                        CodeGenUtils.reg(TokenType.SP)
                                }));
                    }
                    list.add(
                            new Instruction(Token.__internal(TokenType.POP, "pop"), new Argument[]{
                                    CodeGenUtils.reg(TokenType.BP)
                            })
                    );
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
                                        byteArg.value += 3; /* account for CS:PC + stack pointer actually pointing to the next available slot*/
                                        byteArg.value += registers.size() + 1; /* account for the registers we pushed + BP */
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
                default -> Collections.emptySet();
            };
        }


    }
}
