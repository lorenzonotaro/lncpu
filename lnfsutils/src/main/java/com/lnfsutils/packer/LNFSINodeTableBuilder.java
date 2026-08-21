package com.lnfsutils.packer;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.lnfsutils.LNFSException;
import com.lnfsutils.lnfs.INode;

public class LNFSINodeTableBuilder {
    public static INode[] build(final Path rootDir) throws LNFSException {
        int count = 0;

        List<INode> inodes = new ArrayList<>();

        if(Files.isDirectory(rootDir)){
            final INode root = new INode(count++, "ROOT", null, INode.FLAG_DIRECTORY | INode.FLAG_ROOT | INode.FLAG_USED, rootDir);

            inodes.add(root);

            count = buildDirectory(inodes, rootDir, root, count);

            if(count != inodes.size()){
                throw new LNFSException("Number of inodes does not match the number of files: " + count + " != " + inodes.size());
            }

            return inodes.stream().sorted(Comparator.comparingInt(INode::index)).toArray(INode[]::new);
        }else{
            throw new LNFSException("Root path is not a directory: " + rootDir.toString());
        }
    }

    private static int buildDirectory(List<INode> inodes, final Path dirPath, final INode parent, int count) throws LNFSException {
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(dirPath)){
            for (final Path path : paths) {
                final String name = path.getFileName().toString();
                
                INode node;

                if (Files.isDirectory(path)) {
                    node = new INode(count++, name, parent, INode.FLAG_DIRECTORY | INode.FLAG_USED, path);
                    count = buildDirectory(inodes, path, node, count);
                } else {
                    node = new INode(count++, name, parent, INode.FLAG_USED, path);
                }

                inodes.add(node);
                parent.children().add(node);
            }
        } catch (final Exception e) {
            throw new LNFSException("Error building directory structure: " + e.getMessage(), e);
        }
        return count;
    }
}