package com.lnfsutils.unpacker;

import com.lnfsutils.ILNFSBuilder;
import com.lnfsutils.LNFSException;
import com.lnfsutils.Logger;
import com.lnfsutils.lnfs.DataBlock;
import com.lnfsutils.lnfs.INode;
import com.lnfsutils.lnfs.LNFS;
import com.lnfsutils.lnfs.Superblock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LNFSUnpacker implements ILNFSBuilder {
    @Override
    public LNFS build(Path of) throws LNFSException, IOException {
        try{
            if(Files.isDirectory(of)){
                throw new LNFSException("LNFS file must be a single file");
            }
            byte[] data = Files.readAllBytes(of);
            Superblock sb = readSuperblock(data);

            Map<Integer, INode> inodes = parseInodeTable(sb, data);

            DataBlock rootDataBlock = parseDataBlocks(sb, inodes, data);

            return new LNFS(sb,
                    inodes.values().stream().sorted(Comparator.comparingInt(INode::index)).toArray(INode[]::new),
                    rootDataBlock);
        } catch (IOException e) {
            throw new IOException("Error reading file: " + e.getMessage(), e);
        } catch (ArrayIndexOutOfBoundsException e){
            throw new LNFSException("Invalid LNFS: " + e.getMessage());
        }
    }

    private static DataBlock parseDataBlocks(Superblock sb, Map<Integer, INode> inodes, byte[] data) throws LNFSException {
        DataBlock head = null, current = null;

        int i = sb.maxInodes() * 12 + 12;

        try{
            while (i < data.length) {
                int flags = data[i + 4];
                int ownerIndex = data[i + 5] & 0xFF;

                INode owner = inodes.get(ownerIndex);
                if(owner == null){
                    throw new LNFSException("invalid owner index (%d) for block at 0x%04x".formatted(ownerIndex, i));
                }

                int dataLen = (data[i + 6] << 8) | (data[i + 7] & 0xFF);
                byte[] blockData = Arrays.copyOfRange(data, i + 8, i + 8 + dataLen);

                int prevIndex = data[i] << 8 | data[i + 1] & 0xFF;
                int nextIndex = data[i + 2] << 8 | data[i + 3] & 0xFF;

                DataBlock newBlock = new DataBlock((flags & DataBlock.FLAG_OCCUPIED) == 0 ? null : owner, i, flags, blockData, current);

                if(newBlock.owner() != null && (newBlock.owner().flags() & INode.FLAG_DIRECTORY) != 0){
                    if(dataLen == 0){
                        throw new LNFSException("directory block with no data");
                    }
                    int childCount = blockData[0] & 0xFF;
                    if(dataLen < childCount + 1){
                        throw new LNFSException("directory block with invalid child count");
                    }
                    for(int j = 1; j <= childCount; j++){
                        INode child = inodes.get(blockData[j] & 0xFF);
                        if(child == null){
                            throw new LNFSException("invalid child index (%d) for block at 0x%04x".formatted(blockData[j] & 0xFF, i));
                        }
                        newBlock.owner().children().add(child);
                    }
                }

                if(current == null){
                    if(prevIndex != 0){
                        throw new LNFSException("invalid previous index (%d) for block at 0x%04x".formatted(prevIndex, i));
                    }
                }else if(prevIndex != current.offset()){
                    throw new LNFSException("invalid previous index (%d) for block at 0x%04x".formatted(prevIndex, i));
                }else{
                    current.next = newBlock;
                    newBlock.previous = current;
                }


                current = newBlock;

                if(head == null){
                    head = current;
                }

                if((owner.flags() & INode.FLAG_ROOT) == 0 && nextIndex == 0){
                    break;
                }

                i += 8 + dataLen;
            }

            if(head == null){
                throw new LNFSException("no data blocks found");
            }

            if(i != data.length){
                Logger.warning("remaining data after last data block");
            }

        }catch(ArrayIndexOutOfBoundsException e){
            throw new LNFSException(String.format("data block at 0x%04x is or extends outside of image bounds", i));
        }

        return head;
    }

    private static Map<Integer, INode> parseInodeTable(Superblock sb, byte[] data) throws LNFSException {
        int i = 0;
        Map<Integer,INode> inodes = new HashMap<>();
        boolean rootFound = false;
        try{
            for(; i < sb.maxInodes(); i++){
                if(!inodes.containsKey(i)){
                    INode inode = parseInode(inodes, data, i);

                    if(inode == null)
                        continue;

                    if(inode.parent() == null){
                        if((inode.flags() & INode.FLAG_ROOT) != 0){
                            if(rootFound){
                                throw new LNFSException("Multiple root inodes found");
                            }else if(inode.index() != 0){
                                throw new LNFSException("root inode index is not 0");
                            }
                            rootFound = true;
                        }
                    }else if((inode.flags() & INode.FLAG_ROOT) != 0){
                        throw new LNFSException("root inode has parent (index " + i + ")");
                    }

                    inodes.put(inode.index(), inode);
                }
            }
        }catch(ArrayIndexOutOfBoundsException e){
            throw new LNFSException("inode table outside of image bounds (index " + i + ")");
        }
        if(inodes.isEmpty()){
            throw new LNFSException("no inodes found");
        }
        return inodes;
    }

    private static INode parseInode(Map<Integer, INode> inodes, byte[] data, int i) throws LNFSException {

        if(inodes.containsKey(i))
            return inodes.get(i);

        int inodeStart = i * 12 + 12;
        byte flags = data[inodeStart + 9];
        if((flags & INode.FLAG_USED) != 0){
            String name = new String(data, inodeStart, 8);
            int indexOf0 = name.indexOf('\0');
            if(indexOf0 == -1){
                throw new LNFSException("invalid inode name (index " + i + ")");
            }

            name = name.substring(0, indexOf0);

            int parentIndex = data[inodeStart + 8];

            INode parent = null;

            if((flags & INode.FLAG_ROOT) == 0){
                // Root must have a parent
                parent = parseInode(inodes, data, parentIndex);
            }

            int blockOffset = (data[inodeStart + 10] << 8) | (data[inodeStart + 11] & 0xFF);

            return new INode(
                    i,
                    name,
                    parent,
                    flags,
                    (parent == null ? Path.of("./") : parent.ref()).resolve(name)
            );
        }
        return null;
    }

    private static Superblock readSuperblock(byte[] data) throws LNFSException {
        String sign = new String(data, 0 ,4);
        if(!sign.equals("LNFS")){
            throw new LNFSException("invalid superblock signature");
        }

        if(data[4] != LNFS.VERSION){
            throw new LNFSException("invalid version - " + data[4]);
        }

        int sizePages = data[5] & 0xFF;

        int maxInodes = data[7] & 0xFF;

        int firstFreeInode = data[9] & 0xFF;

        int flags = data[10] & 0xFF;

        if((data[11] & 0xFF)!= 0xA5){
            throw new LNFSException("invalid superblock magic byte");
        }

        return new Superblock(LNFS.VERSION, sizePages, maxInodes, firstFreeInode, flags);
    }
}
