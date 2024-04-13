package com.lnasm.compiler;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.lnasm.io.ByteArrayChannel;

public class Section {

    public String name;

    public short startAddress;

    public Type type;

    private final ByteArrayChannel channel;

    public Section(String name, short startAddress, Type type) {
        this();
        this.name = name;
        this.startAddress = startAddress;
        this.type = type;
    }

    public Section() {
        this.channel = new ByteArrayChannel(0, false);
    }

    public enum Type{
        ROM,

        RAM,

        PAGE0
    }
}
