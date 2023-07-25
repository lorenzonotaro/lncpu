package com.lnasm.compiler.ast;

import com.lnasm.compiler.Encodeable;
import com.lnasm.compiler.Token;

import java.util.HashSet;
import java.util.Set;

public interface Matcher {

    Set<Matcher> matchers = new HashSet<>();

    static Set<Matcher> getMatchers() {
        return matchers;
    }

    static void addMatcher(Matcher matcher){
        matchers.add(matcher);
    }


    Token.Type getKeyword();

    boolean matches(Argument... arguments);

    Encodeable make(Argument... arguments);

    static void init(){
        //ADD
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.ADD;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.REGISTER
                        && arguments[1].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new AddRxRx(arguments[0], arguments[1]);
            }
        });
        //AND
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.AND;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.REGISTER
                        && arguments[1].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new AndRxRx(arguments[0], arguments[1]);
            }
        });
        //NOT
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.NOT;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1 &&
                        arguments[0].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new NotRx(arguments[0]);
            }
        });
        //OR
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.OR;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.REGISTER
                        && arguments[1].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new OrRxRx(arguments[0], arguments[1]);
            }
        });
        //POP RX
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.POP;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1 &&
                        arguments[0].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new PopRx(arguments[0]);
            }
        });
        //POP [X]
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.POP;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1 &&
                        arguments[0].type == Argument.Type.DEREFERENCE;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new PopIndirect(arguments[0]);
            }
        });
        //PUSH RX
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.PUSH;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1 && arguments[0].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new PushRx(arguments[0]);
            }
        });
        //PUSH [X]
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.PUSH;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1
                        && arguments[0].type == Argument.Type.DEREFERENCE;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new PushIndirect(((Argument.Dereference) arguments[0]).value);
            }
        });
        //PUSH X
        Matcher.addMatcher(new Matcher() {
                @Override
                public Token.Type getKeyword() {
                    return Token.Type.PUSH;
                }

                @Override
                public boolean matches(Argument... arguments) {
                    return arguments.length == 1
                            && arguments[0].type == Argument.Type.BYTE;
                }

                @Override
                public Encodeable make(Argument... arguments) {
                    return new PushConstant(arguments[0]);
                }
            });
        //SHL
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.SHL;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1 &&
                        arguments[0].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new Shl(arguments[0]);
            }
        });
        //SHR
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.SHR;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1 &&
                        arguments[0].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new Shr(arguments[0]);
            }
        });
        //MOV X, RX
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.BYTE
                        && arguments[1].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovConstantRx(arguments[0], arguments[1]);
            }
        });
        //MOV RX, RX
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.REGISTER
                        && arguments[1].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovRxRx(arguments[0], arguments[1]);
            }
        });
        //MOV with any indirect addressing
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && (arguments[0].type == Argument.Type.DEREFERENCE || arguments[1].type == Argument.Type.DEREFERENCE);
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovIndirect(arguments[0], arguments[1]);
            }
        });

        //Long jumps
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.LJC));
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.LJN));
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.LJZ));
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.LGOTO));
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.LCALL));
        //Short jumps
        Matcher.addMatcher(new ShortJump.SJumpInstrMatcher(Token.Type.JC));
        Matcher.addMatcher(new ShortJump.SJumpInstrMatcher(Token.Type.JN));
        Matcher.addMatcher(new ShortJump.SJumpInstrMatcher(Token.Type.JZ));
        Matcher.addMatcher(new ShortJump.SJumpInstrMatcher(Token.Type.GOTO));

        //SUB
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.SUB;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.REGISTER
                        && arguments[1].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new SubRxRx(arguments[0], arguments[1]);
            }
        });
        //CMP
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.CMP;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.REGISTER
                        && arguments[1].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new Cmp(arguments[0], arguments[1]);
            }
        });
        //XOR
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.XOR;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.REGISTER
                        && arguments[1].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new XorRxRx(arguments[0], arguments[1]);
            }
        });

        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.NOP));
        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.HLT));
        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.RET));
        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.IRET));
        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.SID));
        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.CID));
        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.BRK));
    }
}
