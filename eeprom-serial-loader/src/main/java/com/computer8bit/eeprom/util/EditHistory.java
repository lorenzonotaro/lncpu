package com.computer8bit.eeprom.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class EditHistory {
    private List<IHistoryItem> entries;
    private int index;
    public EditHistory() {
        this.entries = new ArrayList<>();
    }
}
