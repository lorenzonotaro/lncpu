package com.computer8bit.eeprom.util;

import com.computer8bit.eeprom.table.ViewMode;

public class Util {
    public static byte parseByte(String strVal, ViewMode viewMode){
        int value;
        if(strVal.startsWith("0x")){
            value = Integer.parseInt(strVal.substring(2).toUpperCase(), 16);
        }else if(ViewMode.HEXADECIMAL.equals(viewMode)){
            value = Integer.parseInt(strVal.toUpperCase(), 16);
        }else if(ViewMode.ASCII.equals(viewMode)){
            value = strVal.charAt(strVal.length() - 1);
        }else{
            value = Integer.parseInt(strVal, 10);
        }
        if(value < 0 || value > 255)
            throw new NumberFormatException("not in range");
        return (byte) (value & 0xFF);
    }
}
