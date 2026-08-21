package com.lnfsutils.lnfs;

public record Superblock(
        int version,
        int fsSizePages,
        int maxInodes,
        int firstFreeInode,
        int flags
) {
    public static final int FLAG_READONLY = 0x01;
}
