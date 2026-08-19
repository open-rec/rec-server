package com.openrec.util;

import com.openrec.config.AppConfig;
import com.openrec.config.KafkaConfig;
import com.openrec.config.RestConfig;
import org.junit.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.ApplicationContext;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UtilityAndConfigTest {
    @Test public void jsonAndTimeUtilitiesWork() {
        assertEquals("x", JsonUtil.jsonToObj(JsonUtil.objToJson("x"), String.class));
        long before = System.currentTimeMillis(); assertTrue(TimeUtil.now() >= before);
        assertTrue(Math.abs(TimeUtil.nowSecs() - System.currentTimeMillis() / 1000) <= 1);
        assertTrue(FileUtil.read("graph.json").contains("nodes"));
    }

    @Test public void beanUtilityHandlesMissingAndPresentContext() {
        new BeanUtil().setApplicationContext(null);
        assertNull(BeanUtil.getBean(String.class));
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(String.class)).thenReturn("bean");
        new BeanUtil().setApplicationContext(context);
        assertEquals("bean", BeanUtil.getBean(String.class));
    }

    @Test public void lightweightConfigurationsCreateBeans() {
        assertNotNull(AppConfig.getPropertyPlaceholderConfigurer());
        assertNotNull(new RestConfig().restTemplate());
        KafkaConfig kafka = new KafkaConfig(new KafkaProperties());
        assertNotNull(kafka.producerFactory());
        assertNotNull(kafka.consumerFactory());
        assertNotNull(kafka.kafkaAdmin());
        assertNotNull(kafka.kafkaTemplate(kafka.producerFactory()));
    }
}
