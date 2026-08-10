package com.lnc.cc.optimization.ir;

import com.lnc.cc.ast.BinaryExpression;
import com.lnc.cc.ast.UnaryExpression;
import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.ImmediateOperand;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.types.TypeSpecifier;

import java.util.HashMap;
import java.util.Map;

public class ConstantFoldingPass extends IRPass {
    private final ConstantPropagationEvaluator evaluator;
    private final Map<VirtualRegister, Integer> knownConstants = new HashMap<>();

    public ConstantFoldingPass() {
        this.evaluator = new ConstantPropagationEvaluator();
    }

    @Override
    protected void visit(IRBlock block) {
        knownConstants.clear();
        super.visit(block);
    }

    @Override
    public Void visit(Goto aGoto) {
        return null;
    }

    @Override
    public Void visit(CondJump condJump) {

        PropValue condValue = propagateCondition(condJump);

        if (condValue.isConstant()) {
            if (condValue.valueOr(0) == 0) {
                // If condition is false, redirect to the false target
                super.replaceAndContinue(new Goto(condJump.getFalseTarget()));
            } else {
                // If condition is true, redirect to the true target
                super.replaceAndContinue(new Goto(condJump.getTarget()));
            }
            markAsChanged();
        }

        return null;
    }

    private PropValue propagateCondition(CondJump condJump) {
        PropValue left = evaluate(condJump.getLeft());
        PropValue right = evaluate(condJump.getRight());

        if(left.isConstant() && right.isConstant()) {
            return switch (condJump.getCond()) {
                case EQ -> PropValue.constant(left.valueOr(0) == right.valueOr(0) ? 1 : 0);
                case NE -> PropValue.constant(left.valueOr(0) != right.valueOr(0) ? 1 : 0);
                case LT -> PropValue.constant(left.valueOr(0) < right.valueOr(0) ? 1 : 0);
                case LE -> PropValue.constant(left.valueOr(0) <= right.valueOr(0) ? 1 : 0);
                case GT -> PropValue.constant(left.valueOr(0) > right.valueOr(0) ? 1 : 0);
                case GE -> PropValue.constant(left.valueOr(0) >= right.valueOr(0) ? 1 : 0);
            };
        }

        return PropValue.unknown();
    }

    @Override
    public Void visit(Move move) {
        PropValue value = evaluate(move.getSource());
        recordWrites(move);
        if (value.isConstant() && move.getDest() instanceof VirtualRegister dest) {
            knownConstants.put(dest, truncate(value.valueOr(0), dest.getTypeSpecifier()));
        }
        return null;
    }

    @Override
    public Void visit(Ret ret) {
        return null;
    }

    @Override
    public Void visit(Bin bin) {
        PropValue left = evaluate(bin.getLeft());
        PropValue right = evaluate(bin.getRight());

        if (left.isConstant() && right.isConstant()) {
            Integer folded = fold(bin.getOperator(),
                    truncate(left.valueOr(0), bin.getLeft().getTypeSpecifier()),
                    truncate(right.valueOr(0), bin.getRight().getTypeSpecifier()));
            if (folded != null) {
                replaceWithConstant(bin.getDest(), folded);
                return null;
            }
        }

        recordWrites(bin);
        return null;
    }

    @Override
    public Void visit(Call call) {
        recordWrites(call);
        return null;
    }

    @Override
    public Void visit(Push push) {
        return null;
    }

    @Override
    public Void visit(Pop pop) {
        recordWrites(pop);
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        PropValue operand = evaluate(unary.getOperand());

        if (operand.isConstant()) {
            Integer folded = fold(unary.getOperator(),
                    truncate(operand.valueOr(0), unary.getOperand().getTypeSpecifier()));
            if (folded != null) {
                replaceWithConstant(unary.getTarget(), folded);
                return null;
            }
        }

        recordWrites(unary);
        return null;
    }

    private void replaceWithConstant(IROperand dest, int value) {
        int truncated = truncate(value, dest.getTypeSpecifier());
        replaceAndContinue(new Move(new ImmediateOperand(truncated, dest.getTypeSpecifier()), dest));
        if (dest instanceof VirtualRegister vr) {
            knownConstants.put(vr, truncated);
        }
        markAsChanged();
    }

    private PropValue evaluate(IROperand operand) {
        if (operand instanceof VirtualRegister vr) {
            Integer known = knownConstants.get(vr);
            return known == null ? PropValue.unknown() : PropValue.constant(known);
        }
        return operand.accept(evaluator);
    }

    private void recordWrites(IRInstruction instruction) {
        for (VirtualRegister written : instruction.getWrites()) {
            knownConstants.remove(written);
        }
    }

    private static int truncate(int value, TypeSpecifier type) {
        return type != null && type.allocSize() == 1 ? value & 0xFF : value & 0xFFFF;
    }

    private static Integer fold(BinaryExpression.Operator operator, int left, int right) {
        return switch (operator) {
            case ADD -> left + right;
            case SUB -> left - right;
            case AND -> left & right;
            case OR -> left | right;
            case XOR -> left ^ right;
            case SHL -> right >= 0 && right < 16 ? left << right : null;
            case SHR -> right >= 0 && right < 16 ? left >>> right : null;
            default -> null;
        };
    }

    private static Integer fold(UnaryExpression.Operator operator, int operand) {
        return switch (operator) {
            case NOT -> ~operand;
            case NEGATE -> -operand;
            default -> null;
        };
    }
}
