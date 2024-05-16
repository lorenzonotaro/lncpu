package com.lnasm.compiler.linker;

import com.lnasm.Logger;

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
