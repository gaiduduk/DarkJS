package org.pdk.store.model.node;

import org.pdk.modules.ModuleManager;
import org.pdk.modules.utils.Func;
import org.pdk.store.model.node.link.LinkType;

public class NativeNode extends Node {

    public Integer functionIndex;
    public Func func;

    @Override
    public void listLinks(NodeLinkListener linkListener) {
        super.listLinks(linkListener);
        if (functionIndex != null)
            linkListener.get(LinkType.NATIVE_FUNCTION, functionIndex, true);
    }

    @Override
    public void restore(LinkType linkType, Object linkData) {
        super.restore(linkType, linkData);
        if (linkType == LinkType.NATIVE_FUNCTION) {
            setFunctionIndex((int) (double) (Double) linkData);
        }
    }

    public void setFunctionIndex(int functionIndex) {
        this.functionIndex = functionIndex;
        func = ModuleManager.functions.get(this.functionIndex);
    }
}
