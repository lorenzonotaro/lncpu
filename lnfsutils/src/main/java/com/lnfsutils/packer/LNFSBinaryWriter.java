package com.lnfsutils.packer;

import com.lnfsutils.ILNFSWriter;
import com.lnfsutils.Main;
import com.lnfsutils.lnfs.DataBlock;
import com.lnfsutils.lnfs.INode;
import com.lnfsutils.lnfs.LNFS;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

public class LNFSBinaryWriter implements ILNFSWriter {
    protected final OutputStream out;

    public LNFSBinaryWriter(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(LNFS lnfs) throws IOException {
        Charset charset = Charset.forName(Main.programSettings.get("--charset", String.class));
        // SUPERBLOCK
        out.write("LNFS".getBytes(charset));
        out.write(lnfs.superblock.version());
        out.write(lnfs.superblock.fsSizePages());
        out.write(0);
        out.write(lnfs.superblock.maxInodes());
        out.write(0);
        out.write(lnfs.superblock.firstFreeInode());
        out.write(lnfs.superblock.flags());
        out.write(0xA5);

        for (INode inode : lnfs.inodes) {
            out.write(inode.name().getBytes(charset));
            // write 0s to pad the name to 8 bytes
            out.write(new byte[8 - inode.name().length()]);

            out.write(inode.parent() != null ? inode.parent().index() : 0);

            out.write(inode.flags());

            out.write((inode.dataBlock().offset() >> 8));
            out.write(inode.dataBlock().offset() & 0xFF);
        }

        // pad until start of data blocks
        out.write(new byte[(lnfs.superblock.maxInodes() - lnfs.inodes.length) * 12]);

        DataBlock current = lnfs.rootDataBlock;

        while (current != null) {

            int previousOffset, nextOffset;

            if (current.previous != null) {
                previousOffset = current.previous.offset();
            } else {
                previousOffset = 0;
            }

            if (current.next != null) {
                nextOffset = current.next.offset();
            } else {
                nextOffset = 0;
            }

            out.write(previousOffset >> 8);
            out.write(previousOffset & 0xFF);

            out.write(nextOffset >> 8);
            out.write(nextOffset & 0xFF);

            out.write(current.flags());

            out.write(current.owner() != null ? current.owner().index() : 0);

            int dataLen = current.data().length;

            out.write(dataLen >> 8);
            out.write(dataLen & 0xFF);

            out.write(current.data());

            current = current.next;
        }
    }

    public OutputStream out() {
        return out;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LNFSBinaryWriter) obj;
        return Objects.equals(this.out, that.out);
    }

    @Override
    public int hashCode() {
        return Objects.hash(out);
    }

    @Override
    public String toString() {
        return "LNFSBinaryWriter[" +
                "out=" + out + ']';
    }

}