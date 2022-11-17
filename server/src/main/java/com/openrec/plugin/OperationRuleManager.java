package com.openrec.plugin;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;

import com.openrec.contrib.operation.OperationRule;

@Slf4j
public class OperationRuleManager {

    private static final String JAR_FILE =
        System.getProperty("user.dir") + File.separator + "plugins/rec-contrib-1.0-SNAPSHOT.jar";
    private static final String PLUGIN_ID = "contrib-plugins";

    private static PluginManager pluginManager;
    private static Map<String, OperationRule> operationRuleMap;

    static {
        pluginManager = new JarPluginManager();
        operationRuleMap = Maps.newHashMap();
        try {
            load(JAR_FILE, PLUGIN_ID);
        } catch (Exception e) {
            log.error("load jar file:{} failed, plugin:{}", JAR_FILE, PLUGIN_ID);
        }
    }

    public static void load(String jarFile, String pluginId) throws Exception{
        pluginManager.loadPlugin(Paths.get(jarFile));
        pluginManager.startPlugin(pluginId);
        operationRuleMap = pluginManager.getExtensions(OperationRule.class).stream()
            .collect(Collectors.toMap(i -> i.getClass().getSimpleName(), i -> i));
    }

    public static OperationRule getOperationRuleByName(String name) {
        return operationRuleMap.get(name);
    }

    public static void destroy() {
        pluginManager.stopPlugin(PLUGIN_ID);
        pluginManager.unloadPlugin(PLUGIN_ID);
        operationRuleMap.clear();
    }
}
