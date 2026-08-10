package com.lnfsutils.lnfs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.lnfsutils.LNFSException;
import com.lnfsutils.Logger;
import com.lnfsutils.ProgramSettings;
import com.lnfsutils.lnfs.LNFSDataBlock.LNFSDataBlockLinkedList;

import java.nio.charset.Charset;
import java.nio.file.Path;
public class LNFSBuilder {

    private static final int LNFS_VERSION = 1;

	private static final int SUPERBLOCK_FLAG_READONLY = 0x01;

	private final ByteArrayOutputStream outputStream;

    private final int sizePages;

    private final int maxInodes;

    private final boolean readonly;

    private final Charset charset;

    public LNFSBuilder(ProgramSettings programSettings) throws LNFSException {
        this.sizePages = programSettings.get("--size", Double.class).intValue();
        this.maxInodes = programSettings.get("--max-inodes", Double.class).intValue();
        this.readonly = programSettings.get("--readonly", Boolean.class);

        if(sizePages <= 0)
            throw new LNFSException("Invalid size specified in program settings: " + sizePages);

        if(maxInodes <= 0 || maxInodes > 256)
            throw new LNFSException("Invalid max-inodes specified in program settings: " + maxInodes );
        this.charset = Charset.forName(programSettings.get("--charset", String.class));


        this.outputStream = new ByteArrayOutputStream(this.sizePages * 256);
    }

    public byte[] build(String string) throws LNFSException {
        this.outputStream.reset();
        
        LNFSINodeTable inodeTable = LNFSINodeTable.build(Path.of(string));
        

        if(inodeTable.inodesCount > this.maxInodes) {
            throw new LNFSException(String.format("Number of inodes exceeds maximum allowed: %d > %d", inodeTable.inodesCount, this.maxInodes));
        }

        LNFSDataBlockLinkedList linkedList = LNFSDataBlock.buildLinkedList(inodeTable.root, maxInodes * 12 + 12);

        if(linkedList.index < this.sizePages * 256 - 8){
            // append a free block at the end
            LNFSDataBlock freeBlock = new LNFSDataBlock(null, new byte[this.sizePages * 256 - linkedList.index - 8], linkedList.index);
            if(linkedList.tail != null){
                linkedList.tail.next = freeBlock;
                freeBlock.previous = linkedList.tail;
                linkedList.tail = freeBlock;
            }else{
                linkedList.head = freeBlock;
                linkedList.tail = freeBlock;
            }
        }

        LNFSDataBlock rootDataBlock = linkedList.head; /* each inode is 12 bytes in size. 12 bytes for the superblock. */

        try{

            writeSuperblock(inodeTable);

            writeInode(inodeTable.root);

            // Write remaining inode slots as empty
            for(int i = inodeTable.inodesCount; i < this.maxInodes; i++){
                outputStream.write(new byte[12]);
            }
            LNFSDataBlock current = rootDataBlock;
            while(current != null){
                
                if(outputStream.size() != current.getStartAddress())
                    throw new LNFSException(String.format("Data block start address mismatch: expected %d, got %d", current.getStartAddress(), outputStream.size()));
                
                int previousStart;

                if(current.previous == null){
                    previousStart = 0; // No previous block, so set to 0
                }else{
                    previousStart = current.previous.getStartAddress();
                }

                outputStream.write((previousStart >> 8) & 0xFF);
                outputStream.write(previousStart & 0xFF);

                int nextStart = current.next != null ? current.next.getStartAddress() : 0;

                if(nextStart > this.sizePages * 256 || (nextStart == this.sizePages * 256 && current.next != null)){
                    throw new LNFSException(String.format("Data block start address exceeds filesystem size: %d >= %d", nextStart, this.sizePages * 256));
                }else if(nextStart == this.sizePages * 256 && current.next == null){
                    // This is the last data block and it ends exactly at the end of the filesystem, which is valid.
                    nextStart = 0; // Indicate no next block
                }

                outputStream.write((nextStart >> 8) & 0xFF);
                outputStream.write(nextStart & 0xFF);

                outputStream.write(LNFSDataBlock.FLAG_OCCUPIED); // data block is occupied

                int inodeIndex = current.inode != null ? current.inode.getIndex() : 0;
                outputStream.write(inodeIndex);

                int dataLen = current.getData().length;
                outputStream.write((dataLen >> 8) & 0xFF);
                outputStream.write(dataLen & 0xFF);

                outputStream.write(current.getData());

                current = current.next;
            }

            if(outputStream.size() < this.sizePages * 256){
                
                int remaining = this.sizePages * 256 - outputStream.size();
                outputStream.write(new byte[remaining]);
            }
            else if (this.outputStream.size() > this.sizePages * 256)
                throw new LNFSException(String.format("Output exceeds specified size limit: %d > %d", this.outputStream.size(), this.sizePages * 256));
            return this.outputStream.toByteArray();
        }catch(IOException e){
            throw new LNFSException("Error building filesystem: " + e.getMessage(), e);
        }

    }

    private void writeSuperblock(LNFSINodeTable inodeTable) throws IOException {
        outputStream.write("LNFS".getBytes(this.charset));
        outputStream.write(LNFS_VERSION);
        outputStream.write(sizePages);
        outputStream.write(0); // reserved byte
        outputStream.write(maxInodes);
        outputStream.write(0); // reserved byte
        outputStream.write(inodeTable.inodesCount);
        outputStream.write(readonly ? SUPERBLOCK_FLAG_READONLY : 0);
        outputStream.write(0xA5); // magic number
	}

	private void writeInode(LNFSINode node) throws IOException {
        
        String name = node.getName();
        if (name.length() > 7) {
            name = name.substring(0, 7);
            Logger.warning(String.format("'%s' exceeds maximum filename length (7). Truncated to '%s'", node.ref.toAbsolutePath().toString(), name));
        }


        outputStream.write(name.getBytes(this.charset));
        // pad the name to 8 bytes with null bytes
        for (int i = name.length(); i < 8; i++) {
            outputStream.write(0);
        }

        int parentIndex = (node.getParent() != null) ? node.getParent().getIndex() : 0;
        outputStream.write(parentIndex);

        outputStream.write(node.getFlags() & LNFSINode.FLAG_OCCUPIED);

        int dataBlockAddress = node.getDataBlock().getStartAddress();
        outputStream.write((dataBlockAddress >> 8) & 0xFF);
        outputStream.write(dataBlockAddress & 0xFF);

        for (LNFSINode child : node.getChildren()) {
            writeInode(child);
        }
    }
}
