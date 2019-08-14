package com.droid.instance;

import com.droid.djs.fs.Branch;
import com.droid.djs.fs.DataOutputStream;
import com.droid.djs.fs.Files;
import com.droid.djs.treads.Threads;
import com.droid.gdb.map.Crc16;
import com.droid.net.ftp.FtpServer;
import com.droid.net.http.HttpServer;
import com.droid.net.ws.WsClientServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Instance implements Runnable {

    private InstanceParameters instanceParameters;
    private String intallDir;
    private HttpServer http;
    private FtpServer ftp;
    private WsClientServer ws;
    private static Map<Long, InstanceParameters> parameters = new HashMap<>();

    public static InstanceParameters get() {
        return parameters.get(Thread.currentThread().getId());
    }

    public Instance(int addToPortNumber, String storageDir, String intallDir, String nodename, String proxyhost, String login, String password) {
        this.intallDir = intallDir;
        long accessToken = (long) Crc16.getHash(login + password);
        instanceParameters = new InstanceParameters(addToPortNumber, storageDir, nodename, accessToken, proxyhost);
    }

    public static void put(InstanceParameters instanceParameters) {
        parameters.put(Thread.currentThread().getId(), instanceParameters);
    }
    public static InstanceParameters find(int addToPortNumber) {
        for (Long threadID: parameters.keySet())
            if (parameters.get(threadID).instanceID == addToPortNumber)
                return parameters.get(threadID);
        return null;
    }

    public static void connectThreadByPortAdditional(int addToPort) {
        put(find(addToPort));
    }

    public static void disconnectThread() {
        parameters.remove(Thread.currentThread().getId());
    }


    @Override
    public void run() {
        put(instanceParameters);

        loadingBranch = new Branch();
        loadProject(intallDir, "root", false);
        loadingBranch.mergeWithMaster();

        testRootIndex();

        Threads.getInstance().run(Instance.get().getMaster(), null, false, instanceParameters.accessToken);

        try {
            http = new HttpServer();
            http.start();
            ftp = new FtpServer();
            ftp.start();
            ws = new WsClientServer();
            ws.start();

            //waiting
            http.join();
        } catch (Exception e) {
            if (http != null)
                http.stop();
            if (ftp != null)
                ftp.stop();
            if (ws != null)
                ws.stop();
            e.printStackTrace();
        }
    }

    private static void testRootIndex() {
        System.out.println("loading " + (Files.getNodeIfExist("/root/index") != null ? "success" : "fail"));
    }

    private Branch loadingBranch;
    private void loadProject(String projectPath, String localPath, boolean deleteDir) {
        File root = new File(projectPath);
        File[] list = root.listFiles();
        if (list == null) return;
        if (localPath.equals("root"))
            System.out.println("create load " + projectPath);
        for (File file : list) {
            String localFileName = localPath + "/" + file.getName();
            if (file.isDirectory()) {
                loadProject(file.getAbsolutePath(), localFileName, deleteDir);
            } else {
                try {
                    DataOutputStream dataOutputStream = new DataOutputStream(loadingBranch,
                            Files.getNode(loadingBranch.getRoot(), localFileName));
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fileInputStream.read(buffer)) != -1)
                        dataOutputStream.write(buffer, 0, len);
                    fileInputStream.close();
                    dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (deleteDir)
                    file.delete();
            }
        }
        if (deleteDir)
            root.delete();
        if (localPath.equals("root"))
            System.out.println("finish load " + projectPath);
    }

}
