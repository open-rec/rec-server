package com.openrec.util;

import com.openrec.config.AppConfig;
import com.openrec.config.KafkaConfig;
import com.openrec.config.RestConfig;
import org.junit.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

import java.util.Collections;

import static org.junit.Assert.*;

public class UtilityAndConfigTest {
    @Test
    public void jsonAndTimeUtilitiesWork() {
        assertEquals("x", JsonUtil.jsonToObj(JsonUtil.objToJson("x"), String.class));
        long before = System.currentTimeMillis();
        assertTrue(TimeUtil.now() >= before);
        assertTrue(Math.abs(TimeUtil.nowSecs() - System.currentTimeMillis() / 1000) <= 1);
        assertTrue(FileUtil.read("item_graph.json").contains("nodes"));
    }

    @Test
    public void lightweightConfigurationsCreateBeans() {
        assertNotNull(AppConfig.getPropertyPlaceholderConfigurer());
        assertNotNull(new RestConfig().restTemplate());
        KafkaConfig kafka = new KafkaConfig(new KafkaProperties());
        assertNotNull(kafka.producerFactory());
        assertNotNull(kafka.consumerFactory());
        assertNotNull(kafka.kafkaAdmin());
        assertNotNull(kafka.kafkaTemplate(kafka.producerFactory()));
    }
}
