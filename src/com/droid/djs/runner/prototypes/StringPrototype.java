package com.droid.djs.runner.prototypes;

import com.droid.djs.nodes.consts.NodeType;
import com.droid.djs.runner.utils.Utils;

public class StringPrototype extends Utils {

    @Override
    public String name() {
        return DEFAULT_PROTOTYPES_DIR + capitalize(NodeType.STRING.toString());
    }

    @Override
    public void methods() {
        func("reverse", (builder, ths) -> {
            Object thsObject = toObject(builder.set(ths));
            if (thsObject instanceof String) {
                String newString = new StringBuilder().append((String) thsObject).reverse().toString();
                return builder.create(NodeType.STRING).setData(newString).commit();
            }
            return null;
        });
        func("trim", (builder, ths) -> {
            Object thsObject = toObject(builder.set(ths));
            if (thsObject instanceof String) {
                return builder.create(NodeType.STRING).setData(((String) thsObject).trim()).commit();
            }
            return null;
        });

    }
}
