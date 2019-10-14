package org.pdk.files.converters.js;

import jdk.nashorn.internal.ir.*;
import jdk.nashorn.internal.parser.TokenType;
import org.pdk.files.Files;
import org.pdk.files.converters.ConverterBuilder;
import org.pdk.modules.root.MathModule;
import org.pdk.store.NodeBuilder;
import org.pdk.store.model.DataOrNode;
import org.pdk.store.model.data.Data;
import org.pdk.store.model.data.NumberData;
import org.pdk.store.model.data.StringData;
import org.pdk.store.model.node.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.pdk.modules.root.MathModule.*;

public class JsBuilder extends ConverterBuilder {

    private ArrayList<Node> localStack = new ArrayList<>();

    public JsBuilder(NodeBuilder builder) {
        super(builder);
    }

    @Override
    public Node build(Node module, Object parseResult) {
        addParentsToLocalStack(module);
        if (module == null)
            module = builder.create().commit();
        Block program = ((FunctionNode) parseResult).getBody();
        return (Node) jsLine(module, program);
    }

    DataOrNode jsLine(Node module, jdk.nashorn.internal.ir.Node statement) {
        try {
            if (module != null)
                localStack.add(module);

            if (statement instanceof VarNode) {
                VarNode varNode = (VarNode) statement;
                if (varNode.getInit() instanceof FunctionNode) {
                    return jsLine(module, varNode.getInit());
                } else {
                    Node node = (Node) jsLine(module, varNode.getName());
                    if (varNode.getInit() != null) {
                        DataOrNode setLink = jsLine(node, varNode.getInit());
                        node = builder.create().setSource(node).setSet(setLink).commit();
                    }
                    return node;
                }
            }

            if (statement instanceof FunctionNode) {
                FunctionNode function = (FunctionNode) statement;
                Node func = Files.getNode(builder, module, function.getName());
                for (IdentNode param : function.getParameters()) {
                    Node paramNode = builder.create().setTitle(param.getName()).commit();
                    builder.set(func).addParam(paramNode).commit();
                }
                jsLine(func, function.getBody());
                return func;
            }

            if (statement instanceof Block) {
                Block block = (Block) statement;
                //module.next = null;

                Map<Node, Block> subBlocks = new LinkedHashMap<>();
                for (jdk.nashorn.internal.ir.Node line : block.getStatements()) {
                    Node lineNode = (Node) jsLine(module, line);
                    if (line instanceof VarNode && ((VarNode) line).getInit() instanceof FunctionNode) // not a function. its a problem with nashorn parser.
                        subBlocks.put(lineNode, ((FunctionNode) ((VarNode) line).getInit()).getBody());
                    else
                        builder.set(module).addNext(lineNode).commit();
                }
                for (Node lineNode : subBlocks.keySet()) {
                    Block subBlock = subBlocks.get(lineNode);
                    jsLine(lineNode, subBlock);
                }
                return builder.set(module).commit();
            }

            if (statement instanceof ReturnNode) {
                ReturnNode returnNode = (ReturnNode) statement;
                DataOrNode setNode = jsLine(module, returnNode.getExpression());
                return builder.create()
                        .setSource(module)
                        .setSet(setNode)
                        .setExit(module)
                        .commit();
            }

            if (statement instanceof BinaryNode) {
                BinaryNode binaryNode = (BinaryNode) statement;
                DataOrNode left = jsLine(module, binaryNode.lhs());
                DataOrNode right = jsLine(module, binaryNode.rhs());
                if (binaryNode.isAssignment()) {
                    if (binaryNode.tokenType() == TokenType.ASSIGN_ADD ||
                            binaryNode.tokenType() == TokenType.ASSIGN_SUB ||
                            binaryNode.tokenType() == TokenType.ASSIGN_MUL ||
                            binaryNode.tokenType() == TokenType.ASSIGN_DIV) {
                        Node nativeFunc = Files.getNodeFromRootIfExist(builder, MathModule.MATH_UTIL_NAME + "/" + convertTokenTypeToFuncName(binaryNode.tokenType()));
                        right = builder.create()
                                .setFunc(nativeFunc.func)
                                .addParam(left)
                                .addParam(right)
                                .commit();
                    }
                    return builder.create()
                            .setSource((Node) left)
                            .setSet(right)
                            .commit();
                } else {
                    Node nativeFunc = Files.getNodeFromRootIfExist(builder,
                            MathModule.MATH_UTIL_NAME + "/" + convertTokenTypeToFuncName(binaryNode.tokenType()));
                    if (nativeFunc == null)
                        throw new NullPointerException();
                    return builder.create()
                            .setFunc(nativeFunc.func)
                            .addParam(left)
                            .addParam(right)
                            .commit();
                }
            }


            if (statement instanceof IdentNode) {
                IdentNode identNode = (IdentNode) statement;
                String name = identNode.getName();

                Node ident = null;
                for (int i = localStack.size() - 1; i >= 0; i--) {
                    Node node = localStack.get(i);
                    Node findNode = Files.getNodeIfExist(builder, node, name);
                    if (findNode == null) {
                        if (node.param != null)
                            for (DataOrNode param : builder.set(node).getParams())
                                if (param instanceof Node) {
                                    Node paramNode = (Node) param;
                                    if (name.equals(builder.set(paramNode).getTitle()))
                                        findNode = paramNode;
                                }
                    }
                    if (findNode != null) {
                        ident = findNode;
                        break;
                    }
                }
                if (ident == null) {
                    ident = builder.create().commit();
                    builder.set(ident).setTitle(identNode.getName()).commit();
                    builder.set(module).addLocal(ident).commit();
                }
                return ident;
            }

            if (statement instanceof ExpressionStatement) {
                ExpressionStatement expressionStatement = (ExpressionStatement) statement;
                return jsLine(module, expressionStatement.getExpression());
            }

            if (statement instanceof UnaryNode) {
                UnaryNode unaryNode = (UnaryNode) statement;
                // TODO addObject ++a --a
                TokenType tokenType = unaryNode.tokenType();
                if (tokenType == TokenType.INCPOSTFIX || tokenType == TokenType.DECPOSTFIX) {
                    Node variable = (Node) jsLine(module, unaryNode.getExpression());
                    Node nativeNode = Files.getNodeFromRootIfExist(builder,
                            MathModule.MATH_UTIL_NAME + "/" + convertTokenTypeToFuncName(tokenType));
                    Node func = builder.create()
                            .setFunc(nativeNode.func)
                            .addParam(variable)
                            .commit();
                    return builder.create()
                            .setSource(variable)
                            .setValue(variable) // important
                            .setSet(func)
                            .commit();
                } else if (tokenType.toString().equals("-")) {
                    Node nativeNode = Files.getNodeFromRootIfExist(builder,
                            MathModule.MATH_UTIL_NAME + "/" + MathModule.UNARY_MINUS);
                    DataOrNode expression = jsLine(module, unaryNode.getExpression());
                    return builder.create()
                            .setFunc(nativeNode.func)
                            .addParam(expression)
                            .commit();
                } else {
                    return jsLine(module, unaryNode.getExpression());
                }
            }

            if (statement instanceof CallNode) {
                CallNode call = (CallNode) statement;
                Node callNode = builder.create().commit();
                for (jdk.nashorn.internal.ir.Node arg : call.getArgs()) {
                    DataOrNode argNode = jsLine(module, arg);
                    builder.set(callNode).addParam(argNode);
                }

                Node sourceFunc = (Node) jsLine(module, call.getFunction());
                return builder.set(callNode)
                        .setSource(sourceFunc)
                        .commit();
            }

            if (statement instanceof LiteralNode) {
                LiteralNode literalNode = (LiteralNode) statement;

                if (literalNode.isNull()) {
                    return builder.create().commit();
                } else {
                    Data data = null;
                    if (literalNode.isNumeric())
                        data = new NumberData(Double.valueOf(literalNode.getString()));
                    else if (literalNode.isString())
                        data = new StringData(literalNode.getString().getBytes());
                    return data;
                }
            }

            return null;
        } finally {
            localStack.remove(module);
        }
    }


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

    private void addParentsToLocalStack(Node module) {
        if (module != null) {
            Node parent = builder.set(module).getLocalParent();
            while (parent != null) {
                localStack.add(parent);
                parent = builder.set(parent).getLocalParent();
            }
            Collections.reverse(localStack);
        }
    }
}
