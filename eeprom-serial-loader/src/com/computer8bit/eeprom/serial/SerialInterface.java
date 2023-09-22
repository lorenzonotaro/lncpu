package com.computer8bit.eeprom.serial;

import com.computer8bit.eeprom.Window;
import com.fazecast.jSerialComm.SerialPort;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class SerialInterface {
    private static final String PROTOCOL_SIGNATURE = "EEPROMLD";
    private int payloadSize, maxReadWriteLength;
    private static final int DEFAULT_TIMEOUT = 200;
    private static final int BAUD_RATE = 115200;
    private SerialPort activePort;

    public SerialPort getActivePort() {
        return activePort;
    }

    private void requireActivePort() {
        Objects.requireNonNull(activePort, "No active serial port");
    }

    public synchronized void setActivePort(SerialPort activePort) throws SerialException {
        if (this.activePort != null) {
            this.activePort.closePort();
        }
        this.activePort = activePort;
        if (activePort == null)
            return;

        activePort.setComPortParameters(BAUD_RATE, 8, 1, SerialPort.NO_PARITY);

        if (!activePort.openPort())
            throw new SerialException("unable to open port");

        tryDelay(1500);
    }

    public synchronized String getLoaderVersion() throws SerialException {
        requireActivePort();
        byte[] buffer = new byte[32];

        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, DEFAULT_TIMEOUT, 0);

        buffer[0] = 'v';

        if (write(buffer, 1) <= 0) {
            throw new SerialException("error while writing to device");
        }

        if (readUntil(buffer, 0, (byte) '\0') <= 0)
            throw new SerialException("device not responding");

        String val = new String(buffer).trim();

        if (!val.startsWith(PROTOCOL_SIGNATURE))
            throw new SerialException("invalid signature");

        return val.substring(PROTOCOL_SIGNATURE.length());
    }

    public synchronized void setParams() throws SerialException{
        requireActivePort();
        byte[] buffer = new byte[4];
        buffer[0] = 'p';
        if (write(buffer, 1) <= 0){
            throw new SerialException("error while requesting parameters to loader");
        }

        if(read(buffer) != 4){
            throw new SerialException("error while reading parameters from loader");
        }

        maxReadWriteLength = (buffer[0] << 8) + buffer[1];
        payloadSize = (buffer[2] << 8) + buffer[3];
    }

    public synchronized void writeData(byte[] data, Consumer<Integer> progressFunction, Consumer<String> statusFunction, boolean checkContents) throws SerialException {
        byte[] tmpBuffer = new byte[1];
        requireActivePort();

        flushInputBuffer();

        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        try {
            if (data.length != Window.MAX_DATA_LENGTH)
                data = Arrays.copyOf(data, maxReadWriteLength);

            byte[] finalData = data;
            System.out.printf("write: %d are != 0\n", IntStream.range(0, data.length).map(idx -> finalData[idx]).filter(i -> i != 0).count());

            tmpBuffer[0] = 'w';
            if (write(tmpBuffer) <= 0) {
                throw new SerialException("unable to send write request to device");
            }

            int PAYLOAD_COUNT = maxReadWriteLength / payloadSize;
            for (int i = 0; i < PAYLOAD_COUNT; i++) {
                statusFunction.accept("Sending block " + (i + 1) + "/" + PAYLOAD_COUNT);
                if (read(tmpBuffer, 1) != 1)
                    throw new SerialException("unable to read section confirmation character");
                if (tmpBuffer[0] != 'n')
                    throw new SerialException("device sent wrong section confirmation character (" + (char) tmpBuffer[0] + ")");
                if (write(data, payloadSize, i * payloadSize) < payloadSize)
                    throw new SerialException("unable to write to device (block " + (i + 1) + ")");
                progressFunction.accept(Math.round((float) (i + 1) / PAYLOAD_COUNT * 100));
                while (activePort.bytesAwaitingWrite() > 0) tryDelay(1);
            }

            if (read(tmpBuffer, 1) <= 0)
                throw new SerialException("device not responding");

            if (tmpBuffer[0] != 'k')
                throw new SerialException("wrong confirmation character");

            statusFunction.accept("");
            progressFunction.accept(0);
            if(checkContents) {
                byte[] readData = readData(progressFunction, statusFunction);

                statusFunction.accept("Checking data...");
                if (Arrays.compareUnsigned(readData, 0, data.length, data, 0, data.length) != 0)
                    throw new SerialException("data check failed");
            }
        } finally {
            activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, DEFAULT_TIMEOUT, 0);
        }
    }


    public byte[] readData(Consumer<Integer> progressFunction, Consumer<String> statusFunction) throws SerialException {

        requireActivePort();

        flushInputBuffer();

        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        byte[] tmpBuffer = new byte[1];
        int PAYLOAD_COUNT = maxReadWriteLength / payloadSize;

        progressFunction.accept(0);
        byte[] readData = new byte[maxReadWriteLength];
        try {
            tmpBuffer[0] = 'r';
            if (write(tmpBuffer, 1) <= 0)
                throw new SerialException("unable to send read request to device");

            for (int i = 0; i < PAYLOAD_COUNT; i++) {
                statusFunction.accept("Reading block " + (i + 1) + "/" + PAYLOAD_COUNT);
                tmpBuffer[0] = 'n';
                if (write(tmpBuffer, 1) <= 0)
                    throw new SerialException("unable to write section confirmation character during read");
                if (read(readData, payloadSize, i * payloadSize) != payloadSize)
                    throw new SerialException("unable to read from device (block " + (i + 1) + ")");
                progressFunction.accept(Math.round((float) (i + 1) / PAYLOAD_COUNT * 100));
            }

        } finally {
            activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, DEFAULT_TIMEOUT, 0);
        }
        System.out.printf("read: %d are != 0\n", IntStream.range(0, readData.length).map(idx -> readData[idx]).filter(i -> i != 0).count());
        return readData;
    }

    private void flushInputBuffer() {
        int available = activePort.bytesAvailable();
        if (available > 0) {
            byte[] tmp = new byte[available];
            read(tmp, available);
        }
    }

    private int write(byte[] buffer) {
        return activePort.writeBytes(buffer, buffer.length);
    }

    private int write(byte[] buffer, int length, int offset) {
        return activePort.writeBytes(buffer, length, offset);
    }

    private int write(byte[] buffer, int length) {
        return write(buffer, length, 0);
    }

    private int read(byte[] buffer) {
        return read(buffer, buffer.length, 0);
    }

    private int read(byte[] buffer, int length) {
        return read(buffer, length, 0);
    }

    private int readUntil(byte[] buffer, int offset, byte terminator){
        for (int i = 0; i < buffer.length; i++) {
            if (activePort.readBytes(buffer, 1, offset + i) <= 0 || buffer[offset + i] == terminator)
                return i;
        }
        return buffer.length;
    }

    private int read(byte[] buffer, int length, int offset) {
        for (int i = 0; i < length; i++) {
            if (activePort.readBytes(buffer, 1, offset + i) <= 0)
                return i;
        }
        return length;
    }

    private void tryDelay(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            System.out.println("Warning! Caught InterruptedException while delaying!");
        }
    }

    private byte[] toBytes(int i, byte[] buffer) {
        buffer[0] = (byte) ((i >> 24) & 0xFF);
        buffer[1] = (byte) ((i >> 16) & 0xFF);
        buffer[2] = (byte) ((i >> 8) & 0xFF);
        buffer[3] = (byte) (i & 0xFF);
        return buffer;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public int getMaxReadWriteLength() {
        return maxReadWriteLength;
    }
}
