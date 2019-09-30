package org.simpledb.map;

import org.simpledb.Bytes;
import org.simpledb.InfinityConstArray;
import org.simpledb.InfinityStringArray;

import java.io.IOException;
import java.util.Arrays;

public class InfinityHashMap extends InfinityConstArray {
    // TODO delete new and use only 3 objects to search
    // TODO HALF_LONG value change to max map node count
    private final static long HALF_LONG = 1000000000000000000L;
    private InfinityStringArray keys;
    private InfinityStringArray hashes;

    public InfinityHashMap(String infinityFileDir, String infinityFileName) {
        super(infinityFileDir, infinityFileName);
        keys = new InfinityStringArray(infinityFileDir, infinityFileName + ".keys");
        hashes = new InfinityStringArray(infinityFileDir,infinityFileName + ".hashes");
        if (fileData.sumFilesSize == 0)
            add(new TreeNode("****".getBytes(), new long[TreeNode.LINKS_COUNT]));
    }

    public void put(String str, byte[] hash, long value) {
        put(str.getBytes(), hash, value);
    }

    byte hexCharToInt(byte ch) {
        if (ch >= 0 && ch <= '9')
            ch -= '0';
        else if (ch >= 'a' && ch <= 'z')
            ch -= 'a';
        return ch;
    }

    public void put(byte[] hashKey, byte[] hash, long value) {
        TreeNode node = new TreeNode();
        long prevIndex = Long.MAX_VALUE;
        long nodeIndex = 0;
        int i = 0;
        while (i < TreeNode.SIZE) {
            get(nodeIndex, node);
            byte hashChar = hash[i];
            byte nodeChar = node.mask[i];
            if (nodeChar == 0)
                throw new NullPointerException(); // TODO link NodeStorageTest
            while (nodeChar == hashChar && i + 1 < TreeNode.SIZE) {
                i++;
                hashChar = hash[i];
                nodeChar = node.mask[i];
            }
            if (nodeChar == '*') {
                hashChar = hexCharToInt(hashChar);
                long link = node.links[hashChar];
                if (link == 0) {
                    Hash hash1 = new Hash(getFirst8Bytes(hashKey), keys.addBytes(hashKey), value);
                    HashVariants hashVariants = new HashVariants(hash, hash1);
                    node.links[hashChar] = hashes.addObject(hashVariants) + HALF_LONG;
                    set(nodeIndex, node);
                    return;
                } else if (link < HALF_LONG) {
                    prevIndex = nodeIndex;
                    nodeIndex = link;
                } else /* hashIndex >= HALF_LONG */ {
                    long hashVariantIndex = link - HALF_LONG;
                    HashVariants hashVariants = new HashVariants();
                    hashes.getObject(hashVariantIndex, hashVariants);
                    if (Arrays.equals(hash, hashVariants.mask)) {
                        long first8Bytes = getFirst8Bytes(hashKey);
                        boolean findKey = false;
                        for (Hash hashl : hashVariants.hashes)
                            if (hashl.first8Bytes == first8Bytes)
                                if (Arrays.equals(hashKey, keys.getBytes(hashl.keyIndex))) {
                                    hashl.value = value;
                                    findKey = true;
                                    break;
                                }
                        if (!findKey) {
                            long keyIndex = keys.addBytes(hashKey);
                            Hash newHash = new Hash(first8Bytes, keyIndex, value);
                            hashVariants.hashes.add(newHash);
                        }
                        hashes.setObject(hashVariantIndex, hashVariants);
                    } else {
                        byte[] newMask = "****".getBytes();
                        int j = 0;
                        for (; hash[j] == hashVariants.mask[j]; j++)
                            newMask[j] = hash[j];
                        long[] links = new long[TreeNode.LINKS_COUNT];
                        int previousIndex = hexCharToInt(hashVariants.mask[j]);
                        int newIndex = hexCharToInt(hash[j]);
                        links[previousIndex] = link;
                        links[newIndex] = hashes.addObject(
                                new HashVariants(hash, new Hash(getFirst8Bytes(hashKey), keys.addBytes(hashKey), value))
                        ) + HALF_LONG;
                        node.links[hashChar] = add(new TreeNode(newMask, links));
                        set(nodeIndex, node);
                    }
                    return;
                }
            } else {
                nodeChar = hexCharToInt(nodeChar);
                hashChar = hexCharToInt(hashChar);
                byte[] newMask = "****".getBytes();
                System.arraycopy(node.mask, 0, newMask, 0, i);
                long[] links = new long[TreeNode.LINKS_COUNT];
                links[nodeChar] = nodeIndex;
                links[hashChar] = hashes.addObject(new HashVariants(hash, new Hash(getFirst8Bytes(hashKey), keys.addBytes(hashKey), value))) + HALF_LONG;
                long newIndex = add(new TreeNode(newMask, links));
                if (prevIndex != Long.MAX_VALUE) {
                    get(prevIndex, node);
                    int previousPositionInHash = hexCharToInt(hash[i - 1]);
                    node.links[previousPositionInHash] = newIndex;
                    set(prevIndex, node);
                }
                return;
            }
        }
    }

    public long get(String str, byte[] hash) {
        return get(str.getBytes(), hash);
    }

    public long get(byte[] hashKeyFirst8Bytes, byte[] hash) {
        TreeNode node = new TreeNode();
        long nodeIndex = 0;
        int i = 0;
        while (i < TreeNode.SIZE) {
            node = (TreeNode) get(nodeIndex, node);
            byte hashChar = hash[i];
            byte nodeChar = node.mask[i];
            while (nodeChar == hashChar && i + 1 < TreeNode.SIZE) {
                i++;
                hashChar = hash[i];
                nodeChar = node.mask[i];
            }
            if (nodeChar == '*') {
                hashChar = hexCharToInt(hashChar);
                long link = node.links[hashChar];
                if (link == 0) {
                    return Long.MAX_VALUE;
                } else if (link < HALF_LONG) {
                    nodeIndex = link;
                } else /* hashIndex >= HALF_LONG */ {
                    long hashVariantIndex = link - HALF_LONG;
                    HashVariants hashVariants = new HashVariants();
                    hashes.getObject(hashVariantIndex, hashVariants);
                    if (Arrays.equals(hash, hashVariants.mask)) {
                        long first8Bytes = getFirst8Bytes(hashKeyFirst8Bytes);
                        for (Hash hashl : hashVariants.hashes)
                            if (hashl.first8Bytes == first8Bytes)
                                if (Arrays.equals(hashKeyFirst8Bytes, keys.getString(hashl.keyIndex).getBytes()))
                                    return hashl.value;
                    }
                    return Long.MAX_VALUE;
                }
            } else {
                return Long.MAX_VALUE;
            }
        }
        return Long.MAX_VALUE;
    }

    private long getFirst8Bytes(byte[] hashKey) {
        byte[] bytes = Arrays.copyOfRange(hashKey, 0, Math.min(7, hashKey.length - 1));
        byte[] firstly8Bytes = new byte[Long.BYTES];
        System.arraycopy(bytes, 0, firstly8Bytes, 0, bytes.length);
        return Bytes.toLong(firstly8Bytes);
    }

    public void close() throws IOException {
        super.close();
        keys.close();
        hashes.close();
    }

}
