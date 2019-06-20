package com.droid.djs.runner.utils;

import com.droid.djs.consts.NodeType;

public class ThreadUtils extends Utils {

    public static final String SLEEP_NAME = "sleep";
    public static final int SLEEP = 15;

    @Override
    public String name() {
        return "Thread";
    }

    @Override
    public void methods() {
        func(SLEEP_NAME, SLEEP, par("delay", NodeType.NUMBER));
    }
}
