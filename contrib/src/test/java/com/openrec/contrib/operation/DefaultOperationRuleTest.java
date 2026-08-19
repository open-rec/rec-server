package com.openrec.contrib.operation;

import com.openrec.contrib.operation.impl.DefaultOperationRule;
import com.openrec.graph.GraphContext;
import com.openrec.proto.model.ScoreResult;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertSame;

public class DefaultOperationRuleTest {
    @Test
    public void returnsInputUnchanged() {
        List<ScoreResult> input = Collections.singletonList(new ScoreResult("item", 1d));
        assertSame(input, new DefaultOperationRule().handle(new GraphContext(), input));
    }
}
