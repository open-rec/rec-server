package com.openrec.graph.node;

import java.util.Collections;
import java.util.Set;

import com.openrec.graph.data.DataKey;
import com.openrec.graph.data.NodeInput;
import com.openrec.graph.data.NodeOutput;

/** Node contract with explicit immutable inputs and outputs. */
public interface TypedNode extends Node {
    default Set<DataKey<?>> requiredInputs() {
        return Collections.emptySet();
    }

    default Set<DataKey<?>> outputs() {
        return Collections.emptySet();
    }

    NodeOutput execute(NodeInput input);
}
