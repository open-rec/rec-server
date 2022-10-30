package com.openrec.graph.node;

import com.google.common.collect.Lists;

import java.util.List;

public abstract class AbstractNode implements Node {

    protected String id;
    protected String name;

    protected NodeStatus status;

    protected List<Node> parents;
    protected List<Node> children;

    public AbstractNode() {
        this.init();
    }

    public void init() {
        this.status = NodeStatus.INIT;
        this.id = this.getClass().getSimpleName();
        this.name = id;
        this.parents = Lists.newArrayList();
        this.children = Lists.newArrayList();
    }

    public boolean finished() {
        return NodeStatus.STOP == status;
    }

    public boolean isReady() {
        return parents.stream().allMatch(p -> p.finished());
    }

    public void addChild(Node child) {
        children.add(child);
    }

    public void addParent(Node parent) {
        parents.add(parent);
    }

    public void start() {
        this.status = NodeStatus.RUNNING;
    }

    @Override
    public NodeStatus getStatus() {
        return status;
    }

    @Override
    public String getId() {
        return id;
    }

    public void destroy() {
        this.status = NodeStatus.STOP;
        this.children.clear();
        this.parents.clear();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
