package com.openrec.graph;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.Node;
import com.openrec.graph.node.NodeFactory;
import com.openrec.graph.node.NodeRegistry;
import com.openrec.graph.node.NodeContract;
import com.openrec.graph.node.ReflectiveNodeFactory;
import com.openrec.graph.node.TypedNode;
import com.openrec.graph.data.DataKey;

/** Immutable, precompiled graph metadata shared by all executions of one deployment. */
public final class GraphPlan {

    private final GraphConfig config;
    private final List<CompiledNode> factories;
    private final int[][] edges;
    private final int[] roots;
    private final int[][] children;
    private final int[] indegree;

    private GraphPlan(GraphConfig config, List<CompiledNode> factories, int[][] edges, int[] roots, int[][] children,
        int[] indegree) {
        this.config = config;
        this.factories = factories;
        this.edges = edges;
        this.roots = roots;
        this.children = children;
        this.indegree = indegree;
    }

    public static GraphPlan compile(GraphConfig config) {
        return compile(config, NodeRegistry.builder().fallback(new ReflectiveNodeFactory()).build());
    }

    public static GraphPlan compile(GraphConfig config, NodeRegistry registry) {
        if (config == null || config.getNodes() == null || config.getNodes().isEmpty() || config.getEdges() == null) {
            throw new IllegalArgumentException("graph config is incomplete");
        }
        if (registry == null)
            throw new IllegalArgumentException("node registry is required");
        List<CompiledNode> factories = new ArrayList<>();
        Map<String, Integer> indexes = new HashMap<>();
        try {
            for (int index = 0; index < config.getNodes().size(); index++) {
                NodeConfig nodeConfig = config.getNodes().get(index);
                if (nodeConfig == null || isBlank(nodeConfig.getName())
                    || isBlank(nodeConfig.getType()) && isBlank(nodeConfig.getClazz())) {
                    throw new IllegalArgumentException("every graph node requires a name and type or legacy class");
                }
                if (nodeConfig.getTimeout() <= 0) {
                    throw new IllegalArgumentException("node timeout must be positive: " + nodeConfig.getName());
                }
                if (indexes.containsKey(nodeConfig.getName())) {
                    throw new IllegalArgumentException("duplicate node: " + nodeConfig.getName());
                }
                NodeFactory factory = registry.resolve(nodeConfig);
                Node probe = factory.create(nodeConfig);
                if (probe == null)
                    throw new IllegalArgumentException("node factory returned null: " + nodeConfig.getName());
                factories.add(new CompiledNode(nodeConfig, factory, probe, NodeContract.inspect(probe.getClass())));
                indexes.put(nodeConfig.getName(), index);
            }
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("cannot compile serving graph: " + error.getMessage(), error);
        }
        int[][] edges = new int[config.getEdges().size()][2];
        int[] indegree = new int[factories.size()];
        Set<String> uniqueEdges = new HashSet<>();
        for (int index = 0; index < config.getEdges().size(); index++) {
            GraphConfig.NodeEdge edge = config.getEdges().get(index);
            if (edge == null || isBlank(edge.getFrom()) || isBlank(edge.getTo())) {
                throw new IllegalArgumentException("every graph edge requires from and to");
            }
            Integer from = indexes.get(edge.getFrom());
            Integer to = indexes.get(edge.getTo());
            if (from == null || to == null)
                throw new IllegalArgumentException("edge references an unknown node");
            String edgeKey = edge.getFrom() + "->" + edge.getTo();
            if (from.equals(to) || !uniqueEdges.add(edgeKey)) {
                throw new IllegalArgumentException("invalid or duplicate edge: " + edgeKey);
            }
            edges[index][0] = from;
            edges[index][1] = to;
            indegree[to]++;
        }
        int rootCount = 0;
        for (int degree : indegree)
            if (degree == 0)
                rootCount++;
        int[] roots = new int[rootCount];
        for (int index = 0, root = 0; index < indegree.length; index++) {
            if (indegree[index] == 0)
                roots[root++] = index;
        }
        int[] childCounts = new int[factories.size()];
        for (int[] edge : edges)
            childCounts[edge[0]]++;
        int[][] children = new int[factories.size()][];
        for (int index = 0; index < children.length; index++)
            children[index] = new int[childCounts[index]];
        int[] childIndexes = new int[factories.size()];
        for (int[] edge : edges)
            children[edge[0]][childIndexes[edge[0]]++] = edge[1];
        validateAcyclic(children, indegree, roots);
        validateAnnotatedContracts(factories, edges);
        validateTypedContracts(factories, edges);
        return new GraphPlan(config, factories, edges, roots, children, indegree);
    }

    private static void validateAnnotatedContracts(List<CompiledNode> nodes, int[][] edges) {
        List<Set<Integer>> ancestors = ancestors(nodes.size(), edges);
        for (int consumerIndex = 0; consumerIndex < nodes.size(); consumerIndex++) {
            CompiledNode consumer = nodes.get(consumerIndex);
            for (NodeContract.Port input : consumer.contract.imports()) {
                List<Producer> producers = new ArrayList<>();
                for (int producerIndex : ancestors.get(consumerIndex)) {
                    CompiledNode producer = nodes.get(producerIndex);
                    for (NodeContract.Port output : producer.contract.exports()) {
                        if (input.name().equals(output.name()))
                            producers.add(new Producer(producer.config.getName(), output));
                    }
                }
                if (producers.isEmpty()) {
                    if (input.required())
                        throw new IllegalArgumentException("node " + consumer.config.getName() + " requires input "
                            + input.name() + " (" + input.type().getTypeName() + ") but no upstream node exports it");
                    continue;
                }
                for (Producer producer : producers) {
                    if (!isAssignable(input.type(), input.rawType(), producer.port.type(), producer.port.rawType()))
                        throw new IllegalArgumentException("node " + consumer.config.getName() + " imports "
                            + input.name() + " as " + input.type().getTypeName() + " but upstream node "
                            + producer.nodeName + " exports it as " + producer.port.type().getTypeName());
                }
            }
        }
    }

    private static List<Set<Integer>> ancestors(int nodeCount, int[][] edges) {
        List<Set<Integer>> result = new ArrayList<>();
        int[] remaining = new int[nodeCount];
        List<List<Integer>> parents = new ArrayList<>();
        List<List<Integer>> children = new ArrayList<>();
        for (int index = 0; index < nodeCount; index++) {
            result.add(new HashSet<>());
            parents.add(new ArrayList<>());
            children.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            remaining[edge[1]]++;
            parents.get(edge[1]).add(edge[0]);
            children.get(edge[0]).add(edge[1]);
        }
        Queue<Integer> ready = new ArrayDeque<>();
        for (int index = 0; index < nodeCount; index++)
            if (remaining[index] == 0)
                ready.add(index);
        while (!ready.isEmpty()) {
            int current = ready.remove();
            for (int parent : parents.get(current)) {
                result.get(current).add(parent);
                result.get(current).addAll(result.get(parent));
            }
            for (int child : children.get(current))
                if (--remaining[child] == 0)
                    ready.add(child);
        }
        return result;
    }

    private static boolean isAssignable(Type consumer, Class<?> consumerRaw, Type producer, Class<?> producerRaw) {
        if (!consumerRaw.isAssignableFrom(producerRaw))
            return false;
        if (consumer instanceof ParameterizedType && producer instanceof ParameterizedType)
            return consumer.equals(producer);
        return !(consumer instanceof ParameterizedType) && !(producer instanceof ParameterizedType);
    }

    private static void validateTypedContracts(List<CompiledNode> nodes, int[][] edges) {
        Map<String, DataKey<?>> producers = new HashMap<>();
        for (CompiledNode compiled : nodes) {
            if (!(compiled.probe instanceof TypedNode))
                continue;
            for (DataKey<?> output : ((TypedNode)compiled.probe).outputs()) {
                DataKey<?> previous = producers.put(output.getName(), output);
                if (previous != null)
                    throw new IllegalArgumentException("duplicate typed output: " + output.getName());
            }
        }
        List<Set<DataKey<?>>> available = new ArrayList<>();
        boolean[] hasLegacyAncestor = new boolean[nodes.size()];
        for (int index = 0; index < nodes.size(); index++)
            available.add(new HashSet<>());
        int[] remaining = new int[nodes.size()];
        List<List<Integer>> parents = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++)
            parents.add(new ArrayList<>());
        for (int[] edge : edges) {
            remaining[edge[1]]++;
            parents.get(edge[1]).add(edge[0]);
        }
        Queue<Integer> queue = new ArrayDeque<>();
        for (int index = 0; index < remaining.length; index++)
            if (remaining[index] == 0)
                queue.add(index);
        while (!queue.isEmpty()) {
            int current = queue.remove();
            for (int parent : parents.get(current)) {
                available.get(current).addAll(available.get(parent));
                if (nodes.get(parent).probe instanceof TypedNode)
                    available.get(current).addAll(((TypedNode)nodes.get(parent).probe).outputs());
                else
                    hasLegacyAncestor[current] = true;
                hasLegacyAncestor[current] |= hasLegacyAncestor[parent];
            }
            if (nodes.get(current).probe instanceof TypedNode && !hasLegacyAncestor[current]) {
                Set<DataKey<?>> missing = new HashSet<>(((TypedNode)nodes.get(current).probe).requiredInputs());
                missing.removeAll(available.get(current));
                if (!missing.isEmpty())
                    throw new IllegalArgumentException(
                        "typed node " + nodes.get(current).config.getName() + " has missing inputs: " + missing);
            }
            for (int[] edge : edges)
                if (edge[0] == current && --remaining[edge[1]] == 0)
                    queue.add(edge[1]);
        }
    }

    private static void validateAcyclic(int[][] children, int[] indegree, int[] roots) {
        int[] remaining = indegree.clone();
        Queue<Integer> ready = new ArrayDeque<>();
        for (int root : roots)
            ready.add(root);
        int visited = 0;
        while (!ready.isEmpty()) {
            int current = ready.remove();
            visited++;
            for (int child : children[current]) {
                if (--remaining[child] == 0)
                    ready.add(child);
            }
        }
        if (visited != children.length)
            throw new IllegalArgumentException("graph contains a cycle");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public GraphConfig getConfig() {
        return config;
    }

    int[][] getEdges() {
        return edges;
    }

    int[] getRoots() {
        return roots;
    }

    int[] getChildren(int index) {
        return children[index];
    }

    int[] newDependencyCounts() {
        return indegree.clone();
    }

    int size() {
        return factories.size();
    }

    NodeConfig getNodeConfig(int index) {
        return factories.get(index).config;
    }

    NodeContract getNodeContract(int index) {
        return factories.get(index).contract;
    }

    Node newNode(int index) {
        try {
            CompiledNode factory = factories.get(index);
            Node node = factory.factory.create(factory.config);
            if (node == null)
                throw new IllegalStateException("node factory returned null: " + factory.config.getName());
            node.setConfig(factory.config);
            return node;
        } catch (Exception error) {
            throw new IllegalStateException("cannot instantiate compiled graph node", error);
        }
    }

    private static final class CompiledNode {
        private final NodeConfig config;
        private final NodeFactory factory;
        private final Node probe;
        private final NodeContract contract;

        private CompiledNode(NodeConfig config, NodeFactory factory, Node probe, NodeContract contract) {
            this.config = config;
            this.factory = factory;
            this.probe = probe;
            this.contract = contract;
        }
    }

    private static final class Producer {
        private final String nodeName;
        private final NodeContract.Port port;

        private Producer(String nodeName, NodeContract.Port port) {
            this.nodeName = nodeName;
            this.port = port;
        }
    }
}
