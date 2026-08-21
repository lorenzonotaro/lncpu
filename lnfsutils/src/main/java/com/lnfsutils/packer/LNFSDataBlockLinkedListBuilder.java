package com.lnfsutils.packer;

import com.lnfsutils.LNFSException;
import com.lnfsutils.Main;
import com.lnfsutils.lnfs.DataBlock;
import com.lnfsutils.lnfs.INode;

import java.io.IOException;
import java.nio.file.Files;

public class LNFSDataBlockLinkedListBuilder {

    static Result buildLinkedList(INode root, int startAddress) throws LNFSException, IOException {
        return buildLinkedList(root, new Result(startAddress));
    }

    private static Result buildLinkedList(INode node, Result ll) throws IOException, LNFSException {
        byte[] data = getNodeData(node);
        DataBlock current = new DataBlock(node, ll.index, DataBlock.FLAG_OCCUPIED, data);

        if(ll.head == null){
            ll.head = current;
            ll.tail = current;
        }else{
            current.previous = ll.tail;
            ll.tail.next = current;
            ll.tail = current;
        }

        ll.index += data.length + 8; // 8 bytes for the header

        for (INode child : node.children()) {
            ll = buildLinkedList(child, ll);
        }

        return ll;
    }

    private static byte[] getNodeData(INode node) throws IOException {
        if((node.flags() & INode.FLAG_DIRECTORY) != 0){
            int childrenCount = node.children().size();
            byte[] data = new byte[Math.max(childrenCount, Main.programSettings.get("--min-folder-size", Double.class).intValue())];
            data[0] = (byte) (node.children().size());
            int i = 1;
            for(INode child : node.children()){
                data[i++] = (byte) child.index();
            }
            return data;
        }else{
            return Files.readAllBytes(node.ref());
        }
    }

    static class Result {
        DataBlock head, tail;
        int index;
        Result(int startAddress) {
            this.head = null;
            this.tail = null;
            this.index = startAddress;
        }
    }

}
