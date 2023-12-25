package com.computer8bit.eeprom.table;

import com.computer8bit.eeprom.data.EEPROMDataByte;
import com.computer8bit.eeprom.data.EEPROMDataSet;
import com.computer8bit.eeprom.util.Util;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;

import static java.awt.event.KeyEvent.*;

public class TablePopupMenu extends JPopupMenu {
    private final DataTable table;
    private final EEPROMDataSet dataSet;
    private final JMenuItem setToZero;
    private final JMenuItem setTo;
    private final JMenuItem copy;
    private final JMenuItem paste;
    private EEPROMDataByte[][] clipboard;

    public TablePopupMenu(DataTable table, EEPROMDataSet dataSet){
        this.table = table;
        this.dataSet = dataSet;
        setToZero = new JMenuItem("Set to zero");
        setToZero.setAccelerator(KeyStroke.getKeyStroke(VK_0, InputEvent.CTRL_MASK));
        setToZero.addActionListener(e -> this.setSelectionTo((byte) 0x00));
        add(setToZero);
        setTo = new JMenuItem("Set to...");
        setTo.addActionListener(this::setToPressed);
        setTo.setAccelerator(KeyStroke.getKeyStroke(VK_T, InputEvent.CTRL_MASK));
        add(setTo);
        addSeparator();
        copy = new JMenuItem("Copy");
        copy.setAccelerator(KeyStroke.getKeyStroke(VK_C, InputEvent.CTRL_MASK));
        copy.addActionListener(this::copyPressed);
        add(copy);
        paste = new JMenuItem("Paste");
        paste.setAccelerator(KeyStroke.getKeyStroke(VK_V, InputEvent.CTRL_MASK));
        paste.addActionListener(this::pastePressed);
        add(paste);
    }

    private void pastePressed(ActionEvent e) {
        if(clipboard == null) return;
        int startY = table.getSelectedRow();
        int startX = table.getSelectedColumn();

        if(startY < 0 || startX < 1) return;
        if(table.getRowCount() - startY <= clipboard.length){
            startY = table.getRowCount() - clipboard.length;
        }
        if(table.getColumnCount() - startX <= clipboard[0].length){
            startX = table.getColumnCount() - clipboard[0].length;
        }
        for (int y = 0; y < clipboard.length; y++) {
            for (int x = 0; x < clipboard[y].length; x++) {
                dataSet.setByteAt(table.toDatasetIndex(startY + y, startX + x), clipboard[y][x]);
            }
        }
    }

    private void copyPressed(ActionEvent e) {
        int[] rows = table.getSelectedRows();
        int[] cols = table.getSelectedColumns();
        int n = rows.length * cols.length;
        if(n == 0) return;

        clipboard = new EEPROMDataByte[rows.length][cols.length];

        for (int j = 0; j < rows.length; j++) {
            for (int k = 0; k < cols.length; k++) {
                int r = rows[j], c = cols[k];
                clipboard[j][k] = new EEPROMDataByte(dataSet.getByteAt(table.toDatasetIndex(r,c)));
            }
        }
    }

    private void setToPressed(ActionEvent e) {
        String strVal = JOptionPane.showInputDialog(null, "Set bytes to: ", 0);
        if(strVal == null) return;
        byte value;
        try{
            value = Util.parseByte(strVal, ((DataTableModel)table.getModel()).getViewMode());
            setSelectionTo(value);
        }catch (NumberFormatException exception){
            JOptionPane.showMessageDialog(null,  "Invalid input value '" + strVal + "'", "Error.", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setSelectionTo(byte val) {
        int[] rows = table.getSelectedRows();
        int[] columns = table.getSelectedColumns();
        for (int j = 0; j < rows.length; j++) {
            for (int k = 0; k < columns.length; k++) {
                int r = rows[j];
                int c = columns[k];
                if(c == 0)continue;
                dataSet.getByteAt(table.toDatasetIndex(r,c)).setValue(val);
            }
        }
    }

    @Override
    public void show(Component invoker, int x, int y) {
        int[] rows = table.getSelectedRows();
        int[] columns = table.getSelectedColumns();
        boolean selectionDependentActive = rows.length * columns.length != 0;
        setTo.setEnabled(selectionDependentActive);
        setToZero.setEnabled(selectionDependentActive);
        super.show(invoker, x, y);
    }

    private class DataClipboard{
        private final int[][] indices;
        private final int[] data;

        private DataClipboard(int[][] indices, int[] data){
            this.indices = indices;
            this.data = data;
        }
    }
}
