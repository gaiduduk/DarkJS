package com.droid.net.ftp;

import com.droid.djs.nodes.NodeBuilder;
import com.droid.instance.Instance;
import com.guichaguri.minimalftp.FTPConnection;
import com.guichaguri.minimalftp.FTPServer;
import com.guichaguri.minimalftp.api.IFTPListener;

import java.io.IOException;
import java.net.InetAddress;

public class FtpServer implements IFTPListener {

    public static NodeBuilder builder = new NodeBuilder();
    private FTPServer server = new FTPServer();
    public static int defaultPort = 21;

    public FtpServer(Integer port) {
        System.out.println("FtpServer" + Thread.currentThread().getId());
        server.setAuthenticator(new FtpAuthenticator());
        server.addListener(this);
        server.setTimeout(3000);
        server.setBufferSize(1024 * 5);
        try {
            server.listen(InetAddress.getByName("localhost"), (port == null ? defaultPort : port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        server.dispose();
    }

    @Override
    public void onConnected(FTPConnection con) {
        // connect to Instance in FtpAuthenticator class
        System.out.println("onConnected" + Thread.currentThread().getId());
    }

    @Override
    public void onDisconnected(FTPConnection con) {
        System.out.println("onDisconnected" + Thread.currentThread().getId());
    }
}