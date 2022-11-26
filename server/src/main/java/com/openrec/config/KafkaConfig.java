package com.openrec.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

    private final KafkaProperties properties;

    public KafkaConfig(KafkaProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(KafkaTemplate.class)
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory producerFactory) {
        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        return kafkaTemplate;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        KafkaAdmin kafkaAdmin = new KafkaAdmin(properties.buildAdminProperties());
        return kafkaAdmin;
    }

    @Bean
    @ConditionalOnMissingBean(ProducerFactory.class)
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties());
    }

    @Bean
    @ConditionalOnMissingBean(ConsumerFactory.class)
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties());
    }
}
