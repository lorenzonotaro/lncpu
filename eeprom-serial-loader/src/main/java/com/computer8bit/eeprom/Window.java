package com.computer8bit.eeprom;

import com.computer8bit.eeprom.data.EEPROMDataByte;
import com.computer8bit.eeprom.data.EEPROMDataSet;
import com.computer8bit.eeprom.data.FileIO;
import com.computer8bit.eeprom.serial.PortDescriptor;
import com.computer8bit.eeprom.serial.SerialException;
import com.computer8bit.eeprom.serial.SerialInterface;
import com.computer8bit.eeprom.table.DataTable;
import com.computer8bit.eeprom.table.DataTableModel;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
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
        hexViewMode.addActionListener(e -> {if(hexViewMode.isSelected()) ((DataTableModel)dataTable.getModel()).setViewMode(HEXADECIMAL);});
        decimalViewMode.addActionListener(e -> {if(decimalViewMode.isSelected()) ((DataTableModel)dataTable.getModel()).setViewMode(DECIMAL);});
        asciiViewMode.addActionListener(e -> {if(asciiViewMode.isSelected()) ((DataTableModel)dataTable.getModel()).setViewMode(ASCII);});
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
                JOptionPane.showMessageDialog(null, "Unable to write to EEPROM: " + ex.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
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
                if(dataSet.getLength() > serialInterface.getMaxReadWriteLength()){
                    int response = JOptionPane.showConfirmDialog(null, "You are trying to upload more data than the loader allows. Do you wish to continue (the additional data will be ignored)?", "Data length mismatch", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if(response != JOptionPane.YES_OPTION)
                        return;
                }
                serialInterface.writeData(dataSet.getDataAsBytes(), progressBar::setValue, operationStatusLabel::setText, checkContentsAfterWriting.isSelected());
                operationStatusLabel.setText("Done.");
            } catch (SerialException ex) {
                JOptionPane.showMessageDialog(null, "Unable to write to EEPROM: " + ex.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
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
            if(!filename.endsWith(".eeprom")) filename += ".eeprom";
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
}
