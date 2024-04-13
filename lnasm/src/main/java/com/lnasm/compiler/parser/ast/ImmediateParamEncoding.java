package com.lnasm.compiler.parser.ast;

import com.lnasm.compiler.CompileException;
import com.lnasm.compiler.parser.RegisterId;

class ImmediateParamEncoding {
    byte[] args;
    String immediateName;

    public ImmediateParamEncoding(Argument arg) {
        switch (arg.type) {
            case BYTE:
                this.args = new byte[]{((Argument.Byte) arg).value};
                this.immediateName = "cst";
                break;
            case DEREFERENCE:
                dereference(((Argument.Dereference) arg).value);
                break;
            case L_ADDRESS:
                throw new CompileException("invalid argument", arg.token);
            case REGISTER:
                this.args = new byte[0];
                this.immediateName = arg.token.lexeme.toLowerCase();
                break;
            default:
                throw new Error("invalid argument");
        }
    }

    private void dereference(Argument arg) {
        switch (arg.type) {
            case REGISTER -> {
                //can only be RD
                if (((Argument.Register) arg).reg != RegisterId.RD)
                    throw new CompileException("Indirect page 0 mode is only permitted with RD", arg.token);
                this.immediateName = "ipage0rd";
                this.args = new byte[0];
            }
            case L_ADDRESS -> {
                //pop [page:adr], [page:rd] or [rc:rd]
                Argument.LongAddress la = (Argument.LongAddress) arg;

                Argument.Type highType = la.high.type;
                Argument.Type lowType = la.low.type;

                if (highType == Argument.Type.REGISTER && lowType == Argument.Type.REGISTER && ((Argument.Register) la.high).reg == RegisterId.RC && ((Argument.Register) la.low).reg == RegisterId.RD) {
                    //this.encoded = new byte[]{OpcodeMap.getOpcode("pop_ifullrcrd")};
                    this.immediateName = "ifullrcrd";
                    this.args = new byte[0];
                } else if (highType == Argument.Type.BYTE && lowType == Argument.Type.BYTE) {
                    //this.encoded = new byte[]{OpcodeMap.getOpcode("pop_abs"), ((Argument.Byte)la.high).value, ((Argument.Byte)la.low).value};
                    this.immediateName = "abs";
                    this.args = new byte[]{((Argument.Byte) la.high).value, ((Argument.Byte) la.low).value};
                } else {
                    throw new CompileException("invalid indirect argument", arg.token);
                }
            }
            case BYTE -> {
                byte val = ((Argument.Byte) arg).value;
//                    this.encoded = new byte[]{OpcodeMap.getOpcode("pop_page0"), val};
                this.immediateName = "page0";
                this.args = new byte[]{val};
            }
            case WORD -> {
                short val = ((Argument.Word) arg).value;
//                    this.encoded = new byte[]{OpcodeMap.getOpcode("pop_abs"), (byte) (val >> 8), (byte) val};
                this.immediateName = "abs";
                this.args = new byte[]{(byte) (val >> 8), (byte) val};
            }
            default -> throw new CompileException("invalid indirect argument", arg.token);
        }
    }
}
