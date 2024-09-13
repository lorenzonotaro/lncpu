package com.computer8bit.eeprom.table;

import com.computer8bit.eeprom.data.EEPROMDataSet;
import com.computer8bit.eeprom.table.DataTable;
import com.computer8bit.eeprom.table.DataTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class DiffDataTable extends DataTable {
    private final DiffDataTableModel tableModel;

    public DiffDataTable(EEPROMDataSet dataSet, EEPROMDataSet readData, int rowWidth) {
        super(dataSet, rowWidth);
        this.setModel(tableModel = new DiffDataTableModel(dataSet, readData, rowWidth));
        this.setDefaultRenderer(DiffElement.class, new DefaultTableCellRenderer(){
            final Font font = new Font(Font.MONOSPACED, Font.BOLD, 18);
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                DiffElement diffElement = (DiffElement) value;
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(UIManager.getColor("Table.background"));
                if (column == 0)
                    setFont(font);

                if(diffElement != null){
                    setBackground(diffElement.type().colorGetter.get());
                    setText(diffElement.displayString());
                }

                if(isSelected)
                    setBackground(Color.LIGHT_GRAY);
                return this;
            }
        });
    }
}
