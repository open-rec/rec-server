package com.openrec.graph;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.Node;

/** Immutable, precompiled graph metadata shared by all executions of one deployment. */
public final class GraphPlan {

    private final GraphConfig config;
    private final List<NodeFactory> factories;
    private final int[][] edges;
    private final int[] roots;
    private final int[][] children;
    private final int[] indegree;

    private GraphPlan(GraphConfig config, List<NodeFactory> factories, int[][] edges, int[] roots,
        int[][] children, int[] indegree) {
        this.config = config;
        this.factories = factories;
        this.edges = edges;
        this.roots = roots;
        this.children = children;
        this.indegree = indegree;
    }

    public static GraphPlan compile(GraphConfig config) {
        if (config == null || config.getNodes() == null || config.getEdges() == null) {
            throw new IllegalArgumentException("graph config is incomplete");
        }
        List<NodeFactory> factories = new ArrayList<>();
        Map<String, Integer> indexes = new HashMap<>();
        try {
            for (int index = 0; index < config.getNodes().size(); index++) {
                NodeConfig nodeConfig = config.getNodes().get(index);
                Class<?> nodeClass = Class.forName(nodeConfig.getClazz());
                if (!Node.class.isAssignableFrom(nodeClass)) {
                    throw new IllegalArgumentException("node class does not implement Node: " + nodeConfig.getClazz());
                }
                @SuppressWarnings("unchecked")
                Constructor<? extends Node> constructor = (Constructor<? extends Node>) nodeClass
                    .getDeclaredConstructor(NodeConfig.class);
                constructor.setAccessible(true);
                factories.add(new NodeFactory(nodeConfig, constructor));
                indexes.put(nodeConfig.getName(), index);
            }
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("cannot compile serving graph: " + error.getMessage(), error);
        }
        int[][] edges = new int[config.getEdges().size()][2];
        int[] indegree = new int[factories.size()];
        for (int index = 0; index < config.getEdges().size(); index++) {
            GraphConfig.NodeEdge edge = config.getEdges().get(index);
            Integer from = indexes.get(edge.getFrom());
            Integer to = indexes.get(edge.getTo());
            if (from == null || to == null) throw new IllegalArgumentException("edge references an unknown node");
            edges[index][0] = from;
            edges[index][1] = to;
            indegree[to]++;
        }
        int rootCount = 0;
        for (int degree : indegree) if (degree == 0) rootCount++;
        int[] roots = new int[rootCount];
        for (int index = 0, root = 0; index < indegree.length; index++) {
            if (indegree[index] == 0) roots[root++] = index;
        }
        int[] childCounts = new int[factories.size()];
        for (int[] edge : edges) childCounts[edge[0]]++;
        int[][] children = new int[factories.size()][];
        for (int index = 0; index < children.length; index++) children[index] = new int[childCounts[index]];
        int[] childIndexes = new int[factories.size()];
        for (int[] edge : edges) children[edge[0]][childIndexes[edge[0]]++] = edge[1];
        return new GraphPlan(config, factories, edges, roots, children, indegree);
    }

    public GraphConfig getConfig() { return config; }
    int[][] getEdges() { return edges; }
    int[] getRoots() { return roots; }
    int[] getChildren(int index) { return children[index]; }
    int[] newDependencyCounts() { return indegree.clone(); }
    int size() { return factories.size(); }
    NodeConfig getNodeConfig(int index) { return factories.get(index).config; }

    Node newNode(int index) {
        try {
            NodeFactory factory = factories.get(index);
            Node node = factory.constructor.newInstance(factory.config);
            node.setConfig(factory.config);
            return node;
        } catch (Exception error) {
            throw new IllegalStateException("cannot instantiate compiled graph node", error);
        }
    }

    private static final class NodeFactory {
        private final NodeConfig config;
        private final Constructor<? extends Node> constructor;
        private NodeFactory(NodeConfig config, Constructor<? extends Node> constructor) {
            this.config = config;
            this.constructor = constructor;
        }
    }
}
