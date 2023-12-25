package com.computer8bit.eeprom.data;

import com.computer8bit.eeprom.event.EEPROMDataChangeEvent;
import com.computer8bit.eeprom.event.EEPROMDataChangeListener;
import com.computer8bit.eeprom.util.EditHistory;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EEPROMDataSet {

    private EditHistory editHistory;
    private EEPROMDataByte[] data;
    private List<EEPROMDataChangeListener> changeListeners;

    public EEPROMDataSet(int initialLength){
        data = new EEPROMDataByte[initialLength];
        for (int i = 0; i < initialLength; i++) {
            data[i] = new EEPROMDataByte(this, i);
        }
        changeListeners = new ArrayList<>();
    }

    public int getLength(){
        return data.length;
    }

    public EEPROMDataByte getByteAt(int index){
        return data[index];
    }

    public byte getByteValueAt(int index){
        return getByteAt(index).getValue();
    }

    void dataChanged(Object source, EEPROMDataChangeEvent.Type type) {
        EEPROMDataChangeEvent event = new EEPROMDataChangeEvent(source, type);
        EventQueue.invokeLater(() -> changeListeners.forEach(l -> l.dataChanged(event)));
    }

    public void addChangeListener(EEPROMDataChangeListener changeListener) {
        changeListeners.add(changeListener);
    }

    public void resize(int newLength) {
        System.out.println("EEPROMDataSet.resize");
        int oldLength = data.length;
        if(oldLength == newLength) return;
        data = Arrays.copyOf(data, newLength);
        for (int i = oldLength; i < newLength; i++) {
            data[i] = new EEPROMDataByte(this, i);
        }
        dataChanged(this, EEPROMDataChangeEvent.Type.RESIZE);
    }

    public void setByteAt(int index, EEPROMDataByte dataByte) {
        data[index] = dataByte;
        dataByte.address = index;
        dataChanged(this, EEPROMDataChangeEvent.Type.UPDATE);
    }

    public EEPROMDataByte[] getData() {
        return data;
    }

    public void setData(EEPROMDataByte[] newData) {
        this.data = newData;
        for (EEPROMDataByte dataByte : newData) {
            dataByte.dataSet = this;
        }
        dataChanged(this, EEPROMDataChangeEvent.Type.RESIZE);
    }

    public void setData(byte[] data) {
        this.data = new EEPROMDataByte[data.length];
        for (int i = 0; i < data.length; i++) {
            this.data[i] = new EEPROMDataByte(data[i], this, i);
        }

        dataChanged(this, EEPROMDataChangeEvent.Type.RESIZE);
    }

    public byte[] getDataAsBytes() {
        return Arrays.stream(data).map(EEPROMDataByte::getValue).collect(
                ByteArrayOutputStream::new,
                ByteArrayOutputStream::write,
                (a, b) -> {}).toByteArray();
    }

}
