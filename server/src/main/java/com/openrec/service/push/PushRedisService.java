package com.openrec.service.push;


import com.openrec.proto.biz.push.*;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.service.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PushRedisService implements PushService{

    @Autowired
    private RedisService redisService;


    @Override
    public void pushItem(ItemReq itemReq) {
        List<Item> items = itemReq.getData();
        if(itemReq.getCmd() == PushCmd.INSERT || itemReq.getCmd() == PushCmd.UPDATE) {
            redisService.addKvs(items.stream().collect(Collectors.toMap(Item::getId, item->item)));
        } else {
            redisService.removeKs(items.stream().map(item->item.getId()).collect(Collectors.toList()));
        }
    }

    @Override
    public void pushUser(UserReq userReq) {
        List<User> users = userReq.getData();
        if(userReq.getCmd() == PushCmd.INSERT || userReq.getCmd() == PushCmd.UPDATE) {
            redisService.addKvs(users.stream().collect(Collectors.toMap(User::getId, user->user)));
        } else {
            redisService.removeKs(users.stream().map(user->user.getId()).collect(Collectors.toList()));
        }
    }

    @Override
    public void pushEvent(EventReq eventReq) {
        List<Event> events = eventReq.getData();
        if(eventReq.getCmd() == PushCmd.INSERT || eventReq.getCmd() == PushCmd.UPDATE) {
            redisService.addKvs(events.stream().collect(Collectors.toMap(Event::getUserId, event->event)));
        }
    }
}
