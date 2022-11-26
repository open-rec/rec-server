package com.openrec.service.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.util.JsonUtil;

@Service
public class KafkaService {

    @Value("${spring.kafka.topic.item}")
    private String itemTopic;

    @Value("${spring.kafka.topic.user}")
    private String userTopic;

    @Value("${spring.kafka.topic.event}")
    private String eventTopic;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void writeItem(Item item) {
        kafkaTemplate.send(itemTopic, JsonUtil.objToJson(item));
    }

    public void writeUser(User user) {
        kafkaTemplate.send(userTopic, JsonUtil.objToJson(user));
    }

    public void writeEvent(Event event) {
        kafkaTemplate.send(eventTopic, JsonUtil.objToJson(event));
    }
}
