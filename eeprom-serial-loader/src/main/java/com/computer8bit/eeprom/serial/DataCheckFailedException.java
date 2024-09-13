package com.computer8bit.eeprom.serial;

public class DataCheckFailedException extends SerialException {
    private final byte[] readData;

    public DataCheckFailedException(byte[] readData) {
        super("data check failed");
        this.readData = readData;
    }

    public byte[] getReadData() {
        return readData;
    }
}
