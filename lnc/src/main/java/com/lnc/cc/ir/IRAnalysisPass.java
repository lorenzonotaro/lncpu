package com.lnc.cc.ir;

import com.lnc.cc.optimization.ir.IRPass;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.Logger;

/**
 * The IRAnalysisPass class is a specialized implementation of the {@code IRPass} abstract class,
 * tailored to perform analysis on LLVM-like Intermediate Representation (IR) structures.
 * This pass focuses on inspecting and modifying IR blocks and instructions,
 * ensuring the validity and consistency of control flow and return statements within the IR.
 *
 * It provides overrides for visiting specific IR instructions, such as
 * {@code Goto}, {@code CondJump}, {@code Move}, {@code Ret}, {@code Bin}, {@code Call}, {@code Push},
 * and {@code Unary}. The primary functionality includes visiting IR blocks in a graphical traversal
 * approach, backed by reverse post-order traversal defined in the {@code IRPass} base class.
 *
 * Features of IRAnalysisPass:
 * - Automatically ensures that control-flow blocks without successors or a return instruction
 *   emit a {@code Ret} operation with appropriate warning messages in case of non-void functions.
 * - Provides hooks for handling different types of IR instructions, albeit with placeholder
 *   (empty) implementations for extensions or further specialization.
 * - Signals changes in the IR by marking the pass as changed when modifications are made,
 *   facilitating optimization or analysis pipelines that rely on detecting alterations to the IR.
 *
 * Core methods:
 * - {@code visit(Goto aGoto)}: Handles {@code Goto} instructions.
 * - {@code visit(CondJump condJump)}: Handles conditional jump instructions.
 * - {@code visit(Move move)}: Handles move operations between operands.
 * - {@code visit(Ret ret)}: Analyzes and processes return statements.
 * - {@code visit(Bin bin)}: Represents operations involving binary instructions.
 * - {@code visit(Call call)}: Processes function or method call instructions.
 * - {@code visit(Push push)}: Handles stack-related push operations.
 * - {@code visit(Unary unary)}: Handles unary instructions such as negation or increment.
 * - {@code visit(IRBlock block)}: Examines a single IR block, appending a return statement if necessary.
 *
 * This class provides the foundation for further IR-centric optimization, validation,
 * or transformations that extend its provided behavior.
 */
public class IRAnalysisPass extends IRPass {
    @Override
    public Void visit(Goto aGoto) {
        return null;
    }

    @Override
    public Void visit(CondJump condJump) {
        return null;
    }

    @Override
    public Void visit(Move move) {
        return null;
    }

    @Override
    public Void visit(Ret ret) {
        return null;
    }

    @Override
    public Void visit(Bin bin) {
        return null;
    }

    @Override
    public Void visit(Call call) {
        return null;
    }

    @Override
    public Void visit(Push push) {
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        return null;
    }

    @Override
    protected void visit(IRBlock block) {
        if(block.getSuccessors().isEmpty() && !(block.last instanceof Ret)){
            // If the block has no successors and is not a return block, we add a return instruction
            // to ensure that the control flow is well-defined.
            if (getUnit().getFunctionType().returnType.type != TypeSpecifier.Type.VOID) {
                Logger.warning(String.format("no return statement in function '%s' returning non-void", getUnit().getFunctionDeclaration().name.lexeme));
            }
            block.emit(new Ret(null));
            markAsChanged();
        }
        super.visit(block);
    }
}
