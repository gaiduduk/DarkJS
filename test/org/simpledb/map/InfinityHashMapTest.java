package org.simpledb.map;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class InfinityHashMapTest {
    @Test
    void put() {
        int randomInt = new Random(System.currentTimeMillis()).nextInt(1000);
        InfinityHashMap tree = new InfinityHashMap("out/SimpleGraphDB", "map" + randomInt);
        assertEquals( TreeNode.SIZE, tree.fileData.sumFilesSize);
        tree.put("String1", "3012".getBytes(), 123);
        assertEquals( TreeNode.SIZE, tree.fileData.sumFilesSize);
        tree.put("String2", "3021".getBytes(), 123);
        assertEquals(2 * TreeNode.SIZE, tree.fileData.sumFilesSize);
        tree.put("String2", "3021".getBytes(), 345);
        assertEquals(2 * TreeNode.SIZE, tree.fileData.sumFilesSize);
        tree.put("String3", "3100".getBytes(), 234);
        assertEquals(3 * TreeNode.SIZE, tree.fileData.sumFilesSize);
        long value = tree.get("String3", "3100".getBytes());
        assertEquals(234, value);
        value = tree.get("String2", "3021".getBytes());
        assertEquals(345, value);
        value = tree.get("String1", "3012".getBytes());
        assertEquals(123, value);
        // TODO addObject tests with letters abcdef in hash
    }
}