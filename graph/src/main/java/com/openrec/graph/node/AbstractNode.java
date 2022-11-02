package com.openrec.graph.node;

import com.google.common.collect.Lists;
import com.openrec.graph.config.NodeConfig;

import java.util.List;

public abstract class AbstractNode<C> implements Node {

    protected String id;
    protected String name;
    protected boolean open;

    protected NodeStatus status;

    protected List<Node> parents;
    protected List<Node> children;
    protected NodeConfig<C> config;

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

    public boolean isRunning() {
        return NodeStatus.RUNNING == status;
    }

    public boolean isReady() {
        return NodeStatus.INIT == status && parents.stream().allMatch(p -> p.finished());
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

    public void stop() {
        this.status = NodeStatus.STOP;
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

    @Override
    public List<Node> getChildren() {
        return children;
    }

    @Override
    public List<Node> getParents() {
        return parents;
    }

    @Override
    public void setConfig(NodeConfig config) {
        this.config = config;
    }

    public void destroy() {
        this.children.clear();
        this.parents.clear();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
