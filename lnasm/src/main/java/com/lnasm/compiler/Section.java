package com.lnasm.compiler;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.lnasm.io.ByteArrayChannel;

public class Section {

    @Expose
    public String name;

    @SerializedName("start")
    @Expose
    public short startAddress;

    @Expose
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
        @SerializedName("ROM")
        ROM,

        @SerializedName("RAM")
        RAM,

        @SerializedName("PAGE0")
        PAGE0
    }
}
