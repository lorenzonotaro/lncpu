package com.computer8bit.eeprom.serial;

import com.fazecast.jSerialComm.*;

public class PortDescriptor {
    private final SerialPort port;

    private PortDescriptor(SerialPort port) {
        this.port = port;
    }

    @Override
    public String toString() {
        if(port == null)
            return "";
        if(System.getProperty("os.name").contains("Windows"))
                return port.getSystemPortName() + " (" + port.getDescriptivePortName() + ")";
        else
            return port.getDescriptivePortName();
    }

    public static PortDescriptor[] getDescriptors() {
        PortDescriptor[] descriptors;
        SerialPort[] ports = SerialPort.getCommPorts();
        descriptors = new PortDescriptor[ports.length + 1];
        descriptors[0] = new PortDescriptor(null);
        for (int i = 1; i < ports.length + 1; i++) {
            descriptors[i] = new PortDescriptor(ports[i - 1]);
        }
        return descriptors;
    }

    public SerialPort getPort() {
        return port;
    }
}
