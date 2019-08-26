package com.droid.djs.fs;

import com.droid.djs.serialization.js.JsBuilder;
import com.droid.djs.serialization.js.JsParser;
import com.droid.djs.serialization.json.JsonBuilder;
import com.droid.djs.serialization.json.JsonParser;
import com.droid.djs.nodes.NodeBuilder;
import com.droid.djs.nodes.Data;
import com.droid.djs.nodes.Node;
import com.droid.instance.Instance;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.*;
import java.util.Random;

public class DataOutputStream extends OutputStream {


    public final static File ftpTempDir = new File("out/FtpTemp");
    public final static Random random = new Random();
    private Node node;
    private File tempFile = new File(ftpTempDir, "" + random.nextInt());
    private FileOutputStream out;
    private Branch branch;
    private Instance instance;

    public DataOutputStream(Instance instance, Branch branch, Node node) {
        this.instance = instance;
        this.branch = branch;
        this.node = node;
        try {
            out = new FileOutputStream(tempFile);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
        branch.updateTimer();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        branch.updateTimer();
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        branch.updateTimer();
    }


    @Override
    public void close() {
        try {
            out.close();
            boolean connectionOpened = Instance.connectThreadIfNotConnected(instance);
            Instance.get();
            Files.putFile(node, new FileInputStream(tempFile));
            if (connectionOpened)
                Instance.disconnectThread();
            tempFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
