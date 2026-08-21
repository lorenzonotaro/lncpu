package com.lnfsutils.lnfs;

import com.lnfsutils.LNFSException;

import java.util.Objects;

public final class DataBlock {
    public static final int FLAG_OCCUPIED = 0x01;
    public static final int FLAG_IMMOVABLE = 0x02;
    private final int offset;
    private final INode owner;

    public DataBlock previous;
    public DataBlock next;
    private final int flags;
    private final byte[] data;

    public DataBlock(
            INode owner,

            int offset,

            int flags,

            byte[] data
    ) throws LNFSException {
        this.owner = owner;
        this.offset = offset;
        this.flags = flags;
        this.data = data;

        if(owner == null){
            if((flags & FLAG_OCCUPIED) != 0){
                throw new LNFSException("occupied data block with no owner");
            }
        }else{
            if(this.owner.dataBlock() != null)
                throw new LNFSException("owner already has a data block");
            this.owner.setDataBlock(this);
        }
    }

    public DataBlock(INode owner, int offset, int flags, byte[] data, DataBlock previous) throws LNFSException {
        this.flags = flags;
        this.data = data;
        this.owner = owner;
        this.offset = offset;
        this.previous = previous;


        if(owner == null){
            if((flags & FLAG_OCCUPIED) != 0){
                throw new LNFSException("occupied data block with no owner");
            }
        }else{
            if(this.owner.dataBlock() != null)
                throw new LNFSException("owner already has a data block");
            this.owner.setDataBlock(this);
        }
    }

    public int offset() {
        return offset;
    }

    public int flags() {
        return flags;
    }

    public INode owner() {
        return owner;
    }

    public byte[] data() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DataBlock) obj;
        return this.offset == that.offset &&
                Objects.equals(this.previous, that.previous) &&
                Objects.equals(this.next, that.next) &&
                this.flags == that.flags &&
                Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, previous, next, flags, data);
    }

    @Override
    public String toString() {
        return "DataBlock[" +
                "offset=" + offset + ", " +
                "flags=" + flags + ", " +
                "owner=" + owner + ']';
    }

}
