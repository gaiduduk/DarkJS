package com.droid.djs.nodes;

import com.droid.djs.consts.LinkType;
import com.droid.gdb.Bytes;
import com.droid.gdb.InfinityStringArrayCell;

public abstract class SuperNode implements InfinityStringArrayCell {

    @Override
    public byte[] build() {
        return new byte[0];
    }

    @Override
    public void parse(byte[] data) {
        long[] links = Bytes.toLongArray(data);
        for (long dataLink : links) {
            byte linkType = (byte) (dataLink % 256);
            long linkData = (dataLink - linkType) / 256;
            restore(LinkType.values()[linkType], linkData);
        }
    }

    abstract void restore(LinkType linkType, long linkData);

    public interface NodeLinkListener {
        void get(LinkType linkType, Object link, boolean singleValue);
    }

    abstract void listLinks(Node.NodeLinkListener linkListener);
}