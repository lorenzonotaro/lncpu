package com.lnfsutils;

import com.lnfsutils.lnfs.LNFS;

import java.io.IOException;
import java.nio.file.Path;

public interface ILNFSBuilder {
    LNFS build(Path of) throws LNFSException, IOException;
}
