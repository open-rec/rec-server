package com.openrec.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class FileUtilTest {

    private static final String TEST_READ_FILE = "graph.json";

    @Test
    public void read() {
        String res = FileUtil.read(TEST_READ_FILE);
        System.out.println(res);
        Assert.assertTrue(StringUtils.isNotEmpty(res));
    }
}