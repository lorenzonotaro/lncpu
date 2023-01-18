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
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.POLL;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1 &&
                        arguments[0].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new PollRx(arguments[0]);
            }
        });
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
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.PUSH;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1
                        && arguments[0].type == Argument.Type.DEREFERENCE
                        && ((Argument.Dereference)arguments[0]).source == AddressSource.ROM
                        && ((Argument.Dereference)arguments[0]).value.type == Argument.Type.CONSTANT;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new PushDeref(arguments[0]);
            }
        });
        Matcher.addMatcher(new Matcher() {
                @Override
                public Token.Type getKeyword() {
                    return Token.Type.PUSH;
                }

                @Override
                public boolean matches(Argument... arguments) {
                    return arguments.length == 1
                            && arguments[0].type == Argument.Type.CONSTANT;
                }

                @Override
                public Encodeable make(Argument... arguments) {
                    return new PushConstant(arguments[0]);
                }
            });
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
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.REGISTER
                        && arguments[1].type == Argument.Type.REGISTER
                        && (((Argument.Register)arguments[0]).reg != ((Argument.Register)arguments[1]).reg);
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovRxRx(arguments[0], arguments[1]);
            }
        });
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.DEREFERENCE
                        && arguments[1].type == Argument.Type.DEREFERENCE
                        && ((Argument.Dereference)arguments[0]).value.type == Argument.Type.CONSTANT
                        && ((Argument.Dereference)arguments[1]).value.type == Argument.Type.CONSTANT
                        && ((Argument.Dereference)arguments[0]).source == AddressSource.ROM
                        && ((Argument.Dereference)arguments[1]).source != AddressSource.ROM;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovConstantDerefConstantDeref(arguments[0], arguments[1]);
            }
        });
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.DEREFERENCE
                        && arguments[1].type == Argument.Type.REGISTER
                        && ((Argument.Dereference)arguments[0]).value.type == Argument.Type.CONSTANT;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovConstantDerefRx(arguments[0], arguments[1]);
            }
        });
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.CONSTANT
                        && arguments[1].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovConstantRx(arguments[0], arguments[1]);
            }
        });
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.REGISTER
                        && arguments[1].type == Argument.Type.DEREFERENCE
                        && ((Argument.Dereference)arguments[1]).source != AddressSource.ROM
                        && ((Argument.Dereference)arguments[1]).value.type == Argument.Type.CONSTANT;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovRxConstantDeref(arguments[0], arguments[1]);
            }
        });
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.DEREFERENCE
                        && arguments[1].type == Argument.Type.REGISTER
                        && ((Argument.Dereference)arguments[0]).value.type == Argument.Type.REGISTER
                        && ((Argument.Dereference)arguments[1]).source != AddressSource.ROM;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovRxDerefRx(arguments[0], arguments[1]);
            }
        });
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.REGISTER
                        && arguments[1].type == Argument.Type.DEREFERENCE
                        && ((Argument.Dereference)arguments[1]).value.type == Argument.Type.REGISTER
                        && ((Argument.Dereference)arguments[1]).source != AddressSource.ROM
                        && ((Argument.Register)arguments[0]).reg != ((Argument.Register)((Argument.Dereference)arguments[1]).value).reg;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovRxRxDeref(arguments[0], arguments[1]);
            }
        });
        Matcher.addMatcher(new ShortJump.SJumpInstrMatcher(Token.Type.JC));
        Matcher.addMatcher(new ShortJump.SJumpInstrMatcher(Token.Type.JN));
        Matcher.addMatcher(new ShortJump.SJumpInstrMatcher(Token.Type.JZ));
        Matcher.addMatcher(new ShortJump.SJumpInstrMatcher(Token.Type.JA));
        Matcher.addMatcher(new ShortJump.SJumpInstrMatcher(Token.Type.GOTO));
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.JC));
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.JN));
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.JZ));
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.JA));
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.GOTO));
        Matcher.addMatcher(new LongJump.LJumpInstrMatcher(Token.Type.CALL));
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
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.TSM;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1 &&
                        arguments[0].type == Argument.Type.CONSTANT;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new TsmConstant(arguments[0]);
            }
        });
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
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.TSM;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1 &&
                        arguments[0].type == Argument.Type.DEREFERENCE &&
                        ((Argument.Dereference)arguments[0]).value.type == Argument.Type.CONSTANT;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new TsmConstantDeref(arguments[0]);
            }
        });
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.TSM;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 1 &&
                        arguments[0].type == Argument.Type.REGISTER;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new TsmRx(arguments[0]);
            }
        });
        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.NOP));
        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.HLT));
        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.RET));
        Matcher.addMatcher(new NoArgumentInstr.NoArgumentInstrMatcher(Token.Type.RFL));
    }
}
