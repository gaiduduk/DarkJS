package org.pdk.files;

import org.pdk.store.NodeBuilder;
import org.pdk.store.model.DataOrNode;
import org.pdk.store.model.node.Node;

import java.util.Arrays;

public class Files {

    public static String getPath(NodeBuilder builder, Node root, Node file) {
        builder.set(file);
        StringBuilder path = new StringBuilder();
        while (builder.getLocalParent() != null && builder.getNode() != root) {
            path.insert(0, "/" + builder.getTitle());
            Node localParent = builder.getLocalParent();
            builder.set(localParent);
        }
        return "".equals(path.toString()) ? "/" : path.toString();
    }

    // TODO create getNode with creator first
    public static Node getNode(NodeBuilder builder, Node root, String path, boolean createIfNotExist) {
        builder.set(root);
        // TODO add escape characters /
        if (path != null && !path.equals("")) {
            String[] names = path.split("/");
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                if (name.equals("")) continue;
                String type = null;
                if (name.contains(".")) {
                    int dotPos = name.indexOf('.');
                    type = name.substring(dotPos + 1);
                    name = name.substring(0, dotPos);
                }

                boolean find = false;
                for (DataOrNode don : builder.getLocals()) {
                    if (don instanceof Node) {
                        Node node = (Node) don;
                        if (name.equals(builder.set(node).getTitle())) {
                            builder.set(node);
                            find = true;
                            break;
                        }
                    }
                }

                if (!find) {
                    if (createIfNotExist) {
                        Node newNode;
                        if (i == names.length - 1) { // it`s the last
                            newNode = builder.create().setTitle(name).commit();
                        } else {
                            newNode = builder.create().setTitle(name).setParser(type).commit();
                        }
                        builder.addLocal(newNode).commit();
                    } else {
                        return null;
                    }
                }
            }
        }
        return builder.getNode();
    }

    public static Node getNodeIfExist(NodeBuilder builder, Node root, String path){
        return getNode(builder, root, path, false);
    }

    public static Node getNodeIfExist(NodeBuilder builder, String path){
        return getNode(builder, builder.getMaster(), path, false);
    }

    public static Node getNodeFromRoot(NodeBuilder builder, String path, boolean createIfNotExist){
        return getNode(builder, builder.getRoot(), path, createIfNotExist);
    }
    public static Node getNodeFromRootIfExist(NodeBuilder builder, String path){
        return getNode(builder, builder.getRoot(), path, false);
    }

    public static void replace(NodeBuilder builder, Node from, Node to) {
        Node localParent = builder.set(from).getLocalParent();
        DataOrNode[] parentLocals = builder.set(localParent).getLocals();
        int localIndex = Arrays.asList(parentLocals).indexOf(from);
        builder.set(localParent).setLocal(localIndex, to).commit();
        String title = builder.set(from).getTitle();
        String parser = builder.set(from).getParser();
        builder.set(to).setTitle(title).setParser(parser).commit();
    }

    public static void remove(NodeBuilder builder, Node root) {
        Node localParent = builder.set(root).getLocalParent();
        builder.set(localParent).removeLocal(root);
    }

    public interface FindFile {
        void file(DataOrNode node);
    }

    public void observe(NodeBuilder builder, String path, FindFile findFile) {
        Node node = getNodeIfExist(builder, path);
        if (node != null)
            observeRec(builder, node, findFile);
    }

    private static void observeRec(NodeBuilder builder, Node node, FindFile findFile) {
        Node[] locals = builder.set(node).getLocals();
        for (Node local : locals) {
            findFile.file(local);
            observeRec(builder, local, findFile);
        }
    }
}
