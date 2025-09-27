package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.types.TypeSpecifier;

import java.util.*;

/**
 * The VirtualRegisterManager class is responsible for managing a collection of virtual registers.
 * Virtual registers are dynamically allocated based on the specified type information and are
 * associated with specific register classes based on their allocation size.
 */
public class VirtualRegisterManager {


    private final Set<VirtualRegister> virtualRegisters;

    VirtualRegisterManager(){
        this.virtualRegisters = new HashSet<>();
    }


    public VirtualRegister getRegister(TypeSpecifier typeSpecifier){
        VirtualRegister vr = new VirtualRegister(virtualRegisters.size(), typeSpecifier);

        if(typeSpecifier.allocSize() == 0) {
            throw new IllegalArgumentException("TypeSpecifier must have a non-zero allocation size.");
        }else if(typeSpecifier.allocSize() == 1) {
            vr.setRegisterClass(RegisterClass.ANY);
        }else if(typeSpecifier.allocSize() == 2) {
            vr.setRegisterClass(RegisterClass.WORD);
        }else{
            throw new IllegalArgumentException("Given type cannot safely reside in register.");
        }
        virtualRegisters.add(vr);

        return vr;
    }

    public Set<VirtualRegister> getAllRegisters() {
        return virtualRegisters;
    }
}
