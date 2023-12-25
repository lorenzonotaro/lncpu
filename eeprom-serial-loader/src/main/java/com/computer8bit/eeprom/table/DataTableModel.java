package com.computer8bit.eeprom.table;

import com.computer8bit.eeprom.event.EEPROMDataChangeEvent;
import com.computer8bit.eeprom.data.EEPROMDataSet;
import com.computer8bit.eeprom.util.Util;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;

public class DataTableModel extends AbstractTableModel {

    private ViewMode viewMode;
    private EEPROMDataSet data;
    private int rowWidth;

    DataTableModel(EEPROMDataSet data, int rowWidth){
        super();
        this.data = data;
        this.viewMode = ViewMode.HEXADECIMAL;
        data.addChangeListener(this::dataChanged);
        this.rowWidth = rowWidth;
    }

    private void dataChanged(EEPROMDataChangeEvent changeEvent) {
        if(changeEvent.getType().equals(EEPROMDataChangeEvent.Type.RESIZE)){
            super.fireTableDataChanged();
        }else fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        if(data == null) return 0;
        int count =  data.getLength() / rowWidth;
        if ((count == 0 && data.getLength() != 0) || data.getLength() % count != 0) ++count;
        return count;
    }

    @Override
    public int getColumnCount() {
        return 1 + rowWidth;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0 && rowIndex * rowWidth + columnIndex - 1 < data.getLength();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        boolean isFirstCol = columnIndex == 0;
        int val;
        if(isFirstCol){
            val = rowIndex * rowWidth;
        }else if(rowIndex * rowWidth + columnIndex - 1 < data.getLength()){
            val = data.getByteValueAt(rowIndex * rowWidth + columnIndex - 1) & 0xFF;
        }else{
            return "";
        }
        switch(viewMode){

            case DECIMAL:
                return String.format("%d", val);
            case HEXADECIMAL:
                return String.format(isFirstCol ? "%04x" : "%02x", val);
            case ASCII:
                if(isFirstCol){
                    return String.format("%04x", val);
                }else{
                    return String.format("%c", (char) val);
                }
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if(rowIndex * rowWidth + columnIndex - 1 >= data.getLength()){
            JOptionPane.showMessageDialog(null,  "Unable to edit this value. Try increasing the data size.", "Error.", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String strVal = aValue.toString();
        byte value;
        try{
            value = Util.parseByte(strVal, this.viewMode);
            data.getByteAt(rowIndex * rowWidth + columnIndex - 1).setValue(value);
        }catch(NumberFormatException e){
            JOptionPane.showMessageDialog(null,  "Invalid input value '" + strVal + "'", "Error.", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void fireTableDataChanged() {
        fireTableChanged(new TableModelEvent(this, //tableModel
                0, //firstRow
                getRowCount() - 1, //lastRow
                TableModelEvent.ALL_COLUMNS, //column
                TableModelEvent.UPDATE)); //changeType
    }

    public void setViewMode(ViewMode viewMode) {
        this.viewMode = viewMode;
        fireTableDataChanged();
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public boolean isZero(String value) {
        switch(viewMode){
            case DECIMAL:
                return Integer.parseInt(value, 10) == 0;
            case HEXADECIMAL:
                return Integer.parseInt(value, 16) == 0;
            case ASCII:
                return value.charAt(0) == '\0';
            default:
                return false;
        }
    }
}


