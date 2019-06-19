package com.droid.djs.nodes;

import com.droid.djs.consts.LinkType;
import com.droid.djs.consts.NodeType;
import com.droid.djs.runner.Runner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

public class ThreadNode extends Node implements Runnable {

    public Thread thread;
    private Runner runner = new Runner();
    private Long access_owner = null;
    private ArrayList<Long> access_user = null;


    public ThreadNode() {
        super(NodeType.THREAD);
    }

    @Override
    public void listLinks(NodeLinkListener linkListener) {
        super.listLinks(linkListener);
        if (access_owner != null)
            linkListener.get(LinkType.ACCESS_OWNER, access_owner, true);
        if (access_user != null)
            for (Long access_item : access_user)
                linkListener.get(LinkType.ACCESS_USER, access_item, false);
    }

    @Override
    void restore(byte linkType, long linkData) {
        super.restore(linkType, linkData);
        switch (linkType) {
            case LinkType.ACCESS_OWNER:
                access_owner = linkData;
                break;
            case LinkType.ACCESS_USER:
                if (access_user == null)
                    access_user = new ArrayList<>();
                access_user.add(linkData);
                break;
        }
    }

    class RunData {
        Node node;
        Map<String, String> args;

        public RunData(Node node, Map<String, String> args) {
            this.node = node;
            this.args = args;
        }
    }

    private LinkedList<RunData> runQueue = new LinkedList<>();

    public void run(Node node, boolean async, Long access_code) {
        boolean secure_enabled = access_owner != null || access_user != null;
        if (secure_enabled) {
            boolean access_granted = (access_owner != null && access_owner.equals(access_code))
                    || (access_user != null && access_user.indexOf(access_code) != -1);
            if (!access_granted)
                return;
        }
        runQueue.add(new RunData(node, null));
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(this);
            thread.start();
        }
        if (!async) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void run() {
        while (runQueue.size() > 0) {
            RunData data = runQueue.pollFirst();
            runner.start(data.node);
        }
    }

}
