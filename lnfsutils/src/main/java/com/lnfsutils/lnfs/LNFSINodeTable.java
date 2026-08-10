package com.lnfsutils.lnfs;

import java.nio.file.Files;
import java.nio.file.Path;

import com.lnfsutils.LNFSException;

public class LNFSINodeTable {
    final LNFSINode root;
    final int inodesCount;

    private LNFSINodeTable(LNFSINode root, int inodesCount) {
        this.root = root;
        this.inodesCount = inodesCount;
    }

    public static LNFSINodeTable build(final Path rootDir) throws LNFSException {
        int count = 0;


        if(Files.isDirectory(rootDir)){
            final LNFSINode root = new LNFSDirectory(rootDir, "ROOT", null);

            root.setIndex(count++);

            count = buildDirectory(rootDir, root, count);

            return new LNFSINodeTable(root, count);
        }else{
            throw new LNFSException("Root path is not a directory: " + rootDir.toString());
        }
    }

    private static int buildDirectory(final Path dirPath, final LNFSINode parent, int count) throws LNFSException {
        try {
            for (final Path path : Files.newDirectoryStream(dirPath)) {
                final String name = path.getFileName().toString();
                
                LNFSINode node;

                if (Files.isDirectory(path)) {
                    node = new LNFSDirectory(path, name, parent);
                    node.setIndex(count++);
                    count = buildDirectory(path, node, count);
                } else {
                    node = new LNFSFile(path, name, parent);
                    node.setIndex(count++);
                }

                parent.addChild(node);
            }
        } catch (final Exception e) {
            throw new LNFSException("Error building directory structure: " + e.getMessage(), e);
        }
        return count;
    }
}