package com.lnc.assembler.parser.argument;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;

public class ReservedSpace extends EncodedData {
    private ReservedSpace(int length){
        super(new byte[length]);
    }

    public static CodeElement of(int size) {
        return new ReservedSpace(size);
    }

    public String toString(){
        return ".res " + this.data.length;
    }
}
