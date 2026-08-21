package com.openrec.service.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.proto.biz.push.EntityMutation;
import com.openrec.proto.biz.push.PushCmd;
import com.openrec.util.JsonUtil;

@Service
@Profile("cluster")
public class KafkaService {

    @Value("${spring.kafka.topic.item}")
    private String itemTopic;

    @Value("${spring.kafka.topic.user}")
    private String userTopic;

    @Value("${spring.kafka.topic.event}")
    private String eventTopic;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void writeItem(PushCmd operation, Item item) {
        kafkaTemplate.send(itemTopic, item.getId(),
            JsonUtil.objToJson(EntityMutation.of("item", operation, item)));
    }

    public void writeUser(PushCmd operation, User user) {
        kafkaTemplate.send(userTopic, user.getId(),
            JsonUtil.objToJson(EntityMutation.of("user", operation, user)));
    }

    public void writeEvent(PushCmd operation, Event event) {
        kafkaTemplate.send(eventTopic, event.getUserId(),
            JsonUtil.objToJson(EntityMutation.of("event", operation, event)));
    }
}
