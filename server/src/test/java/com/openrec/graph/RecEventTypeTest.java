package com.openrec.graph;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class RecEventTypeTest {

    @Test
    public void testToString() {
        Assert.assertEquals("click", RecEventType.CLICK.toString());
    }
}