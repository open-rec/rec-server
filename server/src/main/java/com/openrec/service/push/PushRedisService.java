package com.openrec.service.push;


import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.PushCmd;
import com.openrec.proto.biz.push.UserReq;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.service.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PushRedisService implements PushService {

    private String USER_KEY = "user:{%s}";
    private String ITEM_KEY = "item:{%s}";
    private String EVENT_KEY = "event:%s:%s:{}";

    private String NEW_KEY = "new:{%s}";

    @Autowired
    private RedisService redisService;


    @Override
    public void pushItem(ItemReq itemReq) {
        List<Item> items = itemReq.getData();
        if (itemReq.getCmd() == PushCmd.INSERT || itemReq.getCmd() == PushCmd.UPDATE) {
            redisService.addKvs(items.stream()
                    .collect(Collectors.toMap(item -> String.format(ITEM_KEY, item.getId()), item -> item)));

            Map<String, Map<String, Double>> newItemMap = items.stream().collect(
                    Collectors.groupingBy(
                            item -> String.format(NEW_KEY, item.getScene()),
                            Collectors.toMap(
                                    item -> item.getId(),
                                    item -> Double.valueOf(item.getPubTime())
                            )
                    )
            );
            for (Map.Entry<String, Map<String, Double>> itemEventEntry : newItemMap.entrySet()) {
                redisService.addZSets(itemEventEntry.getKey(), itemEventEntry.getValue());
            }
        } else {
            redisService.removeKs(items.stream().map(item -> String.format(ITEM_KEY, item.getId()))
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public void pushUser(UserReq userReq) {
        List<User> users = userReq.getData();
        if (userReq.getCmd() == PushCmd.INSERT || userReq.getCmd() == PushCmd.UPDATE) {
            redisService.addKvs(users.stream()
                    .collect(Collectors.toMap(user -> String.format(USER_KEY, user.getId()), user -> user)));
        } else {
            redisService.removeKs(users.stream().map(user -> String.format(USER_KEY, user.getId()))
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public void pushEvent(EventReq eventReq) {
        List<Event> events = eventReq.getData();
        if (eventReq.getCmd() == PushCmd.INSERT || eventReq.getCmd() == PushCmd.UPDATE) {
            Map<String, Map<String, Double>> userEvents = events.stream().collect(
                    Collectors.groupingBy(
                            event -> String.format(EVENT_KEY, event.getScene(), event.getType(), event.getUserId()),
                            Collectors.toMap(
                                    event -> event.getItemId(),
                                    event -> Double.valueOf(event.getTime())
                            )
                    )
            );
            for (Map.Entry<String, Map<String, Double>> userEventEntry : userEvents.entrySet()) {
                redisService.addZSets(userEventEntry.getKey(), userEventEntry.getValue());
            }
        }
    }
}
