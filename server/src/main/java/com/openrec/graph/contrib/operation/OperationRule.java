package com.openrec.graph.contrib.operation;

import com.openrec.graph.GraphContext;
import com.openrec.proto.model.ScoreResult;

import java.util.List;

public interface OperationRule {

    List<ScoreResult> handle(GraphContext context, List<ScoreResult> inputItems);
}
