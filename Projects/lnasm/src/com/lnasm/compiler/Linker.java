package com.lnasm.compiler;

import com.lnasm.io.ByteArrayChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

public class Linker {

    private final ByteArrayChannel channel;
    private Set<Segment> segments;
    private boolean success;

    Linker(){
        this.channel = new ByteArrayChannel(0, false);
    }

    boolean link(Set<Segment> segments){
        this.segments = segments;
        this.success = true;
        for (Segment segment : segments) {
            linkSegment(segment);
        }
        return this.success;
    }

    private void linkSegment(Segment segment) {
        try {
            this.channel.position(segment.startAddress);
            for (Encodeable encodeable : segment.encodeables) {
                try{
                    this.channel.write(ByteBuffer.wrap(encodeable.encode(this, segment)));
                }catch(CompileException e){
                    e.log();
                    this.success = false;
                }
            }
        } catch (IOException e1) {
            throw new Error("I/O while linking" , e1);
        }
    }

    public byte resolveLabel(byte cs, String labelName, Token token) {
        Segment segment = segments.stream().filter(s -> s.csIndex == cs).findFirst().orElse(null);
        if(segment == null)
            throw new CompileException("undefined code segment " + cs + "", token);
        return segment.resolveLabel(labelName, token);
    }

    public byte[] getOutput() {
        return channel.toByteArray();
    }
}
