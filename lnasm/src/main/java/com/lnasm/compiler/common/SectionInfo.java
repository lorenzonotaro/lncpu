package com.lnasm.compiler.common;

public class SectionInfo {
    private static final int PAGE_0_START = 0x2000;
    private final SectionMode mode;
    private final String name;
    private final int start;
    private final int maxSize;
    private final SectionType type;

    public SectionInfo(String name, int start, SectionType type, SectionMode mode) {

        if(name == null){
            throw new IllegalArgumentException("null section name");
        }

        if(type == null){
            throw new IllegalArgumentException("null section type");
        }

        this.name = name;
        this.type = type;

        if(type == SectionType.PAGE0){
            if(start != PAGE_0_START && start != -1){
                throw new IllegalArgumentException("page0 must start at 0x2000");
            }

            if(mode != SectionMode.FIXED && mode != null){
                throw new IllegalArgumentException("page0 must be fixed");
            }

            this.start = PAGE_0_START;
            this.mode = SectionMode.FIXED;
            this.maxSize = 0xFF;
        }else{
            if(mode == null || mode == SectionMode.FIXED){
                if(start == -1){
                    throw new IllegalArgumentException("fixed section must have a start address");
                }

                this.start = start;
                this.mode = SectionMode.FIXED;
                this.maxSize = SectionMode.FIXED.getMaxSize();
            }else{
                if(start != -1){
                    throw new IllegalArgumentException("non-fixed section cannot have a start address");
                }

                this.start = -1;
                this.mode = mode;
                this.maxSize = mode.getMaxSize();
            }
        }

    }

    public String toString() {
        return "SectionInfo{" +
                "name='" + getName() + '\'' +
                ", start=" + getStart() +
                ", type=" + getType() +
                '}';
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

    public SectionType getType() {
        return type;
    }

    public SectionMode getMode() {
        return mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SectionInfo that = (SectionInfo) o;

        if (start != that.start) return false;
        if (maxSize != that.maxSize) return false;
        if (mode != that.mode) return false;
        if (!name.equals(that.name)) return false;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = mode.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + start;
        result = 31 * result + maxSize;
        result = 31 * result + type.hashCode();
        return result;
    }
}
