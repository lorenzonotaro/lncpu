package com.lnc.cc.ir;

import com.lnc.cc.ast.VariableDeclaration;
import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.types.TypeSpecifier;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CallingConvention {
    /** One parameter’s or return’s location. */
    public record ParamLocation(
            String name,
            TypeSpecifier type,
            RegisterClass regClass,   // null if it lives on the stack
            int size,
            boolean onStack,          // true ⇾ stack
            int stackOffset           // byte offset from SP (positive) for loads/pushes
    ) {}

    /** Returns the calling‐convention mapping for a call’s actual arguments. */
    public static List<ParamLocation> mapCallArguments(VariableDeclaration[] params) {
        List<ParamLocation> locs = new ArrayList<>();
        boolean hasWordArg = Arrays.stream(params)
                .anyMatch(op -> op.declarator.typeSpecifier().allocSize() == 2);

        int byteIdx = 0;          // counts only 1-byte args so far
        int stackOffset = 0;      // in bytes

        for (VariableDeclaration param : params) {
            var type = param.declarator.typeSpecifier();
            var name = param.name.lexeme;
            int size = type.allocSize();
            if (size == 2) {
                // A word‐sized argument
                if (locs.stream().noneMatch(pl -> pl.regClass == RegisterClass.WORDPARAM_1)) {
                    // first word → RC:RD
                    locs.add(new ParamLocation(name, type, RegisterClass.WORDPARAM_1, size, false, -1));
                } else {
                    // further words → stack (2 bytes)
                    locs.add(new ParamLocation(name, type, null, size, true, stackOffset));
                    stackOffset += 2;
                }

            } else {
                // A byte‐sized argument
                RegisterClass rc = switch (byteIdx) {
                    case 0 -> RegisterClass.BYTEPARAM_1;  // RA
                    case 1 -> RegisterClass.BYTEPARAM_2;  // RB
                    case 2 -> hasWordArg
                            ? null
                            : RegisterClass.BYTEPARAM_3;      // RC only if no word
                    case 3 -> hasWordArg
                            ? null
                            : RegisterClass.BYTEPARAM_4;      // RD only if no word
                    default -> null;
                };

                if (rc != null) {
                    locs.add(new ParamLocation(name, type, rc,  size,false, -1));
                } else {
                    // spill to stack (1 byte)
                    locs.add(new ParamLocation(name, type, null, size,true, stackOffset));
                    stackOffset += 1;
                }
                byteIdx++;
            }
        }

        return locs;
    }

    /** Pick the return register class for a given return type size. */
    public static RegisterClass returnRegisterFor(TypeSpecifier retType) {

        if(retType == null || retType.type == TypeSpecifier.Type.VOID) {
            return null;
        }

        return retType.allocSize() == 2
                ? RegisterClass.RET_WORD
                : RegisterClass.RET_BYTE;
    }
}
