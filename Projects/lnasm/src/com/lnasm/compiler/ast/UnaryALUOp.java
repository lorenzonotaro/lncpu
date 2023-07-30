package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UnaryALUOp implements Encodeable {

    private final String instruction;
    private final String op;

    public UnaryALUOp(String instruction, String op) {
        this.instruction = instruction;
        this.op = op;
    }

    @Override
    public byte[] encode(Linker linker, short addr) {
        return new byte[]{OpcodeMap.getOpcode(instruction + "_" + op)};
    }

    @Override
    public int size() {
        return 1;
    }

    static class UnaryALUOpMatcher implements Matcher{

        private final Token.Type keyword;

        private final RegisterId[] allowedRegisters;

        public UnaryALUOpMatcher(Token.Type keyword, RegisterId[] allowedRegisters) {
            this.keyword = keyword;
            this.allowedRegisters = allowedRegisters;
        }

        public UnaryALUOpMatcher(Token.Type keyword){
            this(keyword, new RegisterId[]{RegisterId.RA, RegisterId.RB, RegisterId.RC, RegisterId.RD});
        }

        @Override
        public Token.Type getKeyword() {
            return keyword;
        }

        @Override
        public boolean matches(Argument... arguments) {
            return arguments.length == 1 && arguments[0].type == Argument.Type.REGISTER;
        }

        @Override
        public Encodeable make(Argument... arguments) {
            List<RegisterId> list = Arrays.asList(this.allowedRegisters);
            if(!list.contains(((Argument.Register)arguments[0]).reg))
                throw new CompileException("invalid operand for instruction '" + keyword.toString().toLowerCase() + "' (allowed: " + list.stream().map(RegisterId::toString).collect(Collectors.joining(", ")) + ")", arguments[0].token);
            return new UnaryALUOp(keyword.toString().toLowerCase(), ((Argument.Register) arguments[0]).reg.toString());
        }
    }

}
