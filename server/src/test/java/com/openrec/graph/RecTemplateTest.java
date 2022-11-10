package com.openrec.graph;

import org.junit.Assert;
import org.junit.Test;

public class RecTemplateTest {

    @Test
    public void test() {
        GraphConfig graphConfig = RecTemplate.toGraphConfig();
        Assert.assertNotNull(graphConfig);
        GraphConfig graphConfig2 = RecTemplate.toGraphConfig();
        Assert.assertTrue(graphConfig == graphConfig2);
    }
}