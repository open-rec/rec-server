package com.openrec.graph;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.Node;
import com.openrec.graph.node.NodeFactory;
import com.openrec.graph.node.NodeRegistry;
import com.openrec.graph.node.SimpleNodeFactory;

/** Explicit serving-node registry. Java class names remain aliases for existing graph JSON. */
@Configuration
public class ServingNodeRegistryConfig {

    @Bean
    public NodeRegistry servingNodeRegistry(AutowireCapableBeanFactory beans) {
        NodeRegistry.Builder registry = NodeRegistry.builder();
        register(registry, beans, "item.trigger", com.openrec.graph.node.item.TriggerNode.class,
            com.openrec.graph.node.item.TriggerNode::new);
        register(registry, beans, "item.user-feature", com.openrec.graph.node.item.UserFeatureNode.class,
            com.openrec.graph.node.item.UserFeatureNode::new);
        register(registry, beans, "item.new", com.openrec.graph.node.item.NewNode.class,
            com.openrec.graph.node.item.NewNode::new);
        register(registry, beans, "item.hot", com.openrec.graph.node.item.HotNode.class,
            com.openrec.graph.node.item.HotNode::new);
        register(registry, beans, "item.filter", com.openrec.graph.node.item.FilterNode.class,
            com.openrec.graph.node.item.FilterNode::new);
        register(registry, beans, "item.black", com.openrec.graph.node.item.BlackNode.class,
            com.openrec.graph.node.item.BlackNode::new);
        register(registry, beans, "item.i2i", com.openrec.graph.node.item.I2iNode.class,
            com.openrec.graph.node.item.I2iNode::new);
        register(registry, beans, "item.u2i", com.openrec.graph.node.item.U2iNode.class,
            com.openrec.graph.node.item.U2iNode::new);
        register(registry, beans, "item.embedding", com.openrec.graph.node.item.EmbeddingNode.class,
            com.openrec.graph.node.item.EmbeddingNode::new);
        register(registry, beans, "item.combine", com.openrec.graph.node.item.CombineNode.class,
            com.openrec.graph.node.item.CombineNode::new);
        register(registry, beans, "item.rank", com.openrec.graph.node.item.RankNode.class,
            com.openrec.graph.node.item.RankNode::new);
        register(registry, beans, "item.item-feature", com.openrec.graph.node.item.ItemFeatureNode.class,
            com.openrec.graph.node.item.ItemFeatureNode::new);
        register(registry, beans, "item.operation", com.openrec.graph.node.item.OperationNode.class,
            com.openrec.graph.node.item.OperationNode::new);
        register(registry, beans, "item.collector", com.openrec.graph.node.item.CollectorNode.class,
            com.openrec.graph.node.item.CollectorNode::new);
        register(registry, beans, "item.search", com.openrec.graph.node.item.SearchNode.class,
            com.openrec.graph.node.item.SearchNode::new);

        register(registry, beans, "user.trigger", com.openrec.graph.node.user.TriggerNode.class,
            com.openrec.graph.node.user.TriggerNode::new);
        register(registry, beans, "user.user-feature", com.openrec.graph.node.user.UserFeatureNode.class,
            com.openrec.graph.node.user.UserFeatureNode::new);
        register(registry, beans, "user.filter", com.openrec.graph.node.user.FilterNode.class,
            com.openrec.graph.node.user.FilterNode::new);
        register(registry, beans, "user.black", com.openrec.graph.node.user.BlackNode.class,
            com.openrec.graph.node.user.BlackNode::new);
        register(registry, beans, "user.u2u", com.openrec.graph.node.user.U2uNode.class,
            com.openrec.graph.node.user.U2uNode::new);
        register(registry, beans, "user.embedding", com.openrec.graph.node.user.EmbeddingNode.class,
            com.openrec.graph.node.user.EmbeddingNode::new);
        register(registry, beans, "user.combine", com.openrec.graph.node.user.CombineNode.class,
            com.openrec.graph.node.user.CombineNode::new);
        register(registry, beans, "user.rank", com.openrec.graph.node.user.RankNode.class,
            com.openrec.graph.node.user.RankNode::new);
        register(registry, beans, "user.operation", com.openrec.graph.node.user.OperationNode.class,
            com.openrec.graph.node.user.OperationNode::new);
        register(registry, beans, "user.collector", com.openrec.graph.node.user.CollectorNode.class,
            com.openrec.graph.node.user.CollectorNode::new);
        return registry.build();
    }

    private static void register(NodeRegistry.Builder registry, AutowireCapableBeanFactory beans, String type,
        Class<? extends Node> legacyClass, java.util.function.Function<NodeConfig, Node> creator) {
        java.util.function.Function<NodeConfig, Node> wired = config -> {
            Node node = creator.apply(config);
            beans.autowireBean(node);
            return node;
        };
        registry.register(new SimpleNodeFactory(type, wired));
        registry.register(new SimpleNodeFactory(legacyClass.getName(), wired));
    }
}
