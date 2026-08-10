package com.lnfsutils.lnfs;

import java.nio.file.Path;

public class LNFSDirectory extends LNFSINode {

    public LNFSDirectory(Path ref, String name, LNFSINode parent) {
        super(ref, name, parent);
        setFlag(FLAG_DIRECTORY);
    }

	@Override
	public byte[] getData() {
        byte[] bytes = new byte[children.size() + 1];

        bytes[0] = (byte) (children.size() & 0xFF);

        for (int i = 0; i < children.size(); i++) {
            bytes[i + 1] = (byte) (children.get(i).getIndex() & 0xFF);
        }

        return bytes;
	}
    
}
