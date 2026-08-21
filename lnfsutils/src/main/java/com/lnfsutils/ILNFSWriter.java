package com.lnfsutils;

import com.lnfsutils.lnfs.LNFS;

import java.io.IOException;

public interface ILNFSWriter {
    void write(LNFS lnfs) throws IOException, LNFSException;
}
