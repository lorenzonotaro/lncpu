package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.ILabelSectionLocator;
import com.lnasm.compiler.common.SectionInfo;

import java.util.HashMap;

public class ParserLabelSectionLocator extends HashMap<String, LabelBlockInfo> implements ILabelSectionLocator {
    @Override
    public String getSectionName(String label) {
        LabelBlockInfo blockInfo = get(label);
        if (blockInfo != null) {
            return blockInfo.sectionName();
        }
        return null;
    }
}
