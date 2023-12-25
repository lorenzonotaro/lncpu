package com.computer8bit.eeprom.table;

import com.computer8bit.eeprom.data.EEPROMDataSet;
import com.computer8bit.eeprom.Window;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;

import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;

public class DataTable extends JTable implements MouseListener {
    private final DataTableModel tableModel;
    private final Window window;
    private final int rowWidth;

    public DataTable(Window window, EEPROMDataSet dataSet, int rowWidth) {
        this.window = window;
        this.rowWidth = rowWidth;
        tableModel = new DataTableModel(dataSet, rowWidth);
        this.setComponentPopupMenu(new TablePopupMenu(this, dataSet));
        this.setModel(tableModel);
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
                    EventQueue.invokeLater(() -> window.updateByteEditor(e));
            }
        });
        this.setDefaultRenderer(String.class, new DefaultTableCellRenderer(){
            Font font = new Font(Font.MONOSPACED, Font.BOLD, 18);
            Color unsetColor = new Color(220, 220, 220);
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(window.getContentPane().getBackground());
                if (column == 0)
                    setFont(font);
                else if(tableModel.isZero(value.toString()))
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
        this.getSelectionModel().addListSelectionListener(window::updateByteEditor);

        this.getTableHeader().setVisible(true);
        this.getTableHeader().setReorderingAllowed(false);
        this.setIntercellSpacing(new Dimension(0, 0));

    }

    @Override
    public void mouseClicked(MouseEvent e) {
        window.updateByteEditor(e);
        if(e.isPopupTrigger() && columnAtPoint(e.getPoint()) > 0){
            getComponentPopupMenu().show(this, e.getX(), e.getY());
        }
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
}
