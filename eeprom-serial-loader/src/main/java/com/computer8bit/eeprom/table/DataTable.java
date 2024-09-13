package com.computer8bit.eeprom.table;

import com.computer8bit.eeprom.data.EEPROMDataSet;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;

public class DataTable extends JTable implements MouseListener {

    private final List<IDataTableSelectionListener> listeners = new ArrayList<>();

    private final int rowWidth;

    public DataTable(EEPROMDataSet dataSet, int rowWidth) {
        this.rowWidth = rowWidth;
        this.setComponentPopupMenu(new TablePopupMenu(this, dataSet));
        this.setModel(new DataTableModel(dataSet, rowWidth));
        this.setShowGrid(false);
        this.setSelectionForeground(Color.WHITE);
        this.setSelectionBackground(Color.LIGHT_GRAY);
        this.setCellSelectionEnabled(true);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if(e.getKeyCode() >= VK_LEFT && e.getKeyCode() <= VK_DOWN)
                    EventQueue.invokeLater(() -> fireSelectionChanged());
            }
        });
        this.setDefaultRenderer(String.class, new DefaultTableCellRenderer(){
            Font font = new Font(Font.MONOSPACED, Font.BOLD, 18);
            Color unsetColor = new Color(220, 220, 220);
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(UIManager.getColor("Table.background"));
                if (column == 0)
                    setFont(font);
                else if(((DataTableModel) getModel()).isZero(value.toString()))
                    setBackground(unsetColor);
                if(isSelected)
                    setBackground(Color.LIGHT_GRAY);
                return this;
            }
        });
        for(int i = 0; i < this.getColumnCount(); ++i){
            this.getColumnModel().getColumn(i).setHeaderValue(" ");
        }
        this.addMouseListener(this);
        this.getSelectionModel().addListSelectionListener(e -> fireSelectionChanged());

        this.getTableHeader().setVisible(true);
        this.getTableHeader().setReorderingAllowed(false);
        this.setIntercellSpacing(new Dimension(0, 0));

    }

    private void fireSelectionChanged() {
        for (IDataTableSelectionListener listener : listeners) {
            listener.onSelectionChanged(getSelectedRow(), getSelectedColumn());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.isPopupTrigger() && columnAtPoint(e.getPoint()) > 0){
            getComponentPopupMenu().show(this, e.getX(), e.getY());
        }
        SwingUtilities.invokeLater(this::fireSelectionChanged);
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    public int toDatasetIndex(int r, int c) {
        return r * rowWidth + c - 1;
    }

    public void addSelectionListener(IDataTableSelectionListener listener) {
        listeners.add(listener);
    }

    public void removeSelectionListener(IDataTableSelectionListener listener) {
        listeners.remove(listener);
    }
}
