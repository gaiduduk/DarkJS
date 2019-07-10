package com.droid.djs.serialization.json;

import com.droid.djs.nodes.Data;
import com.droid.djs.nodes.Node;
import com.droid.djs.nodes.NodeBuilder;
import com.droid.djs.nodes.consts.NodeType;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Map;

public class JsonBuilder {

    private static NodeBuilder builder = new NodeBuilder();

    public static void build(Node file, JsonElement je) {
        Node localParent = builder.set(file).getLocalParentNode();
        Node sourceCode = builder.set(file).getSourceCodeNode();
        Data parser = builder.set(file).getParserNode();
        Node obj = build(je);
        file.parse(obj.build());
        builder.set(file)
                .setLocalParent(localParent)
                .setSourceCode(sourceCode)
                .setParser(parser)
                .commit();
    }

    public static Node build(JsonElement je) {
        /*if (je.isJsonNull())
            return "null";*/

        if (je.isJsonPrimitive()) {
            JsonPrimitive primitive = je.getAsJsonPrimitive();
            if (primitive.isBoolean())
                return builder.create(NodeType.BOOL).setData(primitive.getAsBoolean()).commit();
            if (primitive.isString())
                return builder.create(NodeType.STRING).setData(primitive.getAsString()).commit();
            if (primitive.isNumber())
                return builder.create(NodeType.NUMBER).setData(primitive.getAsDouble()).commit();
        }

        if (je.isJsonArray()) {
            Node arr = builder.create(NodeType.ARRAY).getNode();
            for (JsonElement item : je.getAsJsonArray()) {
                Node node = build(item);
                builder.set(arr).addCell(node).commit();
            }
            return arr;
        }

        if (je.isJsonObject()) {
            Node obj = builder.create().getNode();
            for (Map.Entry<String, JsonElement> e : je.getAsJsonObject().entrySet()) {
                Node value = build(e.getValue());
                Node title = builder.create(NodeType.STRING).setData(e.getKey()).commit();
                Node var = builder.create().setTitle(title).setValue(value).commit();
                builder.set(obj).addLocal(var).commit();
            }
            return obj;
        }
        return null;
    }
}
