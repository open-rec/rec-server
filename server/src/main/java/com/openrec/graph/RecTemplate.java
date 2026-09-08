package com.openrec.graph;

import org.apache.commons.lang3.StringUtils;

import com.openrec.graph.config.NodeConfig;
import com.openrec.util.FileUtil;
import com.openrec.util.JsonUtil;

public class RecTemplate {

    public static GraphConfig toGraphConfig(String templateFileName) {
        return parse(FileUtil.read(templateFileName));
    }

    public static GraphConfig parse(String graphJson) {
        GraphConfig parsed = JsonUtil.jsonToObj(graphJson, GraphConfig.class);
        if (parsed == null || parsed.getNodes() == null) {
            return parsed;
        }
        for (NodeConfig nodeConfig : parsed.getNodes()) {
            String content = JsonUtil.objToJson(nodeConfig.getContent());
            String configClazz = nodeConfig.getConfigClazz();
            if (StringUtils.isNotEmpty(configClazz)) {
                try {
                    nodeConfig.setContent(JsonUtil.jsonToObj(content, Class.forName(configClazz)));
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("unknown node config class: " + configClazz, e);
                }
            }
        }
        return parsed;
    }

    public static GraphConfig toGraphConfig() {
        return toGraphConfig("item_graph.json");
    }
}
