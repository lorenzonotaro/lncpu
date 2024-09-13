package com.computer8bit.eeprom.table;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public record DiffElement(com.computer8bit.eeprom.table.DiffElement.Type type, String displayString) {

    public enum Type {

        ADDRESS(() -> UIManager.getColor("Table.background")),
        SAME(() -> UIManager.getColor("Table.background")),
        DIFFERENT(() -> new Color(255, 235, 137)),
        MISSING(() -> new Color(255, 116, 116)),
        ADDED(() -> new Color(87, 255, 87));

        public final Supplier<Color> colorGetter;

        Type(Supplier<Color> colorGetter) {
            this.colorGetter = colorGetter;
        }

    }
}
