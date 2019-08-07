package com.droid.djs.runner.utils;

import com.droid.djs.nodes.NodeBuilder;
import com.droid.djs.nodes.consts.NodeType;
import com.droid.djs.fs.Files;
import com.droid.djs.nodes.NativeNode;
import com.droid.djs.nodes.Node;
import com.droid.djs.runner.Func;
import com.droid.djs.runner.prototypes.StringPrototype;

import java.util.*;

abstract public class Utils {

    private static NodeBuilder builder = new NodeBuilder();
    public static String DEFAULT_PROTOTYPES_DIR = "defaultPrototypes/";
    private static Node defaultPrototypesDir;
    private static List<Func> functions = new ArrayList<>();
    private static List<FuncInterface> interfaces = new ArrayList<>();

    public static Node trueValue = builder.createBool(true);
    public static Node falseValue = builder.createBool(false);

    public Utils() {
        methods();
    }

    public static List<Func> getFunctions() {
        if (functions.size() == 0) {
            new StringPrototype();
            new ThreadUtils();
            new MathUtils();
            new RootUtils();
            new NodeUtils();
            new Net();
            new Console();
            new Instance();
        }
        return functions;
    }

    public static Node getDefaultPrototypesDir() {
        if (defaultPrototypesDir == null)
            defaultPrototypesDir = Files.getNode(DEFAULT_PROTOTYPES_DIR);
        return defaultPrototypesDir;
    }

    public void func(String name, Func func, Parameter... args) {
        functions.add(func);
        interfaces.add(new FuncInterface((name().endsWith("/") ? name() : name() + "/"), name, Arrays.asList(args)));
    }

    public static void saveInterfaces() {
        for (int i = 0; i < interfaces.size(); i++) {
            FuncInterface funcInterface = interfaces.get(i);
            String functionName = funcInterface.path + funcInterface.name;
            NativeNode function = (NativeNode) Files.getNode(functionName, NodeType.NATIVE_FUNCTION);
            builder.set(function).setFunctionIndex(i);
            for (Parameter parameter : funcInterface.parameters) {
                Node param = parNode(parameter);
                builder.set(function).addParam(param).commit();
            }
        }
    }

    public static FuncInterface getFunctionInterface(int index) {
        return interfaces.get(index);
    }

    private static Node parNode(Parameter parameter) {
        Node title = builder.createString(parameter.name);
        Node defValue;
        if (parameter.nodeType == NodeType.NUMBER)
            defValue = builder.createNumber(0D);
        else if (parameter.nodeType == NodeType.STRING)
            defValue = builder.createString("string");
        else
            defValue = builder.create(parameter.nodeType).commit();
        return builder.create().setTitle(title).setValue(defValue).commit();
    }

    public Parameter par(String name, NodeType nodeType) {
        return new Parameter(name, nodeType);
    }

    Object getObject(int index, NodeBuilder builder, Node node) {
        return toObject(builder, builder.set(node).getParamNode(index));
    }

    Object firstObject(NodeBuilder builder, Node node) {
        return getObject(0, builder, node);
    }

    Object secondObject(NodeBuilder builder, Node node) {
        return getObject(1, builder, node);
    }

    String getString(int index, NodeBuilder builder, Node node) {
        Object leftObj = getObject(index, builder, node);
        if (leftObj != null)
            return (String) leftObj;
        return null;
    }

    String firstString(NodeBuilder builder, Node node) {
        return getString(0, builder, node);
    }

    String secondString(NodeBuilder builder, Node node) {
        return getString(1, builder, node);
    }

    Double getNumber(int index, NodeBuilder builder, Node node, Double def) {
        Object leftObj = getObject(index, builder, node);
        if (leftObj != null)
            return (Double) leftObj;
        return def;
    }

    protected Object toObject(NodeBuilder builder, Node node) {
        builder.set(node);
        if (node != null) node = builder.set(node).getValueOrSelf();
        Object obj = null;
        if (node != null && node.type.ordinal() < NodeType.NODE.ordinal())
            obj = builder.set(node).getData().getObject();
        return obj;
    }

    public static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public abstract String name();

    public abstract void methods();
}
