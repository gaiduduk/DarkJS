package com.droid.net.ftp;

import com.droid.djs.node.NodeUtils;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BranchTest {

    @Test
    void test() {
        Branch branch = new Branch();
        String testPath = "tests/BranchTest";
        String branchData = "" + new Random().nextInt();
        NodeUtils.putFile(Master.getInstance(), testPath, "sdf");
        String str = NodeUtils.getFileString(Master.getInstance(), testPath);
        NodeUtils.putFile(branch.getRoot(), testPath, branchData);
        branch.mergeWithMaster();
        String masterData = NodeUtils.getFileString(Master.getInstance(), testPath);
        assertEquals(branchData, masterData);
    }
}