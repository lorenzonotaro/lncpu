package com.computer8bit.eeprom.util;

import com.computer8bit.eeprom.data.EEPROMDataByte;
import com.computer8bit.eeprom.data.EEPROMDataSet;

public interface IHistoryItem {

    void undo() throws HistoryException;
    void redo() throws HistoryException;
    default IHistoryItem merge(IHistoryItem other){
        return null;
    }

    class ByteValueEdit implements IHistoryItem{

        private final EEPROMDataByte dataByte;
        private final byte before;
        private final byte after;

        public ByteValueEdit(EEPROMDataByte dataByte, byte before, byte after){
            this.dataByte = dataByte;
            this.before = before;
            this.after = after;
        }

        @Override
        public void undo() {
            dataByte.setValue(before);
        }

        @Override
        public void redo() {
            dataByte.setValue(after);
        }

    }

    class ByteLabelEdit implements IHistoryItem{

        private final EEPROMDataByte dataByte;
        private final int index;
        private final String before;
        private final String after;
        private final long time;

        public ByteLabelEdit(EEPROMDataByte dataByte, int index, String before, String after, long time){
            this.dataByte = dataByte;
            this.index = index;
            this.before = before;
            this.after = after;
            this.time = time;
        }

        @Override
        public void undo() throws HistoryException {
            if(index == -1){
                dataByte.setByteLabel(before);
            }else if(index >= 0 && index <= 7){
                dataByte.setBitLabel(index, before);
            }else throw new HistoryException("invalid index");
        }

        @Override
        public void redo() throws HistoryException {
            if(index == -1){
                dataByte.setByteLabel(after);
            }else if(index >= 0 && index <= 7){
                dataByte.setBitLabel(index, after);
            }else throw new HistoryException("invalid index");
        }

        @Override
        public IHistoryItem merge(IHistoryItem other_){
            if (!(other_ instanceof ByteLabelEdit))
                return null;
            ByteLabelEdit other = (ByteLabelEdit) other_;
            if(other.dataByte.getAddress() != this.dataByte.getAddress() || other.index != this.index)
                return null;
            ByteLabelEdit last = other.time > this.time ? other : this;
            ByteLabelEdit first = this.equals(last) ? other : this;
            return new ByteLabelEdit(dataByte, index, first.before, last.after, last.time);
        }
    }

    class DatasetResize implements IHistoryItem{

        private final EEPROMDataSet dataSet;
        private final EEPROMDataByte[] before;
        private final EEPROMDataByte[] after;

        public DatasetResize(EEPROMDataSet dataSet, EEPROMDataByte[] before, EEPROMDataByte[] after){
            this.dataSet = dataSet;
            this.before = before;
            this.after = after;
        }

        @Override
        public void undo() throws HistoryException {
            dataSet.setData(before);
        }

        @Override
        public void redo() throws HistoryException {
            dataSet.setData(after);
        }
    }
}
