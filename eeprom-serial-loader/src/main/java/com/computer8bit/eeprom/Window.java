package com.computer8bit.eeprom;

import com.computer8bit.eeprom.data.EEPROMDataByte;
import com.computer8bit.eeprom.data.EEPROMDataSet;
import com.computer8bit.eeprom.data.FileIO;
import com.computer8bit.eeprom.serial.PortDescriptor;
import com.computer8bit.eeprom.serial.SerialException;
import com.computer8bit.eeprom.serial.SerialInterface;
import com.computer8bit.eeprom.table.DataTable;
import com.computer8bit.eeprom.table.DataTableModel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static com.computer8bit.eeprom.table.ViewMode.*;

public class Window {

    private static final int ROW_WIDTH = 16;
    private final JFrame frame;
    JPanel contentPane;
    public static final int INITAL_DATA_LENGTH = 4096, MAX_DATA_LENGTH = 65536;
    private EEPROMDataSet dataSet;

    private JComboBox<PortDescriptor> serialPortSelection;
    private JButton refreshSerialPorts;
    private JTable dataTable;
    private JSpinner dataSizeSpinner;
    private JLabel serialPortStatus;
    private JButton writeEEPROMButton;
    private ByteEditor byteEditor;
    private JProgressBar progressBar;
    private JLabel operationStatusLabel;
    private JButton readEEPROMButton;
    private JPanel sidePanel;
    private JLabel chipSizeLabel;
    private JLabel payloadSizeLabel;
    private JRadioButton hexViewMode;
    private JRadioButton decimalViewMode;
    private JCheckBox checkContentsAfterWriting;
    private JRadioButton asciiViewMode;
    private final JMenuBar menuBar;
    private final SerialInterface serialInterface;
    private boolean serialPortValid = false;

    Window(JFrame frame) {
        this.frame = frame;
        $$$setupUI$$$();
        refreshSerialPorts.addActionListener(this::refreshSerialPorts);
        dataSizeSpinner.addChangeListener(this::dataSizeChanged);
        serialPortSelection.addItemListener(this::serialPortChanged);
        serialInterface = new SerialInterface();
        menuBar = new JMenuBar();
        JMenuItem helpMenu = new JMenuItem("Help");
        menuBar.add(makeFileMenu());
        menuBar.add(helpMenu);
        writeEEPROMButton.addActionListener(this::writeData);
        byteEditor.getContentPane().setEnabled(false);
        readEEPROMButton.addActionListener(this::readDataFromEEPROM);
        sidePanel.setMaximumSize(new Dimension(200, sidePanel.getMaximumSize().height));
        ButtonGroup viewModeBG = new ButtonGroup();
        viewModeBG.add(decimalViewMode);
        viewModeBG.add(hexViewMode);
        viewModeBG.add(asciiViewMode);
        hexViewMode.addActionListener(e -> {
            if (hexViewMode.isSelected()) ((DataTableModel) dataTable.getModel()).setViewMode(HEXADECIMAL);
        });
        decimalViewMode.addActionListener(e -> {
            if (decimalViewMode.isSelected()) ((DataTableModel) dataTable.getModel()).setViewMode(DECIMAL);
        });
        asciiViewMode.addActionListener(e -> {
            if (asciiViewMode.isSelected()) ((DataTableModel) dataTable.getModel()).setViewMode(ASCII);
        });
    }

    private void readDataFromEEPROM(ActionEvent actionEvent) {
        if (!serialPortValid) {
            JOptionPane.showMessageDialog(null, "Select a valid device first.", "Invalid device", JOptionPane.ERROR_MESSAGE);
            return;
        }
        doAsThread(() -> {
            try {
                byte[] data = serialInterface.readData(progressBar::setValue, operationStatusLabel::setText);
                setDataSet(data);
                operationStatusLabel.setText("Done.");
            } catch (SerialException ex) {
                JOptionPane.showMessageDialog(null, "Unable to write to EEPROM: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private JMenu makeFileMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem open = new JMenuItem("Open...");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        open.addActionListener(this::openFilePressed);
        JMenuItem save = new JMenuItem("Save...");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        save.addActionListener(this::saveFilePressed);
        fileMenu.add(open);
        fileMenu.add(save);

        fileMenu.addSeparator();

        JMenu exportSubmenu = new JMenu("Export");
        exportSubmenu.setMnemonic(KeyEvent.VK_E);
        JMenuItem exportAsHexDump = new JMenuItem("As hex dump...");
        exportAsHexDump.addActionListener(this::exportHexDump);
        exportSubmenu.add(exportAsHexDump);
        JMenuItem exportAsRawData = new JMenuItem("As raw data...");
        exportAsRawData.addActionListener(this::exportRawData);
        exportSubmenu.add(exportAsRawData);
        fileMenu.add(exportSubmenu);

        fileMenu.addSeparator();

        JMenu importSubmenu = new JMenu("Import");
        importSubmenu.setMnemonic(KeyEvent.VK_I);
        JMenuItem importAsHexDump = new JMenuItem("From hex dump...");
        importAsHexDump.addActionListener(this::importHexDump);
        importSubmenu.add(importAsHexDump);
        JMenuItem importAsRawData = new JMenuItem("From raw data...");
        importAsRawData.addActionListener(this::importRawData);
        importSubmenu.add(importAsRawData);
        fileMenu.add(importSubmenu);

        return fileMenu;
    }

    private void importRawData(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int response = fc.showDialog(null, "Import");
        if (response == JFileChooser.APPROVE_OPTION) {
            try {
                setDataSet(FileIO.importRawData(fc.getSelectedFile().toPath()));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Unable to export the file. (" + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importHexDump(ActionEvent actionEvent) {
        throw new Error("not implemented");
    }

    private void exportRawData(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int response = fc.showDialog(null, "Export");
        if (response == JFileChooser.APPROVE_OPTION) {
            String filename = fc.getSelectedFile().getAbsolutePath();
            try {
                FileIO.exportRawData(filename, dataSet.getData());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Unable to export the file. (" + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportHexDump(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int response = fc.showDialog(null, "Export");
        if (response == JFileChooser.APPROVE_OPTION) {
            String filename = fc.getSelectedFile().getAbsolutePath();
            try (FileWriter fw = new FileWriter(filename)) {
                FileIO.exportHexDump(filename, dataSet.getData());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Unable to export the file. (" + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void writeData(ActionEvent e) {
        if (!serialPortValid) {
            JOptionPane.showMessageDialog(null, "Select a valid device first.", "Invalid device", JOptionPane.ERROR_MESSAGE);
            return;
        }
        doAsThread(() -> {
            try {
                if (dataSet.getLength() > serialInterface.getMaxReadWriteLength()) {
                    int response = JOptionPane.showConfirmDialog(null, "You are trying to upload more data than the loader allows. Do you wish to continue (the additional data will be ignored)?", "Data length mismatch", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (response != JOptionPane.YES_OPTION)
                        return;
                }
                serialInterface.writeData(dataSet.getDataAsBytes(), progressBar::setValue, operationStatusLabel::setText, checkContentsAfterWriting.isSelected());
                operationStatusLabel.setText("Done.");
            } catch (SerialException ex) {
                JOptionPane.showMessageDialog(null, "Unable to write to EEPROM: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

    }

    private void saveFilePressed(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter("EEPROM data files", "eeprom"));
        fc.setAcceptAllFileFilterUsed(true);
        int response = fc.showDialog(null, "Save");
        if (response == JFileChooser.APPROVE_OPTION) {
            String filename = fc.getSelectedFile().getAbsolutePath();
            if (!filename.endsWith(".eeprom")) filename += ".eeprom";
            try {
                FileIO.saveFile(filename, dataSet.getData());
                frame.setTitle(Main.WINDOW_TITLE + " - " + filename);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Unable to save the file. (" + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openFilePressed(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter("EEPROM data files", "eeprom"));
        fc.setAcceptAllFileFilterUsed(true);
        int response = fc.showDialog(contentPane, "Open");
        if (response == JFileChooser.APPROVE_OPTION) {
            openFile(fc.getSelectedFile().toPath());
        }
    }

    void openFile(Path path) {
        try {
            EEPROMDataByte[] newData = FileIO.loadFile(path);
            if (newData.length > MAX_DATA_LENGTH) {
                JOptionPane.showMessageDialog(contentPane, "The input file (" + newData.length + " bytes) exceeds the maximum length: only the first " + MAX_DATA_LENGTH + " bytes will be imported.", "Input cropped", JOptionPane.INFORMATION_MESSAGE);
                newData = Arrays.copyOf(newData, MAX_DATA_LENGTH);
            }
            setDataSet(newData);
            frame.setTitle(Main.WINDOW_TITLE + " - " + path.toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(contentPane, "Unable to save the file. (" + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setDataSet(EEPROMDataByte[] newData) {
        dataSet.setData(newData);
        dataSizeSpinner.setValue(newData.length);
    }


    private void setDataSet(byte[] data) {
        dataSet.setData(data);
        dataSizeSpinner.setValue(data.length);
    }


    private void serialPortChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.DESELECTED)
            return;
        PortDescriptor item = (PortDescriptor) serialPortSelection.getSelectedItem();
        if (item == null || item.getPort() == null)
            return;
        doAsThread(() -> {
            try {
                serialPortSelection.setEnabled(false);
                serialPortStatus.setForeground(Color.BLACK);
                serialPortStatus.setText("Retrieving info...");
                serialInterface.setActivePort(item.getPort());
                String version = serialInterface.getLoaderVersion();
                serialInterface.setParams();
                serialPortStatus.setForeground(Color.GREEN.darker());
                serialPortStatus.setText("OK! Chip " + version);
                payloadSizeLabel.setText(String.valueOf(serialInterface.getPayloadSize()));
                chipSizeLabel.setText(String.valueOf(serialInterface.getMaxReadWriteLength()));
                this.serialPortValid = true;
            } catch (SerialException exception) {
                serialPortStatus.setForeground(Color.RED);
                serialPortStatus.setText("Invalid device (" + exception.getMessage() + ")!");
                try {
                    serialInterface.setActivePort(null);
                } catch (SerialException ex) {
                    ex.printStackTrace();
                }
                this.serialPortValid = false;
            } finally {
                serialPortSelection.setEnabled(true);
            }
        });

    }

    private void doAsThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    private void dataSizeChanged(ChangeEvent e) {
        dataSet.resize(((Integer) dataSizeSpinner.getModel().getValue()));
    }

    private void refreshSerialPorts(ActionEvent e) {
        serialPortStatus.setText("Select a serial port");
        serialPortStatus.setForeground(Color.BLACK);
        serialPortValid = false;
        serialPortSelection.removeAllItems();
        Arrays.asList(PortDescriptor.getDescriptors()).forEach(serialPortSelection::addItem);
        serialPortSelection.setSelectedIndex(0);
    }

    private void createUIComponents() {
        dataSet = new EEPROMDataSet(INITAL_DATA_LENGTH);
        byteEditor = new ByteEditor(dataSet);
        dataSizeSpinner = new JSpinner(new SpinnerNumberModel(INITAL_DATA_LENGTH, 1, MAX_DATA_LENGTH, 10));
        serialPortSelection = new JComboBox<>(PortDescriptor.getDescriptors());
        serialPortSelection.setMaximumSize(serialPortSelection.getPreferredSize());
        setupTable();
    }

    private void setupTable() {
        dataTable = new DataTable(this, dataSet, ROW_WIDTH);
    }

    JMenuBar getMenuBar() {
        return menuBar;
    }

    public void updateByteEditor(EventObject e) {

        int row = dataTable.getSelectedRow();
        int col = dataTable.getSelectedColumn();
        int address = row * ROW_WIDTH + col - 1;
        if (row >= 0 && col >= 1 && address >= 0 && address < dataSet.getData().length) {
            byteEditor.setDataByte(dataSet.getByteAt(address));
        } else {
            byteEditor.setDataByte(null);
        }
    }

    public JPanel getContentPane() {
        return contentPane;
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setPreferredSize(new Dimension(1280, 768));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        sidePanel = new JPanel();
        sidePanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(sidePanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        sidePanel.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(10, 10, 10, 10), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.add(dataSizeSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Data size:");
        panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("bytes");
        panel3.add(label2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        writeEEPROMButton = new JButton();
        writeEEPROMButton.setText("Write EEPROM");
        panel2.add(writeEEPROMButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        readEEPROMButton = new JButton();
        readEEPROMButton.setText("Read EEPROM");
        panel2.add(readEEPROMButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkContentsAfterWriting = new JCheckBox();
        checkContentsAfterWriting.setSelected(true);
        checkContentsAfterWriting.setText("Check contents after writing");
        panel2.add(checkContentsAfterWriting, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sidePanel.add(byteEditor.$$$getRootComponent$$$(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        progressBar = new JProgressBar();
        sidePanel.add(progressBar, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        operationStatusLabel = new JLabel();
        operationStatusLabel.setHorizontalAlignment(0);
        operationStatusLabel.setHorizontalTextPosition(0);
        operationStatusLabel.setText("");
        sidePanel.add(operationStatusLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel5.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dataTable.setSurrendersFocusOnKeystroke(false);
        scrollPane1.setViewportView(dataTable);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        hexViewMode = new JRadioButton();
        hexViewMode.setSelected(true);
        hexViewMode.setText("Hexadecimal");
        panel6.add(hexViewMode, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        decimalViewMode = new JRadioButton();
        decimalViewMode.setSelected(false);
        decimalViewMode.setText("Decimal");
        panel6.add(decimalViewMode, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel6.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("View mode:");
        panel6.add(label3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        asciiViewMode = new JRadioButton();
        asciiViewMode.setText("ASCII");
        panel6.add(asciiViewMode, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        refreshSerialPorts = new JButton();
        refreshSerialPorts.setText("Refresh ports");
        panel8.add(refreshSerialPorts, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Serial port:");
        panel9.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serialPortSelection.setAutoscrolls(false);
        panel9.add(serialPortSelection, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel10, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel10.add(panel11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Chip size:");
        panel11.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chipSizeLabel = new JLabel();
        chipSizeLabel.setText("");
        panel11.add(chipSizeLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel10.add(panel12, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Payload size:");
        panel12.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        payloadSizeLabel = new JLabel();
        payloadSizeLabel.setText("");
        panel12.add(payloadSizeLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serialPortStatus = new JLabel();
        Font serialPortStatusFont = this.$$$getFont$$$("Monospaced", Font.BOLD, 14, serialPortStatus.getFont());
        if (serialPortStatusFont != null) serialPortStatus.setFont(serialPortStatusFont);
        serialPortStatus.setHorizontalAlignment(0);
        serialPortStatus.setText("Select a serial port");
        panel7.add(serialPortStatus, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel7.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
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
