package com.openrec.graph;

import com.google.gson.GsonBuilder;
import com.openrec.util.FileUtil;

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
            graphConfig = new GsonBuilder().setLenient().create().fromJson(template, GraphConfig.class);
            needUpdate = false;
        }
        return graphConfig;
    }

    public static String getTemplate() {
        return template;
    }
}
