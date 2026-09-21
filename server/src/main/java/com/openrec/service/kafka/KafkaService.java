package com.openrec.service.kafka;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    @Value("${push.kafka.ack-timeout-ms:10000}")
    private long ackTimeoutMs = 10000L;

    public void writeItem(PushCmd operation, Item item) {
        send(itemTopic, item.getId(), EntityMutation.of("item", operation, item));
    }

    public void writeUser(PushCmd operation, User user) {
        send(userTopic, user.getId(), EntityMutation.of("user", operation, user));
    }

    public void writeEvent(PushCmd operation, Event event) {
        send(eventTopic, event.getUserId(), EntityMutation.of("event", operation, event));
    }

    private void send(String topic, String key, EntityMutation<?> mutation) {
        try {
            kafkaTemplate.send(topic, key, JsonUtil.objToJson(mutation)).get(ackTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka acknowledgement interrupted for " + topic, error);
        } catch (ExecutionException error) {
            throw new IllegalStateException("Kafka delivery failed for " + topic, error.getCause());
        } catch (TimeoutException error) {
            // A timeout is an unknown delivery outcome; it must never be reported as success.
            throw new IllegalStateException("Kafka acknowledgement timed out for " + topic, error);
        }
    }
}
