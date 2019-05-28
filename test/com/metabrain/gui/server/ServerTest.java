package com.metabrain.gui.server;

import com.metabrain.net.http.Server;
import org.junit.jupiter.api.Test;

class ServerTest {

    @Test
    void join() {
        try {
            Server server = new Server(9080);
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}