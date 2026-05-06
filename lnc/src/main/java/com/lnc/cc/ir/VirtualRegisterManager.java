package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.operands.CompoundVirtualRegister;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.types.TypeSpecifier;

import java.util.*;

/**
 * The VirtualRegisterManager class is responsible for managing a collection of virtual registers.
 * Virtual registers are dynamically allocated based on the specified type information and are
 * associated with specific register classes based on their allocation size.
 *
 * For 2-byte types, compound registers are represented as CompoundVirtualRegister wrapping
 * two simple VirtualRegisters with independent live ranges and allocation.
 */
public class VirtualRegisterManager {

    private final Set<VirtualRegister> virtualRegisters;

    VirtualRegisterManager(){
        this.virtualRegisters = new HashSet<>();
    }


    public IROperand getRegister(TypeSpecifier typeSpecifier){
        if(typeSpecifier.allocSize() == 0) {
            throw new IllegalArgumentException("TypeSpecifier must have a non-zero allocation size.");
        }else if(typeSpecifier.allocSize() == 1) {
            VirtualRegister vr = new VirtualRegister(virtualRegisters.size(), typeSpecifier);
            vr.setRegisterClass(RegisterClass.ANY);
            virtualRegisters.add(vr);
            return vr;
        }else if(typeSpecifier.allocSize() == 2) {
            // Create two simple 1-byte virtual registers for the high and low bytes
            VirtualRegister high = new VirtualRegister(virtualRegisters.size(), typeSpecifier);
            high.setRegisterClass(RegisterClass.ANY);
            virtualRegisters.add(high);

            VirtualRegister low = new VirtualRegister(virtualRegisters.size(), typeSpecifier);
            low.setRegisterClass(RegisterClass.ANY);
            virtualRegisters.add(low);

            // Wrap them in a compound virtual register
            return new CompoundVirtualRegister(high, low, typeSpecifier);
        }else{
            throw new IllegalArgumentException("Given type cannot safely reside in register.");
        }
    }

    public Set<VirtualRegister> getAllRegisters() {
        return virtualRegisters;
    }

    public void clearAssignedPhysicalRegisters() {
        for (VirtualRegister vr : virtualRegisters) {
            vr.setAssignedPhysicalRegister(null);
        }
    }
}
