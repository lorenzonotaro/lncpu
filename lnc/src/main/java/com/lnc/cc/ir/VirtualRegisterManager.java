package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.VirtualRegister;

import java.util.*;

public class VirtualRegisterManager {


    private final Set<VirtualRegister> virtualRegisters;

    VirtualRegisterManager(){
        this.virtualRegisters = new HashSet<>();
    }


    VirtualRegister getRegister(){
        return new VirtualRegister(virtualRegisters.size());
    }

    public Set<VirtualRegister> getAllRegisters() {
        return virtualRegisters;
    }
}
