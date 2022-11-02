package com.openrec.graph.node;

import com.openrec.RecServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RecServer.class)
public class FilterNodeTest {



    @Test
    public void run() {
        FilterNode filterNode = new FilterNode();
        filterNode.run(null);
    }
}