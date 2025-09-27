package com.lnc.cc.optimization.asm;

import com.lnc.assembler.common.LabelInfo;
import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.assembler.parser.argument.LabelRef;
import com.lnc.common.ExtendedListIterator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class UselessLabelPass extends AbstractAsmLevelLinearPass {

    private HashMap<String, Integer> labelCounts = new HashMap<>();

    @Override
    public Boolean visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator) {
        return false;
    }

    @Override
    public Boolean visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator) {

        boolean changed = false;

        for (Iterator<LabelInfo> it = instruction.getLabels().iterator(); it.hasNext(); ) {
            String labelName = it.next().token().lexeme;
            if (labelName.startsWith("_") && labelCounts.getOrDefault(labelName, 0) == 0) {
                it.remove();
                changed = true;
            }
        }

        return changed;
    }

    @Override
    public boolean runPass(LinkedList<CodeElement> code) {

        labelCounts.clear();
        for (ExtendedListIterator<CodeElement> iterator = new ExtendedListIterator<>(code); iterator.hasNext();) {
            CodeElement elem = iterator.next();
            if(elem instanceof Instruction instr && instr.isShortJump() && instr.getArguments().length == 1 && instr.getArguments()[0].type == Argument.Type.LABEL) {
                String labelName = ((LabelRef)(instr.getArguments()[0])).labelToken.lexeme;
                if(labelName.startsWith("_"))
                    labelCounts.put(labelName, labelCounts.getOrDefault(labelName, 0) + 1);
            }
        }

        return super.runPass(code);
    }
}
