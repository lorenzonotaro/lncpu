package com.lnfsutils.packer;

import com.lnfsutils.ILNFSWriter;
import com.lnfsutils.lnfs.LNFS;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LNFSFileWriter extends LNFSBinaryWriter implements AutoCloseable{
    public LNFSFileWriter(Path outFile) throws IOException {
        super(Files.newOutputStream(outFile));
    }

    @Override
    public void close() throws IOException {
        this.out.close();
    }
}