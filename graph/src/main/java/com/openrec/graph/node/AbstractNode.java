package com.openrec.graph.node;

import com.google.common.collect.Lists;
import com.openrec.graph.config.NodeConfig;

import java.util.List;

public abstract class AbstractNode implements Node {

    protected String id;
    protected String name;
    protected boolean open;

    protected NodeStatus status;

    protected List<Node> parents;
    protected List<Node> children;
    protected NodeConfig config;

    public AbstractNode() {
        this(null);
    }

    public AbstractNode(NodeConfig nodeConfig) {
        this.id = this.getClass().getSimpleName();
        this.parents = Lists.newArrayList();
        this.children = Lists.newArrayList();
        this.config = nodeConfig;
        this.init();
    }

    public void init() {
        this.status = NodeStatus.INIT;
        if(config!=null) {
            this.name = config.getName();
            this.open = config.isOpen();
        }
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

    @Override
    public String getName() {
        return name;
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
