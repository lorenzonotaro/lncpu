package com.lnfsutils.lnfs;

import com.lnfsutils.LNFSException;

public class LNFS {
    public static final int VERSION = 1;
    public Superblock superblock;
    public INode[] inodes;
    public DataBlock rootDataBlock;

    public LNFS(Superblock superblock, INode[] inodes, DataBlock rootDataBlock) {
        this.superblock = superblock;
        this.inodes = inodes;
        this.rootDataBlock = rootDataBlock;
    }

    public void check() throws LNFSException {
        if(superblock.version() != VERSION)
            throw new LNFSException("Invalid version: " + superblock.version());

        if(inodes.length > superblock.maxInodes())
            throw new LNFSException("Number of inodes exceeds maximum allowed: " + inodes.length + " > " + superblock.maxInodes());

    }
}
