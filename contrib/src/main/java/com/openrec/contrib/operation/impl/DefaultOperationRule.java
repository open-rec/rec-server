package com.openrec.contrib.operation.impl;

import java.util.List;

import org.pf4j.Extension;

import com.openrec.contrib.operation.OperationRule;
import com.openrec.graph.GraphContext;
import com.openrec.proto.model.ScoreResult;

/**
 * do nothing, just return.
 */
@Extension
public class DefaultOperationRule implements OperationRule {

    @Override
    public List<ScoreResult> handle(GraphContext context, List<ScoreResult> inputItems) {
        return inputItems;
    }
}
