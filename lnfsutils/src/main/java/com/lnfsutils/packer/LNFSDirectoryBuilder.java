package com.lnfsutils.packer;

import java.io.IOException;

import com.lnfsutils.ILNFSBuilder;
import com.lnfsutils.LNFSException;
import com.lnfsutils.Logger;
import com.lnfsutils.ProgramSettings;
import com.lnfsutils.lnfs.DataBlock;
import com.lnfsutils.lnfs.INode;
import com.lnfsutils.lnfs.LNFS;
import com.lnfsutils.lnfs.Superblock;
import com.lnfsutils.packer.LNFSDataBlockLinkedListBuilder.Result;

import java.nio.file.Files;
import java.nio.file.Path;
public class LNFSDirectoryBuilder implements ILNFSBuilder {

    private static final int LNFS_VERSION = 1;

	private static final int SUPERBLOCK_FLAG_READONLY = 0x01;

    private final int sizePages;

    private final int maxInodes;

    private final boolean readonly;

    public LNFSDirectoryBuilder(ProgramSettings programSettings) throws LNFSException {
        this.sizePages = programSettings.get("--size", Double.class).intValue();
        this.maxInodes = programSettings.get("--max-inodes", Double.class).intValue();
        this.readonly = programSettings.get("--readonly", Boolean.class);

        if(sizePages <= 0)
            throw new LNFSException("Invalid size specified in program settings: " + sizePages);

        if(maxInodes <= 0 || maxInodes > 256)
            throw new LNFSException("Invalid max-inodes specified in program settings: " + maxInodes );

    }

    @Override
    public LNFS build(Path rootFolder) throws LNFSException, IOException {

        if(!Files.isDirectory(rootFolder)){
            throw new LNFSException("Root folder must be a directory");
        }

        INode[] inodeTable = LNFSINodeTableBuilder.build(rootFolder);

        if(inodeTable.length > this.maxInodes) {
            throw new LNFSException(String.format("Number of inodes exceeds maximum allowed: %d > %d", inodeTable.length, this.maxInodes));
        }

        Result linkedList = LNFSDataBlockLinkedListBuilder.buildLinkedList(inodeTable[0], maxInodes * 12 + 12);

        if(linkedList.index < this.sizePages * 256 - 8){
            // append a free block at the end
            DataBlock freeBlock = new DataBlock(null, linkedList.index, 0, new byte[this.sizePages * 256 - linkedList.index - 8]);
            if(linkedList.tail != null){
                linkedList.tail.next = freeBlock;
                freeBlock.previous = linkedList.tail;
            }else{
                linkedList.head = freeBlock;
            }
            linkedList.tail = freeBlock;
        }
        return new LNFS(
                new Superblock(LNFS_VERSION, sizePages, maxInodes, inodeTable.length, readonly ? Superblock.FLAG_READONLY : 0),
                inodeTable,
                linkedList.head
        );
    }
}
