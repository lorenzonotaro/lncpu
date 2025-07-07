package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.types.TypeSpecifier;

import java.util.*;

public class VirtualRegisterManager {


    private final Set<VirtualRegister> virtualRegisters;

    VirtualRegisterManager(){
        this.virtualRegisters = new HashSet<>();
    }


    public VirtualRegister getRegister(TypeSpecifier typeSpecifier){
        VirtualRegister vr = new VirtualRegister(virtualRegisters.size(), typeSpecifier);

        virtualRegisters.add(vr);

        return vr;
    }

    public Set<VirtualRegister> getAllRegisters() {
        return virtualRegisters;
    }
}
