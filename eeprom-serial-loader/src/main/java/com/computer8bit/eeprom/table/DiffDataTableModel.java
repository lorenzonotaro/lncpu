package com.computer8bit.eeprom.table;

import com.computer8bit.eeprom.data.EEPROMDataSet;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

public class DiffDataTableModel extends DataTableModel {
    private final EEPROMDataSet dataSet;

    private final EEPROMDataSet readData;

    public DiffDataTableModel(EEPROMDataSet dataSet, EEPROMDataSet readData, int rowWidth) {
        super(dataSet, rowWidth);
        this.dataSet = dataSet;
        this.readData = readData;
    }

    @Override
    public int getRowCount() {
        if(dataSet == null || readData == null) return 0;
        return Math.max(1, Math.max(dataSet.getLength(), readData.getLength()) / this.rowWidth);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if(columnIndex == 0){
            return String.format("%04X", rowIndex * rowWidth);
        }else{
            int address = rowIndex * rowWidth + columnIndex - 1;
            if(address >= dataSet.getLength() && address >= readData.getLength()){
                return null;
            }else if(address >= dataSet.getLength()){
                return new DiffElement(DiffElement.Type.ADDED, String.format("%02X", readData.getByteAt(address).getValue()));
            }else if(address >= readData.getLength()) {
                return new DiffElement(DiffElement.Type.MISSING, String.format("%02X", dataSet.getByteAt(address).getValue()));
            }else if(dataSet.getByteAt(address).getValue() == readData.getByteAt(address).getValue()){
                return new DiffElement(DiffElement.Type.SAME, String.format("%02X", dataSet.getByteAt(address).getValue()));
            }else{
                return new DiffElement(DiffElement.Type.DIFFERENT, String.format("%02X (r: %02X)", dataSet.getByteAt(address).getValue(), readData.getByteAt(address).getValue()));
            }
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? String.class : DiffElement.class;
    }
}
