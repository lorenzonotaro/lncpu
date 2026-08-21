package com.lnfsutils.lnfs;

import com.lnfsutils.LNFSException;
import com.lnfsutils.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class INode {
    public static final int FLAG_USED = 0x01;
    public static final int FLAG_DIRECTORY = 0x02;
    public static final int FLAG_READONLY = 0x04;
    public static final int FLAG_HIDDEN = 0x08;
    public static final int FLAG_ROOT = 0x10;
    public static final int FLAG_EXECUTABLE = 0x20;

    private static final int FILENAME_MAX_LENGTH = 7;

    private final int index;
    private final String name;
    private final INode parent;
    private final List<INode> children;
    private final int flags;
    private final Path ref;
    private DataBlock dataBlock;

    public INode(
            int index,
            String name,
            INode parent,
            int flags,
            Path ref

    ) {
        this.index = index;

        if(name.length() > FILENAME_MAX_LENGTH){
            name = name.substring(0, FILENAME_MAX_LENGTH + 1);
            Logger.warning("Filename '" + name + "' exceeds maximum length. Truncated to '" + name + "'");
        }

        this.name = name;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.flags = flags;
        this.ref = ref;
    }

    public String name() {
        return name;
    }

    public INode parent() {
        return parent;
    }

    public int index() {
        return index;
    }

    public List<INode> children() {
        return children;
    }

    public int flags() {
        return flags;
    }

    public Path ref() {
        return ref;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (INode) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.parent, that.parent) &&
                Objects.equals(this.children, that.children) &&
                this.flags == that.flags &&
                Objects.equals(this.dataBlock, that.dataBlock) &&
                this.index == that.index &&
                Objects.equals(this.ref, that.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }

    @Override
    public String toString() {
        return "INode[" + getRelativePath() + "]";
    }

    private Path getRelativePath() {
        if(parent == null){
            return Path.of("/");
        }
        return parent.getRelativePath().resolve(name);
    }

    public DataBlock dataBlock() {
        return dataBlock;
    }

    public void setDataBlock(DataBlock dataBlock) {
        this.dataBlock = dataBlock;
    }
}
