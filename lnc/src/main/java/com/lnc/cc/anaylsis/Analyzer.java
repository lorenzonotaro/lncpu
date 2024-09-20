package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.Declaration;

public class Analyzer {

    private final LocalResolver localResolver;

    public Analyzer() {
        localResolver = new LocalResolver();
    }


    public boolean analize(Declaration[] result) {
        return localResolver.resolveLocals(result);
    }
}
