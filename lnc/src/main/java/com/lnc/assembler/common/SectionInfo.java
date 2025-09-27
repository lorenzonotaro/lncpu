package com.lnc.assembler.common;

import com.lnc.assembler.linker.LinkTarget;

import java.io.Serializable;

/**
 * Represents a section configuration with defined properties such as name, start address,
 * mode, target, and various flags indicating specific behaviors.
 *
 * This class encapsulates the information and constraints associated with a memory or data
 * section, ensuring proper initialization and validation of parameters.
 *
 */
public class SectionInfo implements Serializable {
    private final LinkMode mode;
    private final String name;
    private final int start;
    private final int maxSize;
    private final LinkTarget target;
    private final boolean multiWriteAllowed;
    private final boolean dataPage, virtual;

    public SectionInfo(String name, int start, LinkTarget target, LinkMode mode, boolean multiWriteAllowed, boolean dataPage, boolean virtual) {

        int maxSize;

        if(name == null)
            throw new IllegalArgumentException("empty section name");

        if(virtual && (!dataPage || start != -1 || target != null || mode != null))
            throw new IllegalArgumentException("virtual only applies to data pages. Virtual sections cannot have a target, mode or start address");

        // fixed or no mode specified, start address must be specified
        if(start != -1 && (mode == LinkMode.FIXED || mode == null)){
            mode = LinkMode.FIXED;
            if (target == null) {
                target = LinkTarget.fromAddress(start);
            }else if(!target.contains(start)){
                throw new IllegalArgumentException("start address (%04x) is outside of target address space (%04x-%04x)".formatted(start, target.start, target.end));
            }

            if(dataPage){

                if((start & 0xFF) != 0)
                    throw new IllegalArgumentException("data page sections must start at a page boundary");

                maxSize = 0x100;
            }else{
                maxSize = target.end - start + 1;
            }
        }else if(mode != LinkMode.FIXED && mode != null){
            if(target == null)
                throw new IllegalArgumentException("target address space must be specified for mode %s".formatted(mode));

            if(start != -1)
                throw new IllegalArgumentException("start address cannot be specified for mode %s".formatted(mode));

            if(dataPage){
                if(mode != LinkMode.PAGE_ALIGN)
                    throw new IllegalArgumentException("data page sections must use mode page_align");
                maxSize = 0x100;
            }else{
                maxSize = target.getMaxSize();
            }
        }else if (dataPage && virtual){
            maxSize = 0x100;
            target = LinkTarget.__VIRTUAL__;
        }else{
            throw new IllegalArgumentException("invalid section configuration");
        }

        this.maxSize = maxSize;
        this.name = name;
        this.multiWriteAllowed = multiWriteAllowed;
        this.mode = mode;
        this.target = target;
        this.start = start;
        this.dataPage = dataPage;
        this.virtual = virtual;

    }

    public String getName() {
        return name;
    }

    public int getStart() {
        return start;
    }

    public int getMaxSize() {
        return maxSize;
    }


    public LinkMode getMode() {
        return mode;
    }

    public boolean isMultiWriteAllowed() {
        return multiWriteAllowed;
    }

    public boolean isDataPage() {
        return dataPage;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public LinkTarget getTarget() {
        return target;
    }

    public String serialize(){
        return "%s,%s,%s,%s,%s,%s,%s".formatted(
                name,
                start == -1 ? "null" : String.format("0x%04X", start),
                target == null ? "null" : target.toString(),
                mode == null ? "null" : mode.name().toLowerCase(),
                multiWriteAllowed,
                dataPage,
                virtual
        );
    }
    public SectionInfo deserialize(String line){
        var parts = line.split(",");
        if(parts.length != 7)
            throw new IllegalArgumentException("invalid section info format");

        String name = parts[0].trim();
        int start = parts[1].trim().equals("null") ? -1 : Integer.parseInt(parts[1].trim().substring(2), 16);
        LinkTarget target = parts[2].trim().equals("null") ? null : LinkTarget.valueOf(parts[2].trim());
        LinkMode mode = parts[3].trim().equals("null") ? null : LinkMode.valueOf(parts[3].trim().toUpperCase());
        boolean multiWriteAllowed = Boolean.parseBoolean(parts[4].trim());
        boolean dataPage = Boolean.parseBoolean(parts[5].trim());
        boolean virtual = Boolean.parseBoolean(parts[6].trim());

        return new SectionInfo(name, start, target, mode, multiWriteAllowed, dataPage, virtual);
    }
}
