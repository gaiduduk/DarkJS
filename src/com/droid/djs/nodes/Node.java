package com.droid.djs.nodes;

import com.droid.djs.consts.LinkType;
import com.droid.gdb.Bytes;
import com.droid.gdb.InfinityStringArrayCell;

import java.io.InputStream;
import java.util.ArrayList;

public class Node extends SuperNode {

    public boolean isSaved;
    public Long id;
    public DataInputStream data;
    // TODO read external data in another thread
    public InputStream externalData;
    // TODO move type to node body in the storage
    // TODO add flag isData into nodeMeta
    public byte type;
    public Object value;
    public Object source;
    public Object title;
    public Object set;
    public Object _true;
    public Object _else;
    public Object exit;
    public Object _while;
    public Object _if;
    public Object prototype;
    public Object body; //TODO delete
    public Object localParent;
    public Object history; //TODO delete

    public ArrayList<Object> local;
    public ArrayList<Object> param;
    public ArrayList<Object> next;
    public ArrayList<Object> cell;
    public ArrayList<Object> prop;
    public ArrayList<Object> style;
    // after addObject new link you should addObject it to listLinks and parse function

    public Node(byte type) {
        this.type = type;
    }

    @Override
    public byte[] build() {
        ArrayList<Long> links = new ArrayList<>();
        listLinks((linkType, link, singleValue) -> {
            Long linkId = null;
            if (link instanceof Long)
                linkId = (Long) link;
            else if (link instanceof Node)
                linkId = ((Node) link).id;
            long dataLink = linkId * 256L + (long) linkType;
            links.add(dataLink);
        });
        return Bytes.fromLongList(links);
    }

    public void listLinks(NodeLinkListener linkListener) {
        if (linkListener == null)
            return;
        // sequences for well see in Formatter
        if (title != null)
            linkListener.get(LinkType.TITLE, title, true);
        if (value != null)
            linkListener.get(LinkType.VALUE, value, true);
        if (source != null)
            linkListener.get(LinkType.SOURCE, source, true);
        if (set != null)
            linkListener.get(LinkType.SET, set, true);
        if (_true != null)
            linkListener.get(LinkType.TRUE, _true, true);
        if (_else != null)
            linkListener.get(LinkType.ELSE, _else, true);
        if (exit != null)
            linkListener.get(LinkType.EXIT, exit, true);
        if (_while != null)
            linkListener.get(LinkType.WHILE, _while, true);
        if (_if != null)
            linkListener.get(LinkType.IF, _if, true);
        if (prototype != null)
            linkListener.get(LinkType.PROTOTYPE, prototype, true);
        if (body != null)
            linkListener.get(LinkType.BODY, body, true);
        if (localParent != null)
            linkListener.get(LinkType.LOCAL_PARENT, localParent, true);
        if (history != null)
            linkListener.get(LinkType.HISTORY, history, true);
        if (local != null)
            for (Object item : local)
                linkListener.get(LinkType.LOCAL, item, false);
        if (param != null)
            for (Object item : param)
                linkListener.get(LinkType.PARAM, item, false);
        if (next != null)
            for (Object item : next)
                linkListener.get(LinkType.NEXT, item, false);
        if (prop != null)
            for (Object item : prop)
                linkListener.get(LinkType.PROP, item, false);
        if (cell != null)
            for (Object item : cell)
                linkListener.get(LinkType.CELL, item, false);
        if (style != null)
            for (Object item : style)
                linkListener.get(LinkType.STYLE, item, false);
    }


    @Override
    void restore(byte linkType, long linkData) {
        switch (linkType) {
            case LinkType.VALUE:
                value = linkData;
                break;
            case LinkType.SOURCE:
                source = linkData;
                break;
            case LinkType.TITLE:
                title = linkData;
                break;
            case LinkType.SET:
                set = linkData;
                break;
            case LinkType.TRUE:
                _true = linkData;
                break;
            case LinkType.ELSE:
                _else = linkData;
                break;
            case LinkType.EXIT:
                exit = linkData;
                break;
            case LinkType.WHILE:
                _while = linkData;
                break;
            case LinkType.IF:
                _if = linkData;
                break;
            case LinkType.PROTOTYPE:
                prototype = linkData;
                break;
            case LinkType.BODY:
                body = linkData;
                break;
            case LinkType.LOCAL_PARENT:
                localParent = linkData;
                break;
            case LinkType.HISTORY:
                history = linkData;
                break;
            case LinkType.LOCAL:
                if (local == null)
                    local = new ArrayList<>();
                local.add(linkData);
                break;
            case LinkType.PARAM:
                if (param == null)
                    param = new ArrayList<>();
                param.add(linkData);
                break;
            case LinkType.NEXT:
                if (next == null)
                    next = new ArrayList<>();
                next.add(linkData);
                break;
            case LinkType.CELL:
                if (cell == null)
                    cell = new ArrayList<>();
                cell.add(linkData);
                break;
            case LinkType.STYLE:
                if (style == null)
                    style = new ArrayList<>();
                style.add(linkData);
                break;
        }
    }
}
