package com.lnc.assembler.linker;

import java.util.Objects;

/**
 * MemoryLayoutManager is responsible for managing memory segments, organizing and allocating them
 * according to specified section requirements. It validates the memory layout, ensures continuity,
 * and handles different allocation strategies such as fixed, page-aligned, page-fit, and general fits.
 */
public class MemoryLayoutManager {


    private final LinkTarget device;

    private final Segment head;

    public MemoryLayoutManager(LinkTarget device) {
        this.device = device;
        head = new Segment(device.start, device.end);

    }

    public void allocate(SectionBuilder sectionBuilder){
        if(sectionBuilder.getSectionInfo().isVirtual()){
            sectionBuilder.setSectionStart(0);
        }else switch(sectionBuilder.getSectionInfo().getMode()){
            case FIXED -> allocateFixed(sectionBuilder);
            case PAGE_ALIGN -> allocatePageAlign(sectionBuilder);
            case PAGE_FIT -> allocatePageFit(sectionBuilder);
            case FIT -> allocateFit(sectionBuilder);
        }
    }

    private void allocatePageFit(SectionBuilder sectionBuilder) {
        Segment current = head;
        while(current != null){
            if(!current.isAllocated() && current.getSize() >= sectionBuilder.getCodeLength()){
                if((current.start & 0xFF00) == (current.start + sectionBuilder.getCodeLength() - 1 & 0xFF00)){
                    current.allocate(0, sectionBuilder);
                    return;
                }else if(allocatePageAlignedInSegment(current, sectionBuilder)){
                    return;
                }
            }
            current = current.next;
        }
        unableToPlace(sectionBuilder);
    }

    private void unableToPlace(SectionBuilder sectionBuilder) {
        throw new LinkException("unable to place section '%s' (%s)".formatted(sectionBuilder.getSectionInfo().getName(), sectionBuilder.getSectionInfo().getMode()));
    }

    private boolean allocatePageAlignedInSegment(Segment current, SectionBuilder sectionBuilder) {
        int pageStart;
        for (pageStart = (current.start | 0xFF) + 1; pageStart <= current.end; pageStart += 0x100) {
            if(pageStart + sectionBuilder.getCodeLength() <= current.end){
                current.allocate(pageStart - current.start, sectionBuilder);
                return true;
            }
        }
        return false;
    }

    private void allocatePageAlign(SectionBuilder sectionBuilder) {
        Segment current = head;
        while(current != null){
            if(!current.isAllocated() && current.getSize() >= sectionBuilder.getCodeLength()){
                if(allocatePageAlignedInSegment(current, sectionBuilder))
                    return;
            }
            current = current.next;
        }
        unableToPlace(sectionBuilder);
    }

    private void allocateFit(SectionBuilder sectionBuilder) {
        Segment current = head;
        while(current != null){
            if(!current.isAllocated() && current.getSize() >= sectionBuilder.getCodeLength()){
                current.allocate(0, sectionBuilder);
                return;
            }

            current = current.next;
        }

        unableToPlace(sectionBuilder);
    }

    private void allocateFixed(SectionBuilder sectionBuilder) {
        int sectionStart = sectionBuilder.getSectionInfo().getStart();

        Segment segment = getAt(sectionStart);

        if(segment == null){
            throw new IllegalStateException("no segment at address");
        }

        segment.allocate(sectionStart - segment.start, sectionBuilder);
    }

    private Segment getAt(int address){
        Segment current = head;
        while(current != null){
            if(current.start <= address && current.end >= address){
                return current;
            }

            current = current.next;
        }
        return null;
    }

    public void visit(){
        System.out.printf("Memory layout for %s (%04x - %04x)\n", device.name(), device.start, device.end);
        Segment current = head;
        while(current != null){
            System.out.printf("\t%04x-%04x:\t%s%n", current.start, current.end, current.isAllocated() ? current.sectionBuilder.getSectionInfo().getName() : "free");
            current = current.next;
        }
    }

    public void validate() {
        //validate memory continuity, no overlaps
        Segment current = head;
        while(current != null){
            if(current.isAllocated()){

                int length = current.sectionBuilder.getCodeLength();
                if (length == 0) {
                    current = current.next;
                    continue;
                }

                if(current.start != current.sectionBuilder.getStart() || current.end != current.sectionBuilder.getStart() + current.sectionBuilder.getCodeLength() - 1){
                    throw new IllegalStateException("section '%s': mismatch between allocated segment (%04x-%04x) and section boundaries (%04x-%04x)".formatted(current.sectionBuilder.getSectionInfo().getName(), current.start, current.end, current.sectionBuilder.getSectionInfo().getStart(), current.sectionBuilder.getSectionInfo().getStart() + current.sectionBuilder.getCodeLength() - 1));
                }

                switch (current.sectionBuilder.getSectionInfo().getMode()) {
                    case FIXED -> {
                        if (current.start != current.sectionBuilder.getSectionInfo().getStart()) {
                            throw new IllegalStateException("unable to place '%s' sec".formatted(current.sectionBuilder.getSectionInfo().getName()));
                        }
                    }
                    case PAGE_ALIGN -> {
                        if(current.start != (current.start & 0xFF00)) {
                            throw new IllegalStateException("unable to place section '%s' in page aligned mode".formatted(current.sectionBuilder.getSectionInfo().getName()));
                        }
                    }
                    case PAGE_FIT -> {
                        if((current.start & 0xFF00) != (current.end & 0xFF00)) {
                            throw new IllegalStateException("unable to place section '%s' in page fit mode".formatted(current.sectionBuilder.getSectionInfo().getName()));
                        }
                    }
                }
            }

            // memory continuity
            if(current.next != null){
                if(current.end + 1 > current.next.start) {
                    throw new IllegalStateException("memory layout not continuous (overlapping segments '%s' and '%s')".formatted(current.sectionBuilder.getSectionInfo().getName(), current.next.sectionBuilder.getSectionInfo().getName()));
                }else if (current.end + 1 < current.next.start) {
                    throw new IllegalStateException("memory layout not continuous (missing segment between '%s' and '%s')".formatted(current.sectionBuilder.getSectionInfo().getName(), current.next.sectionBuilder.getSectionInfo().getName()));
                }
            }else if(current.end != device.end){
                throw new IllegalStateException("memory layout not continuous (missing end segment)");
            }

            current = current.next;
        }
    }

    private static class Segment{
        //start address, inclusive
        private final int start;

        //end address, inclusive
        private int end;

        private Segment previous, next;

        private SectionBuilder sectionBuilder;

        private Segment(int start, int end){
            this.start = start;
            this.end = end;
        }

        private boolean isAllocated(){
            return sectionBuilder != null;
        }

        private void allocate(int offset, SectionBuilder sectionBuilder){
            if(isAllocated()){
                throw new IllegalStateException("segment already allocated (section '%s')".formatted(this.sectionBuilder.getSectionInfo().getName()));
            }

            if(offset < 0 || offset >= getSize()){
                throw new IllegalStateException("offset out of bounds");
            }

            if(offset + sectionBuilder.getCodeLength() > getSize()){
                throw new IllegalStateException("section too large");
            }

            if(offset == 0){
                split(sectionBuilder.getCodeLength());
                this.sectionBuilder = sectionBuilder;
                this.sectionBuilder.setSectionStart(start);
            }else{
                split(offset);
                next.allocate(0, sectionBuilder);
            }
        }



        private void split(int offset){
            if(isAllocated()){
                throw new IllegalStateException("segment already allocated (section '%s')".formatted(sectionBuilder.getSectionInfo().getName()));
            }

            if(offset < 0 || offset > getSize()){
                throw new IllegalArgumentException("offset out of bounds");
                }

            if(offset == getSize() - 1){
                return;
            }

            Segment newSegment = new Segment(this.start + offset, this.end);
            this.end = this.start + offset - 1;

            if(this.next != null){
                this.next.previous = newSegment;
            }

            newSegment.next = this.next;
            newSegment.previous = this;
            this.next = newSegment;

        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Segment segment = (Segment) o;

            if (start != segment.start) return false;
            if (end != segment.end) return false;
            return Objects.equals(sectionBuilder, segment.sectionBuilder);
        }

        @Override
        public int hashCode() {
            int result = start;
            result = 31 * result + end;
            result = 31 * result + (sectionBuilder != null ? sectionBuilder.hashCode() : 0);
            return result;
        }

        private int getSize(){
            return end - start + 1;
        }
    }
}
