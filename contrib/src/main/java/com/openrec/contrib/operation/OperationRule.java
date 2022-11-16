package com.openrec.contrib.operation;

import com.openrec.graph.GraphContext;
import com.openrec.proto.model.ScoreResult;
import org.pf4j.ExtensionPoint;

import java.util.List;

public interface OperationRule extends ExtensionPoint {

    List<ScoreResult> handle(GraphContext context, List<ScoreResult> inputItems);
}
