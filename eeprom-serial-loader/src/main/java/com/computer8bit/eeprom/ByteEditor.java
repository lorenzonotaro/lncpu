package com.computer8bit.eeprom;

import com.computer8bit.eeprom.data.EEPROMDataByte;
import com.computer8bit.eeprom.data.EEPROMDataSet;
import com.computer8bit.eeprom.event.EEPROMDataChangeEvent;
import com.computer8bit.eeprom.event.EEPROMDataChangeListener;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Locale;

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

    private ByteEditor() {
        $$$setupUI$$$();
    }

    public ByteEditor(EEPROMDataSet dataSet) {
        $$$setupUI$$$();
        dataSet.addChangeListener(this);
    }

    private void createUIComponents() {
        contentPane = new JPanel() {
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

    private void dataUpdated() {
        if (shouldRunListeners && dataByte != null) {
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

    public void setDataByte(EEPROMDataByte dataByte) {
        contentPane.setEnabled((this.dataByte = dataByte) != null);
        this.dataByte = dataByte;

        if (dataByte == null)
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

    private static void setEnabled(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (!(component instanceof JComponent)) return;
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

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        Font contentPaneFont = this.$$$getFont$$$("Monospaced", -1, 16, contentPane.getFont());
        if (contentPaneFont != null) contentPane.setFont(contentPaneFont);
        contentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(9, 1, new Insets(5, 5, 5, 5), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Bit editor", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Font bitEditor0Font = this.$$$getFont$$$("Monospaced", -1, 18, bitEditor0.getFont());
        if (bitEditor0Font != null) bitEditor0.setFont(bitEditor0Font);
        panel2.add(bitEditor0, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bitNLabel0 = new JLabel();
        bitNLabel0.setText("#0");
        panel2.add(bitNLabel0, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Font bitLabelEditor0Font = this.$$$getFont$$$("Monospaced", -1, 16, bitLabelEditor0.getFont());
        if (bitLabelEditor0Font != null) bitLabelEditor0.setFont(bitLabelEditor0Font);
        panel2.add(bitLabelEditor0, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Font bitEditor1Font = this.$$$getFont$$$("Monospaced", -1, 18, bitEditor1.getFont());
        if (bitEditor1Font != null) bitEditor1.setFont(bitEditor1Font);
        panel3.add(bitEditor1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bitNLabel1 = new JLabel();
        bitNLabel1.setText("#1");
        panel3.add(bitNLabel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Font bitLabelEditor1Font = this.$$$getFont$$$("Monospaced", -1, 16, bitLabelEditor1.getFont());
        if (bitLabelEditor1Font != null) bitLabelEditor1.setFont(bitLabelEditor1Font);
        panel3.add(bitLabelEditor1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Font bitEditor2Font = this.$$$getFont$$$("Monospaced", -1, 18, bitEditor2.getFont());
        if (bitEditor2Font != null) bitEditor2.setFont(bitEditor2Font);
        panel4.add(bitEditor2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bitNLabel2 = new JLabel();
        bitNLabel2.setText("#2");
        panel4.add(bitNLabel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Font bitLabelEditor2Font = this.$$$getFont$$$("Monospaced", -1, 16, bitLabelEditor2.getFont());
        if (bitLabelEditor2Font != null) bitLabelEditor2.setFont(bitLabelEditor2Font);
        panel4.add(bitLabelEditor2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Font bitEditor3Font = this.$$$getFont$$$("Monospaced", -1, 18, bitEditor3.getFont());
        if (bitEditor3Font != null) bitEditor3.setFont(bitEditor3Font);
        panel5.add(bitEditor3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bitNLabel3 = new JLabel();
        bitNLabel3.setText("#3");
        panel5.add(bitNLabel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Font bitLabelEditor3Font = this.$$$getFont$$$("Monospaced", -1, 16, bitLabelEditor3.getFont());
        if (bitLabelEditor3Font != null) bitLabelEditor3.setFont(bitLabelEditor3Font);
        panel5.add(bitLabelEditor3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Font bitEditor4Font = this.$$$getFont$$$("Monospaced", -1, 18, bitEditor4.getFont());
        if (bitEditor4Font != null) bitEditor4.setFont(bitEditor4Font);
        panel6.add(bitEditor4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bitNLabel4 = new JLabel();
        bitNLabel4.setText("#4");
        panel6.add(bitNLabel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Font bitLabelEditor4Font = this.$$$getFont$$$("Monospaced", -1, 16, bitLabelEditor4.getFont());
        if (bitLabelEditor4Font != null) bitLabelEditor4.setFont(bitLabelEditor4Font);
        panel6.add(bitLabelEditor4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel7, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Font bitEditor5Font = this.$$$getFont$$$("Monospaced", -1, 18, bitEditor5.getFont());
        if (bitEditor5Font != null) bitEditor5.setFont(bitEditor5Font);
        panel7.add(bitEditor5, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bitNLabel5 = new JLabel();
        bitNLabel5.setText("#5");
        panel7.add(bitNLabel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Font bitLabelEditor5Font = this.$$$getFont$$$("Monospaced", -1, 16, bitLabelEditor5.getFont());
        if (bitLabelEditor5Font != null) bitLabelEditor5.setFont(bitLabelEditor5Font);
        panel7.add(bitLabelEditor5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel8, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Font bitEditor6Font = this.$$$getFont$$$("Monospaced", -1, 18, bitEditor6.getFont());
        if (bitEditor6Font != null) bitEditor6.setFont(bitEditor6Font);
        panel8.add(bitEditor6, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bitNLabel6 = new JLabel();
        bitNLabel6.setText("#6");
        panel8.add(bitNLabel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Font bitLabelEditor6Font = this.$$$getFont$$$("Monospaced", -1, 16, bitLabelEditor6.getFont());
        if (bitLabelEditor6Font != null) bitLabelEditor6.setFont(bitLabelEditor6Font);
        panel8.add(bitLabelEditor6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel9, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Font bitEditor7Font = this.$$$getFont$$$("Monospaced", -1, 18, bitEditor7.getFont());
        if (bitEditor7Font != null) bitEditor7.setFont(bitEditor7Font);
        panel9.add(bitEditor7, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bitNLabel7 = new JLabel();
        bitNLabel7.setText("#7");
        panel9.add(bitNLabel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Font bitLabelEditor7Font = this.$$$getFont$$$("Monospaced", -1, 16, bitLabelEditor7.getFont());
        if (bitLabelEditor7Font != null) bitLabelEditor7.setFont(bitLabelEditor7Font);
        panel9.add(bitLabelEditor7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("(Most to least significant)");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        contentPane.add(panel10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        panel10.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 2, new Insets(5, 5, 5, 5), -1, -1));
        panel10.add(panel11, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        valueLabel = new JLabel();
        Font valueLabelFont = this.$$$getFont$$$("Monospaced", -1, 24, valueLabel.getFont());
        if (valueLabelFont != null) valueLabel.setFont(valueLabelFont);
        valueLabel.setText("");
        panel11.add(valueLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Byte value:");
        panel11.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel10.add(panel12, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        Font byteLabelEditorFont = this.$$$getFont$$$("Monospaced", -1, 16, byteLabelEditor.getFont());
        if (byteLabelEditorFont != null) byteLabelEditor.setFont(byteLabelEditorFont);
        panel12.add(byteLabelEditor, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Byte label:");
        panel12.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel10.add(panel13, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addressLabel = new JLabel();
        Font addressLabelFont = this.$$$getFont$$$("Monospaced", -1, 24, addressLabel.getFont());
        if (addressLabelFont != null) addressLabel.setFont(addressLabelFont);
        addressLabel.setText("");
        panel13.add(addressLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Byte address:");
        panel13.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label2.setLabelFor(byteLabelEditor);
        label3.setLabelFor(byteLabelEditor);
        label4.setLabelFor(byteLabelEditor);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
