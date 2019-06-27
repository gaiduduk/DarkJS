package com.droid.djs.runner;

import com.droid.djs.fs.Files;
import com.droid.djs.nodes.NativeNode;
import com.droid.djs.runner.utils.NodeUtils;
import com.droid.djs.runner.utils.ThreadUtils;
import com.droid.djs.runner.utils.UiUtils;
import com.droid.djs.serialization.node.Formatter;
import com.droid.djs.nodes.Node;
import com.droid.djs.builder.NodeBuilder;
import com.droid.djs.consts.NodeType;
import jdk.nashorn.internal.parser.TokenType;

public class Caller {

    // TODO change const system to search tree system without funcID
    public final static int UNARY_MINUS = -1;
    public final static int EQ = 0; // ==
    public final static int ADD = 1; // +
    public final static int SUB = 2; // -
    public final static int DIV = 3; // /
    public final static int MUL = 4; // *
    public final static int INC = 5; // +1
    public final static int DEC = 6; // -1
    public final static int GT = 7; // >
    public final static int GE = 8; // >=
    public final static int LT = 9; // <
    public final static int LE = 10; // <=
    public final static int STRING_REVERCE = 11; // "abc".reverse() -> "cba"
    public final static String STRING_REVERCE_NAME = "reverse";
    public static final int STRING_TRIM = 12;
    public final static String STRING_TRIM_NAME = "trim";


    private static NodeBuilder builder = new NodeBuilder();

    public static int fromTokenType(TokenType tokenType) {
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
                return GT;
            case GE:
                return GE;
            case LT:
                return LT;
            case LE:
                return LE;
        }
        return EQ;
    }

    private static Node trueValue = builder.create(NodeType.BOOL).setData(Formatter.TRUE).commit();
    private static Node falseValue = builder.create(NodeType.BOOL).setData(Formatter.FALSE).commit();

    public static void invoke(Node node, Node ths) {
        builder.set(node);

        Node left = builder.getParamNode(0);
        Node right = builder.getParamNode(1);

        if (ths != null) ths = builder.set(ths).getValueOrSelf();
        if (left != null) left = builder.set(left).getValueOrSelf();
        if (right != null) right = builder.set(right).getValueOrSelf();

        Object thsObject = null;
        Object leftObject = null;
        Object rightObject = null;

        if (ths != null && ths.type.ordinal() < NodeType.NODE.ordinal()) thsObject = builder.set(ths).getData().getObject();
        if (left != null && left.type.ordinal() < NodeType.NODE.ordinal()) leftObject = builder.set(left).getData().getObject();
        if (right != null && right.type.ordinal() < NodeType.NODE.ordinal()) rightObject = builder.set(right).getData().getObject();

        Object firstObject = leftObject;
        Object secondObject = rightObject;

        Node resultNode = null;
        try {
            switch (((NativeNode) node).functionId) {
                case EQ:
                    resultNode = (leftObject != null && leftObject.equals(rightObject)) ? trueValue : falseValue;
                    break;
                case ADD:
                    if (leftObject instanceof String && rightObject instanceof String) {
                        String newString = leftObject + (String) rightObject;
                        resultNode = builder.create(NodeType.STRING).setData(newString).commit();
                    } else if (leftObject instanceof Double && rightObject instanceof Double) {
                        Double newString = (Double) leftObject + (Double) rightObject;
                        resultNode = builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    break;
                case SUB:
                    if (leftObject instanceof Double && rightObject instanceof Double) {
                        Double newString = (Double) leftObject - (Double) rightObject;
                        resultNode = builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    break;
                case MUL:
                    if (leftObject instanceof Double && rightObject instanceof Double) {
                        Double newString = (Double) leftObject * (Double) rightObject;
                        resultNode = builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    break;
                case DIV:
                    if (leftObject instanceof Double && rightObject instanceof Double) {
                        Double newString = (Double) leftObject / (Double) rightObject;
                        resultNode = builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    break;
                case UNARY_MINUS:
                    if (leftObject instanceof Double) {
                        Double newString = -(Double) leftObject;
                        resultNode = builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    break;
                case INC:
                    if (leftObject instanceof Double) {
                        Double newString = (Double) leftObject + 1;
                        resultNode = builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    break;
                case DEC:
                    if (leftObject instanceof Double) {
                        Double newString = (Double) leftObject - 1;
                        resultNode = builder.create(NodeType.NUMBER).setData(newString).commit();
                    }
                    break;
                case GT:
                    if (leftObject instanceof Double && rightObject instanceof Double) {
                        resultNode = ((Double) leftObject > (Double) rightObject) ? trueValue : falseValue;
                    }
                    break;
                case GE:
                    if (leftObject instanceof Double && rightObject instanceof Double) {
                        resultNode = ((Double) leftObject >= (Double) rightObject) ? trueValue : falseValue;
                    }
                    break;
                case LT:
                    if (leftObject instanceof Double && rightObject instanceof Double) {
                        resultNode = ((Double) leftObject < (Double) rightObject) ? trueValue : falseValue;
                    }
                    break;
                case LE:
                    if (leftObject instanceof Double && rightObject instanceof Double) {
                        resultNode = ((Double) leftObject <= (Double) rightObject) ? trueValue : falseValue;
                    }
                    break;
                case STRING_REVERCE:
                    if (thsObject instanceof String) {
                        String newString = new StringBuilder().append((String) thsObject).reverse().toString();
                        resultNode = builder.create(NodeType.STRING).setData(newString).commit();
                    }
                    break;
                case STRING_TRIM:
                    if (thsObject instanceof String) {
                        resultNode = builder.create(NodeType.STRING).setData(((String) thsObject).trim()).commit();
                    }
                    break;
                case ThreadUtils.SLEEP:
                    if (firstObject instanceof Double) {
                        Thread.sleep(((Double) firstObject).longValue());
                    }
                    break;
                case NodeUtils.GET:
                    if (firstObject instanceof String && right instanceof Node) {

                    }
                    break;
                case NodeUtils.PATH:
                    if (left instanceof Node) {
                        String path = Files.getPath(left);
                        resultNode = builder.create(NodeType.STRING).setData(path).commit();
                    }
                    break;
                case UiUtils.SEND:
                    if (leftObject instanceof String && right instanceof Node) {

                    }
                    break;
            }
        } catch (Exception e) {
            // TODO addObject exceptions to djs
        }
        builder.set(node).setValue(resultNode).commit();
    }
}
