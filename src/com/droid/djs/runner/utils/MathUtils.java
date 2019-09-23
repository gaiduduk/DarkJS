package com.droid.djs.runner.utils;

import com.droid.djs.nodes.Node;
import com.droid.djs.nodes.consts.NodeType;
import jdk.nashorn.internal.parser.TokenType;

import java.util.Objects;

public class MathUtils extends Utils {

    public final static String MATH_UTIL_NAME = "Math";
    public final static String UNARY_MINUS = "unaryMinus";
    public final static String EQ = "equals"; // ==
    public final static String ADD = "add"; // +
    public final static String SUB = "sub"; // -
    public final static String DIV = "div"; // /
    public final static String MUL = "mul"; // *
    public final static String INC = "inc"; // +1
    public final static String DEC = "dec"; // -1
    public final static String MORE = "more"; // >
    public final static String MORE_OR_EQUAL = "moreOrEqual"; // >=
    public final static String LESS = "less"; // <
    public final static String LESS_OR_EQUAL = "lessOrEqual"; // <=

    public static String convertTokenTypeToFuncName(TokenType tokenType) {
        switch (tokenType) {
            case EQ:
                return EQ;
            case ADD:
                return ADD;
            case SUB:
                return SUB;
            case DIV:
                return DIV;
            case MUL:
                return MUL;
            case ASSIGN_ADD:
                return ADD;
            case ASSIGN_SUB:
                return SUB;
            case ASSIGN_DIV:
                return DIV;
            case ASSIGN_MUL:
                return MUL;
            case INCPOSTFIX:
                return INC;
            case DECPOSTFIX:
                return DEC;
            case GT:
                return MORE;
            case GE:
                return MORE_OR_EQUAL;
            case LT:
                return LESS;
            case LE:
                return LESS_OR_EQUAL;
        }
        return EQ;
    }

    @Override
    public String name() {
        return MATH_UTIL_NAME;
    }

    @Override
    public void methods() {
        func(UNARY_MINUS, (builder, ths) -> {
            Node currentNode = builder.getNode();
            Object leftObject = firstObject(builder);
            if (leftObject instanceof Double) {
                Double newString = -(Double) leftObject;
                return builder.create(NodeType.NUMBER).setData(newString).commit();
            }
            return currentNode;
        }, par("par1", NodeType.NUMBER));

        func(EQ, (builder, ths) -> {
                    Object leftObject = firstObject(builder);
                    Object rightObject = secondObject(builder);
                    return (Objects.equals(leftObject, rightObject))
                            ? builder.createBool(true) : builder.createBool(false);
                }, par("par1", NodeType.NUMBER),
                par("par2", NodeType.NUMBER));

        func(ADD, (builder, ths) -> {
                    Object leftObject = firstObject(builder);
                    Object rightObject = secondObject(builder);
                    if (leftObject instanceof String && rightObject instanceof String) {
                        String newString = leftObject + (String) rightObject;
                        return builder.create(NodeType.STRING).setData(newString).commit();
                    } else if (leftObject instanceof Double && rightObject instanceof Double) {
                        Double newString = (Double) leftObject + (Double) rightObject;
                        return builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    return null;
                }, par("par1", NodeType.NUMBER),
                par("par2", NodeType.NUMBER));

        func(SUB, (builder, ths) -> {
                    Object leftObject = firstObject(builder);
                    Object rightObject = secondObject(builder);
                    if (leftObject instanceof Double && rightObject instanceof Double) {
                        Double newString = (Double) leftObject - (Double) rightObject;
                        return builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    return null;
                }, par("par1", NodeType.NUMBER),
                par("par2", NodeType.NUMBER));

        func(MUL, (builder, ths) -> {
                    Object leftObject = firstObject(builder);
                    Object rightObject = secondObject(builder);
                    if (leftObject instanceof Double && rightObject instanceof Double) {
                        Double newString = (Double) leftObject * (Double) rightObject;
                        return builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    return null;
                }, par("par1", NodeType.NUMBER),
                par("par2", NodeType.NUMBER));

        func(DIV, (builder, ths) -> {
                    Object leftObject = firstObject(builder);
                    Object rightObject = secondObject(builder);
                    if (leftObject instanceof Double && rightObject instanceof Double) {
                        Double newString = (Double) leftObject / (Double) rightObject;
                        return builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    return null;
                }, par("par1", NodeType.NUMBER),
                par("par2", NodeType.NUMBER));

        func(INC, (builder, ths) -> {
            Object leftObject = firstObject(builder);
            if (leftObject instanceof Double) {
                Double newString = (Double) leftObject + 1;
                return builder.create(NodeType.NUMBER).setData(newString).commit();
            }
            return null;
        }, par("par1", NodeType.NUMBER));

        func(DEC, (builder, ths) -> {
            Object leftObject = firstObject(builder);
            if (leftObject instanceof Double) {
                Double newString = (Double) leftObject - 1;
                return builder.create(NodeType.NUMBER).setData(newString).commit();
            }
            return null;
        }, par("par1", NodeType.NUMBER));

        func(MORE, (builder, ths) -> {
            Object leftObject = firstObject(builder);
            Object rightObject = secondObject(builder);
            if (leftObject instanceof Double && rightObject instanceof Double)
                return ((Double) leftObject > (Double) rightObject) ? builder.createBool(true) : builder.createBool(false);
            return null;
        }, par("par1", NodeType.NUMBER), par("par2", NodeType.NUMBER));

        func(MORE_OR_EQUAL, (builder, ths) -> {
            Object leftObject = firstObject(builder);
            Object rightObject = secondObject(builder);
            if (leftObject instanceof Double && rightObject instanceof Double)
                return ((Double) leftObject >= (Double) rightObject) ? builder.createBool(true) : builder.createBool(false);
            return null;
        }, par("par1", NodeType.NUMBER), par("par2", NodeType.NUMBER));

        func(LESS, (builder, ths) -> {
            Object leftObject = firstObject(builder);
            Object rightObject = secondObject(builder);
            if (leftObject instanceof Double && rightObject instanceof Double)
                return ((Double) leftObject < (Double) rightObject) ? builder.createBool(true) : builder.createBool(false);
            return null;
        }, par("par1", NodeType.NUMBER), par("par2", NodeType.NUMBER));


        func(LESS_OR_EQUAL, (builder, ths) -> {
            Object leftObject = firstObject(builder);
            Object rightObject = secondObject(builder);
            if (leftObject instanceof Double && rightObject instanceof Double)
                return ((Double) leftObject <= (Double) rightObject) ? builder.createBool(true) : builder.createBool(false);
            return null;
        }, par("par1", NodeType.NUMBER), par("par2", NodeType.NUMBER));

    }
}
