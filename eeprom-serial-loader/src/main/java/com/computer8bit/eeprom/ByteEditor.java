package com.computer8bit.eeprom;

import com.computer8bit.eeprom.data.EEPROMDataByte;
import com.computer8bit.eeprom.data.EEPROMDataSet;
import com.computer8bit.eeprom.event.EEPROMDataChangeEvent;
import com.computer8bit.eeprom.event.EEPROMDataChangeListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ByteEditor extends KeyAdapter implements EEPROMDataChangeListener {
    private static final Integer[] BIT_VALUES = {0, 1};
    private JPanel contentPane;
    private JComboBox bitEditor0;
    private JComboBox bitEditor1;
    private JComboBox bitEditor2;
    private JComboBox bitEditor3;
    private JComboBox bitEditor4;
    private JComboBox bitEditor5;
    private JComboBox bitEditor6;
    private JComboBox bitEditor7;
    private JComboBox[] bitEditors = new JComboBox[8];
    private JLabel bitNLabel0;
    private JLabel bitNLabel1;
    private JLabel bitNLabel2;
    private JLabel bitNLabel3;
    private JLabel bitNLabel4;
    private JLabel bitNLabel5;
    private JLabel bitNLabel6;
    private JLabel bitNLabel7;
    private JTextField bitLabelEditor0;
    private JTextField bitLabelEditor1;
    private JTextField bitLabelEditor2;
    private JTextField bitLabelEditor3;
    private JTextField bitLabelEditor4;
    private JTextField bitLabelEditor5;
    private JTextField bitLabelEditor6;
    private JTextField bitLabelEditor7;
    private JTextField[] bitLabelEditors = new JTextField[8];
    private JTextField byteLabelEditor;
    private JLabel valueLabel;
    private JLabel addressLabel;

    private boolean shouldRunListeners = true;
    private EEPROMDataByte dataByte = null;

    private ByteEditor(){ }

    public ByteEditor(EEPROMDataSet dataSet){
        dataSet.addChangeListener(this);
    }

    private void createUIComponents() {
        contentPane = new JPanel(){
            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : this.getComponents()) {
                    ByteEditor.setEnabled(c, enabled);
                }
            }
        };

        bitEditors[0] = bitEditor0 = new JComboBox<>(BIT_VALUES);
        bitEditors[1] = bitEditor1 = new JComboBox<>(BIT_VALUES);
        bitEditors[2] = bitEditor2 = new JComboBox<>(BIT_VALUES);
        bitEditors[3] = bitEditor3 = new JComboBox<>(BIT_VALUES);
        bitEditors[4] = bitEditor4 = new JComboBox<>(BIT_VALUES);
        bitEditors[5] = bitEditor5 = new JComboBox<>(BIT_VALUES);
        bitEditors[6] = bitEditor6 = new JComboBox<>(BIT_VALUES);
        bitEditors[7] = bitEditor7 = new JComboBox<>(BIT_VALUES);

        for (JComboBox item : bitEditors) {
            item.addActionListener(e -> EventQueue.invokeLater(this::dataUpdated));
        }


        bitLabelEditors[0] = bitLabelEditor0 = new JTextField();
        bitLabelEditors[1] = bitLabelEditor1 = new JTextField();
        bitLabelEditors[2] = bitLabelEditor2 = new JTextField();
        bitLabelEditors[3] = bitLabelEditor3 = new JTextField();
        bitLabelEditors[4] = bitLabelEditor4 = new JTextField();
        bitLabelEditors[5] = bitLabelEditor5 = new JTextField();
        bitLabelEditors[6] = bitLabelEditor6 = new JTextField();
        bitLabelEditors[7] = bitLabelEditor7 = new JTextField();

        for (JTextField item : bitLabelEditors) {
            item.addKeyListener(this);
        }

        byteLabelEditor = new JTextField();
        byteLabelEditor.addKeyListener(this);
        contentPane.setEnabled(false);
    }

    private void dataUpdated(){
        if(shouldRunListeners && dataByte != null){
            int value = 0;
            for (int i = 0; i < bitEditors.length; i++) {
                value += ((Integer) bitEditors[i].getSelectedItem() << (7 - i));
            }
            this.dataByte.setValue((byte) (value & 0xFF));

            for (int i = 0; i < bitLabelEditors.length; i++) {
                JTextField t = bitLabelEditors[i];
                dataByte.setBitLabel(i, t.getText());
            }

            dataByte.setByteLabel(byteLabelEditor.getText());
        }

        if (dataByte != null) {
            valueLabel.setText(String.format("0x%02x", dataByte.getValue()));
        }
    }

    public void setDataByte(EEPROMDataByte dataByte){
        contentPane.setEnabled((this.dataByte = dataByte) != null);
        this.dataByte = dataByte;

        if(dataByte == null)
            return;

        addressLabel.setText(String.format("0x%04x", dataByte.getAddress()));
        valueLabel.setText(String.format("0x%02x", dataByte.getValue()));
        this.shouldRunListeners = false;

        for (int i = 0; i < bitEditors.length; i++) {
            JComboBox item = bitEditors[i];
            item.setSelectedItem((dataByte.getValue() >> (7 - i)) & 0x1);
        }

        for (int i = 0; i < bitLabelEditors.length; i++) {
            JTextField t = bitLabelEditors[i];
            t.setText(dataByte.getBitLabel(i));
        }

        byteLabelEditor.setText(dataByte.getByteLabel());

        EventQueue.invokeLater(() -> shouldRunListeners = true);
    }

    private static void setEnabled(Component component, boolean enabled){
        component.setEnabled(enabled);
        if(!(component instanceof JComponent)) return;
        for (Component c : ((JComponent) component).getComponents()) {
            ByteEditor.setEnabled(c, enabled);
        }
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        dataUpdated();
    }

    @Override
    public void dataChanged(EEPROMDataChangeEvent event) {
        this.setDataByte(this.dataByte);
    }
}
