package com.lnfsutils.lnfs;

import com.lnfsutils.LNFSException;

public class LNFSDataBlock {
    public static final byte FLAG_OCCUPIED = 0x01;
    LNFSDataBlock previous, next;
    LNFSINode inode;
    final byte[] data;
	private int startAddress; 

	/** Start of the data block from the start of the filesystem. */

    public LNFSDataBlock(LNFSINode inode, byte[] data, int index) {
        this.inode = inode;
        this.startAddress = index;
        this.data = data;
        if(inode != null) {
            inode.setDataBlock(this);
        }
    }

    public byte[] getData() {
        return data;
    }

    static LNFSDataBlockLinkedList buildLinkedList(LNFSINode root, int startAddress) throws LNFSException{
        return buildLinkedList(root, new LNFSDataBlockLinkedList(startAddress));
    }

    private static LNFSDataBlockLinkedList buildLinkedList(LNFSINode node, LNFSDataBlockLinkedList ll) throws LNFSException{
        byte[] dataBlock = node.getData();
        LNFSDataBlock current = new LNFSDataBlock(node, dataBlock, ll.index);
        
        if(ll.head == null){
            ll.head = current;
            ll.tail = current;
        }else{
            current.previous = ll.tail;
            ll.tail.next = current;
            ll.tail = current;
        }

        ll.index += dataBlock.length + 8; // 8 bytes for the header

        for (LNFSINode child : node.getChildren()) {
            ll = buildLinkedList(child, ll);
        }

        return ll;
    }

    public int getStartAddress() {
		return startAddress;
	}

    static class LNFSDataBlockLinkedList {
        LNFSDataBlock head, tail;
        int index;
        LNFSDataBlockLinkedList(int startAddress) {
            this.head = null;
            this.tail = null;
            this.index = startAddress;
        }
    }

}
