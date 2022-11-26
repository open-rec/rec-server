package com.openrec.service.push;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.UserReq;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.service.kafka.KafkaService;

@Service
public class PushKafkaService implements PushService {

    @Autowired
    private KafkaService kafkaService;

    @Override
    public void pushItem(ItemReq itemReq) {
        for (Item item : itemReq.getData()) {
            kafkaService.writeItem(item);
        }
    }

    @Override
    public void pushUser(UserReq userReq) {
        for (User user : userReq.getData()) {
            kafkaService.writeUser(user);
        }
    }

    @Override
    public void pushEvent(EventReq eventReq) {
        for (Event event : eventReq.getData()) {
            kafkaService.writeEvent(event);
        }
    }
}
