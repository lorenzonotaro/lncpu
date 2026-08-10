package com.lnfsutils.lnfs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.lnfsutils.LNFSException;

public abstract class LNFSINode {

    public static final int FLAG_OCCUPIED = 0x01;
    public static final int FLAG_DIRECTORY = 0x02;
    public static final int FLAG_READONLY = 0x04;
    public static final int FLAG_HIDDEN = 0x08;
    public static final int FLAG_ROOT = 0x10;
    public static final int FLAG_EXECUTABLE = 0x20;

    private int index = -1;

    protected String name;
    
    protected int flags;

    private final LNFSINode parent;

    protected final List<LNFSINode> children;

    protected final Path ref;

    private LNFSDataBlock dataBlock;


	protected LNFSINode(Path ref, String name, LNFSINode parent) {
        this.ref = ref;
        this.name = name;
        this.parent = parent;
        this.children = new ArrayList<>();

        if(parent == null){
            setFlag(FLAG_ROOT);
        }
    }


    public abstract byte[] getData() throws LNFSException;


    public final void setIndex(int index) {
        if (this.index != -1)
            throw new IllegalStateException("Index already set for this inode.");
        this.index = index;
    }

    public final int getIndex() {
        return index;
    }

    public void setFlag(int flagMask){
        this.flags |= flagMask;
    }

    public final void clearFlag(int flagMask){
        this.flags &= ~flagMask;
    }

    public final int getFlags() {
        return flags;
    }

    public void addChild(LNFSINode child) {
        
        if ((this.flags & FLAG_DIRECTORY) == 0) {
            throw new IllegalStateException("Cannot add child to a non-directory node.");
        }

        if (child == null) {
            throw new IllegalArgumentException("Child cannot be null.");
        }
        
        if (child.parent != this) {
            throw new IllegalArgumentException("Child's parent does not match this node.");
        }
        
        children.add(child);
    }

    public final List<LNFSINode> getChildren() {
        return children;
    }

    public final LNFSINode getParent() {
        return parent;
    }


    public String getName() {
        return name;
    }


    public void setDataBlock(LNFSDataBlock dataBlock) {
        if (this.dataBlock != null) {
            throw new IllegalStateException("Data block already set for this inode.");
        }
		this.dataBlock = dataBlock;
	}

    public LNFSDataBlock getDataBlockObject() {
        return dataBlock;
    }

    public LNFSDataBlock getDataBlock() {
        return dataBlock;
    }
} 