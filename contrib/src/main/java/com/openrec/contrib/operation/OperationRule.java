package com.openrec.contrib.operation;

import java.util.List;

import org.pf4j.ExtensionPoint;

import com.openrec.graph.GraphContext;
import com.openrec.proto.model.ScoreResult;

public interface OperationRule extends ExtensionPoint {

    List<ScoreResult> handle(GraphContext context, List<ScoreResult> inputItems);
}
