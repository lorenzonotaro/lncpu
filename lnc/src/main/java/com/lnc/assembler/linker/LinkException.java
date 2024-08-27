package com.lnc.assembler.linker;

import com.lnc.common.Logger;

public class LinkException extends RuntimeException{

    public LinkException(String message){
        super(message);
    }

    public LinkException(String message, Throwable cause){
        super(message, cause);
    }

    public void log() {
        Logger.error(this.getMessage());
    }
}
