package com.lnasm.compiler.parser.ast;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.lexer.Token;
import com.lnasm.compiler.linker.AbstractLinker;
import com.lnasm.compiler.common.Encodeable;
import com.lnasm.compiler.parser.LnasmParser;

class Swap extends NoArgumentInstr{

    public Swap(Token token, String instr) {
        super(token, instr);
    }

    @Override
    public byte[] encode(AbstractLinker linker, short addr) {
        try{
            return super.encode(linker, addr);
        }catch(CompileException e){
            throw new CompileException("invalid swap pair", this.token);
        }
    }

    static class SwapMatcher implements Matcher {
        @Override
        public Token.Type getKeyword() {
            return Token.Type.SWAP;
        }

        @Override
        public boolean matches(Argument... arguments) {
            return arguments.length == 2 && arguments[0].type == Argument.Type.REGISTER && arguments[1].type == Argument.Type.REGISTER;
        }

        @Override
        public Encodeable make(LnasmParser parser, Token instructionToken, Argument... arguments) {
            String reg1 = ((Argument.Register) arguments[0]).reg.toString();
            String reg2 = ((Argument.Register) arguments[1]).reg.toString();

            String instr = "swap_";

            int compare = reg1.compareTo(reg2);
            if (compare < 0) {
                instr += reg1 + "_" + reg2;
            } else if (compare > 0) {
                instr += reg2 + "_" + reg1;
            } else
                throw new CompileException("cannot swap a register with itself", arguments[0].token);
            return new Swap(instructionToken, instr);
        }
    }

}
