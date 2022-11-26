package com.openrec.service.kafka;


import com.openrec.proto.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {

    @Value("${spring.kafka.topic.item}")
    private String itemTopic;

    @Value("${spring.kafka.topic.user}")
    private String userTopic;

    @Value("${spring.kafka.topic.event}")
    private String eventTopic;

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    public void writeItem(Item item) {
    }
}
