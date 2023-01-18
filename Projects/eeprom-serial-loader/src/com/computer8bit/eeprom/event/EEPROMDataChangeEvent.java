package com.computer8bit.eeprom.event;

import javax.swing.event.ChangeEvent;

public class EEPROMDataChangeEvent extends ChangeEvent {

    private final Type type;

    public enum Type{
        UPDATE, RESIZE
    }

    /**
     * Constructs a ChangeEvent object.
     *
     * @param source the Object that is the source of the event
     *               (typically <code>this</code>)
     */
    public EEPROMDataChangeEvent(Object source, Type type) {
        super(source);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
