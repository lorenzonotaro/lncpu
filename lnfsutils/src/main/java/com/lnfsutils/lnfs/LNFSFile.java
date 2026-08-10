package com.lnfsutils.lnfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.lnfsutils.LNFSException;

public class LNFSFile extends LNFSINode {

    public LNFSFile(Path ref, String name, LNFSINode parent) {
        super(ref, name, parent);
    }

    @Override
    public byte[] getData() throws LNFSException {
        try {
			return Files.readAllBytes(ref);
		} catch (IOException e) {
            throw new LNFSException("Error reading file data for inode: " + name + " at path: " + ref.toString() + ". " + e.getMessage(), e);
		}
    }
}
