package com.computer8bit.eeprom.data;

import com.computer8bit.eeprom.event.EEPROMDataChangeEvent;

import java.util.Arrays;

public class EEPROMDataByte {

    transient EEPROMDataSet dataSet;
    int address;
    private byte value;
    private String byteLabel;
    private final String[] bitLabels;

    public EEPROMDataByte(EEPROMDataSet dataSet, int address){
        this((byte) 0, dataSet, address);
    }

    public EEPROMDataByte(byte value, EEPROMDataSet dataSet, int address){
        this.dataSet = dataSet;
        this.address = address;
        this.value = value;
        this.byteLabel = "";
        this.bitLabels = new String[8];
        for (int i = 0; i < 8; i++) {
            this.bitLabels[i] = "";
        }
    }

    public EEPROMDataByte(EEPROMDataByte other) {
        this.dataSet = other.dataSet;
        this.address = other.getAddress();
        this.value = other.value;
        this.byteLabel = other.byteLabel;
        this.bitLabels = Arrays.copyOf(other.bitLabels, other.bitLabels.length);
    }

    public String getBitLabel(int index) {
        return bitLabels[index];
    }

    public void setBitLabel(int index, String value){
        if(bitLabels[index].equals(value)) return;
        bitLabels[index] = value;
        dataSet.dataChanged(this, EEPROMDataChangeEvent.Type.UPDATE);
    }

    public String getByteLabel() {
        return byteLabel;
    }

    public void setByteLabel(String byteLabel) {
        if(this.byteLabel.equals(byteLabel)) return;
        this.byteLabel = byteLabel;
        dataSet.dataChanged(this, EEPROMDataChangeEvent.Type.UPDATE);
    }

    public byte getValue() {
        return value;
    }

    public void setValue(byte value) {
        if(this.value == value) return;
        this.value = value;
        dataSet.dataChanged(this, EEPROMDataChangeEvent.Type.UPDATE);
    }

    public int getAddress() {
        return address;
    }
}
