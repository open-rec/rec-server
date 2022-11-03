package com.openrec.graph;

import com.google.gson.GsonBuilder;
import com.openrec.graph.config.NodeConfig;
import com.openrec.util.FileUtil;
import com.openrec.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;

public class RecTemplate {

    private static final String TEMPLATE_FILE_NAME = "graph.json";
    private static String template;
    private static int version;
    private static boolean needUpdate;
    private static GraphConfig graphConfig;

    static {
        load();
    }

    private static void load() {
        template = FileUtil.read(TEMPLATE_FILE_NAME);
        int newVersion = template.hashCode();
        if (newVersion != version) {
            version = newVersion;
            needUpdate = true;
        }
    }

    public static GraphConfig toGraphConfig() {
        load();
        if (needUpdate) {
            graphConfig = JsonUtil.jsonToObj(template, GraphConfig.class);
            for(NodeConfig nodeConfig: graphConfig.getNodes()) {
                String content = JsonUtil.objToJson(nodeConfig.getContent());
                String configClazz = nodeConfig.getConfigClazz();
                if(StringUtils.isNotEmpty(configClazz)) {
                    try {
                        nodeConfig.setContent(JsonUtil.jsonToObj(content, Class.forName(configClazz)));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            needUpdate = false;
        }
        return graphConfig;
    }

    public static String getTemplate() {
        return template;
    }
}
