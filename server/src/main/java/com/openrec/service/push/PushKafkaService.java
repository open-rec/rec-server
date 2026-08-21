package com.openrec.service.push;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.UserReq;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.service.kafka.KafkaService;

@Service
@Profile("cluster")
public class PushKafkaService implements PushService {

    @Autowired
    private KafkaService kafkaService;

    @Override
    public void pushItem(ItemReq itemReq) {
        for (Item item : itemReq.getData()) {
            requireId("item", item == null ? null : item.getId());
            kafkaService.writeItem(itemReq.getCmd(), item);
        }
    }

    @Override
    public void pushUser(UserReq userReq) {
        for (User user : userReq.getData()) {
            requireId("user", user == null ? null : user.getId());
            kafkaService.writeUser(userReq.getCmd(), user);
        }
    }

    @Override
    public void pushEvent(EventReq eventReq) {
        if (eventReq.getCmd() == com.openrec.proto.biz.push.PushCmd.DELETE) {
            throw new IllegalArgumentException("event DELETE is not supported");
        }
        for (Event event : eventReq.getData()) {
            if ("dislike".equalsIgnoreCase(event.getType())) {
                DislikeRules.parse(event.getValue());
            }
            kafkaService.writeEvent(eventReq.getCmd(), event);
        }
    }

    private static void requireId(String type, String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(type + " id is required");
        }
    }
}
