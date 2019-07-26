package com.droid.djs.runner.utils;

import com.droid.djs.nodes.consts.NodeType;

public class Console extends Utils {
    @Override
    public String name() {
        return "Console";
    }

    @Override
    public void methods() {
        func("log", (builder, node, ths) -> {
            String message = firstString(builder, node);
            System.out.println(message);
            return builder.createBool(true);
        }, par("message", NodeType.STRING));
    }
}
