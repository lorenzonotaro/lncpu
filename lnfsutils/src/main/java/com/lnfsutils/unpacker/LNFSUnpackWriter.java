package com.lnfsutils.unpacker;

import com.lnfsutils.ILNFSWriter;
import com.lnfsutils.LNFSException;
import com.lnfsutils.lnfs.INode;
import com.lnfsutils.lnfs.LNFS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class LNFSUnpackWriter implements ILNFSWriter {

    private final Path outDir;

    public LNFSUnpackWriter(Path outDir) {
        this.outDir = outDir;
    }

    @Override
    public void write(LNFS lnfs) throws IOException, LNFSException {
        if(!Files.exists(outDir)){
            Files.createDirectories(outDir);
        }

        if(!Files.isDirectory(outDir)){
            throw new IOException("Output directory must be a directory");
        }

        try(var stream = Files.newDirectoryStream(outDir)){
            if(stream.iterator().hasNext()){
                throw new IOException("Output directory is not empty");
            }
        }

        if(lnfs.inodes.length == 0)
            throw new IOException("No inodes found");

        Set<INode> processed = new HashSet<>();

        processed.add(lnfs.inodes[0]);

        processChildren(lnfs.inodes[0], outDir, processed);
    }

    private void processChildren(INode inode, Path outDir, Set<INode> processed) throws LNFSException, IOException {
       if((inode.flags() & INode.FLAG_DIRECTORY) == 0){
           throw new IllegalArgumentException("Node is not a directory");
       }

       for(INode child : inode.children()){
           if(!processed.contains(child)){
               processInode(child, outDir, processed);
           }else{
               throw new LNFSException("Duplicate inode: " + child.name());
           }
       }
    }

    private void processInode(INode inode, Path parentDir, Set<INode> processed) throws IOException, LNFSException {
        processed.add(inode);
        Path outPath = parentDir.resolve(inode.name());
        if((inode.flags() & INode.FLAG_DIRECTORY) == 0){
            Files.write(outPath, inode.dataBlock().data());
        }else{
            Files.createDirectory(outPath);
            processChildren(inode, outPath, processed);
        }
    }
}
