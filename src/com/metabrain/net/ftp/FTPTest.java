package com.metabrain.net.ftp;

import com.guichaguri.minimalftp.FTPConnection;
import com.guichaguri.minimalftp.FTPServer;
import com.guichaguri.minimalftp.api.IFTPListener;
import com.metabrain.djs.node.Node;
import com.metabrain.djs.node.NodeBuilder;
import com.metabrain.djs.node.NodeStorage;
import com.metabrain.djs.node.NodeType;
import com.metabrain.net.auth.UserbaseAuthenticator;

import java.io.IOException;
import java.net.InetAddress;

public class FTPTest implements IFTPListener {

    public static NodeBuilder builder = new NodeBuilder();

    static Node addDir(Long nodeId, String titleStr){
        Node title = builder.create(NodeType.STRING).setData(titleStr).commit();
        Node local = builder.create().setTitle(title).setLocalParent(nodeId).commit();
        builder.get(nodeId).addLocal(local).commit();
        return local;
    }

    private static void addFile(Long nodeId, String titleStr, String fileData) {
        Node title = builder.create(NodeType.STRING).setData(titleStr).commit();
        Node data = builder.create(NodeType.STRING).setData(fileData).commit();
        Node local = builder.create().setTitle(title).setLocalParent(nodeId).setValue(data).commit();
        builder.get(nodeId).addLocal(local).commit();
    }

    static void initTestStorage() {
        if (builder.get(0L).getLocalCount() == 0){
            addDir(0L, "first");
            addDir(0L, "second");
            addFile(0L, "firstFile", "fileData");
            Node third = addDir(0L, "third");
            addDir(third.id, "t1");
            addDir(third.id, "t2");
            addDir(third.id, "t3");
            addDir(third.id, "t4");
            addFile(third.id, "firstFile2", "fileData2");
            NodeStorage.getInstance().transactionCommit();
        }
    }

    public static void main(String[] args) throws IOException {
        FTPServer server = new FTPServer();

        UserbaseAuthenticator auth = new UserbaseAuthenticator();

        initTestStorage();
        auth.registerUser("john", "1234");
        auth.registerUser("alex", "abcd123");
        auth.registerUser("hannah", "98765");

        server.setAuthenticator(auth);
        server.addListener(new FTPTest());
        server.setTimeout(10 * 60 * 1000); // 10 minutes
        server.setBufferSize(1024 * 5); // 5 kilobytes
        server.listenSync(InetAddress.getByName("localhost"), 21);
    }

    @Override
    public void onConnected(FTPConnection con) {

    }

    @Override
    public void onDisconnected(FTPConnection con) {
        // You can use this event to dispose resources related to the connection
        // As the instance of CommandHandler is only held by the command, it will
        // be automatically disposed by the JVM
    }
}