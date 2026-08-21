package com.openrec.service.push;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.PushCmd;
import com.openrec.proto.biz.push.UserReq;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.service.redis.RedisService;

@Service
public class PushRedisService implements PushService {

    private String USER_KEY = "user:{%s}";
    private String ITEM_KEY = "item:{%s}";
    private String EVENT_KEY = "event:{%s}:%s:%s";

    private String NEW_KEY = "new:{%s}";

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    private List<String> itemKeys(List<Item> items) {
        return items.stream().map(item -> String.format(ITEM_KEY, item.getId())).collect(Collectors.toList());
    }

    private List<Item> loadStoredItems(List<Item> items) {
        return redisService.getVs(itemKeys(items)).stream().filter(Objects::nonNull)
            .map(value -> objectMapper.convertValue(value, Item.class)).collect(Collectors.toList());
    }

    private void removeFromNewIndexes(List<Item> items) {
        Map<String, List<String>> itemsByScene = items.stream()
            .filter(item -> item != null && item.getScene() != null && item.getId() != null)
            .collect(Collectors.groupingBy(Item::getScene, Collectors.mapping(Item::getId, Collectors.toList())));
        for (Map.Entry<String, List<String>> entry : itemsByScene.entrySet()) {
            redisService.removeZSetValues(String.format(NEW_KEY, entry.getKey()), entry.getValue());
        }
    }

    @Override
    public void pushItem(ItemReq itemReq) {
        List<Item> items = itemReq.getData();
        List<Item> storedItems = loadStoredItems(items);
        if (itemReq.getCmd() == PushCmd.INSERT || itemReq.getCmd() == PushCmd.UPDATE) {
            // Remove the previous membership first. This also handles an item moving to another
            // scene or changing its publication time without leaving a stale entry behind.
            removeFromNewIndexes(storedItems);
            redisService.addKvs(
                items.stream().collect(Collectors.toMap(item -> String.format(ITEM_KEY, item.getId()), item -> item)));

            Map<String, Map<String, Double>> newItemMap =
                items.stream().collect(Collectors.groupingBy(item -> String.format(NEW_KEY, item.getScene()),
                    Collectors.toMap(item -> item.getId(), item -> Double.valueOf(item.getPubTime()))));
            for (Map.Entry<String, Map<String, Double>> itemEventEntry : newItemMap.entrySet()) {
                redisService.addZSets(itemEventEntry.getKey(), itemEventEntry.getValue());
            }
        } else {
            removeFromNewIndexes(storedItems);
            // Fall back to the scene carried by the delete request when the entity is already gone.
            removeFromNewIndexes(items);
            redisService.removeKs(itemKeys(items));
        }
    }

    @Override
    public void pushUser(UserReq userReq) {
        List<User> users = userReq.getData();
        if (userReq.getCmd() == PushCmd.INSERT || userReq.getCmd() == PushCmd.UPDATE) {
            redisService.addKvs(
                users.stream().collect(Collectors.toMap(user -> String.format(USER_KEY, user.getId()), user -> user)));
        } else {
            redisService.removeKs(
                users.stream().map(user -> String.format(USER_KEY, user.getId())).collect(Collectors.toList()));
        }
    }

    @Override
    public void pushEvent(EventReq eventReq) {
        List<Event> events = eventReq.getData();
        if (eventReq.getCmd() == PushCmd.INSERT || eventReq.getCmd() == PushCmd.UPDATE) {
            for (Event event : events) {
                if ("dislike".equalsIgnoreCase(event.getType())) {
                    String key = String.format(EVENT_KEY, event.getUserId(), event.getScene(), event.getType());
                    double time = Double.valueOf(event.getTime());
                    for (String rule : DislikeRules.parse(event.getValue())) {
                        redisService.addZSet(key, rule, time);
                    }
                }
            }
            Map<String,
                Map<String, Double>> userEvents = events.stream()
                    .filter(event -> !"dislike".equalsIgnoreCase(event.getType()))
                    .collect(Collectors.groupingBy(
                        event -> String.format(EVENT_KEY, event.getUserId(), event.getScene(), event.getType()),
                        Collectors.toMap(event -> event.getItemId(), event -> Double.valueOf(event.getTime()))));
            for (Map.Entry<String, Map<String, Double>> userEventEntry : userEvents.entrySet()) {
                redisService.addZSets(userEventEntry.getKey(), userEventEntry.getValue());
            }
        } else {
            Map<String, List<String>> eventItems = events.stream().collect(Collectors.groupingBy(
                event -> String.format(EVENT_KEY, event.getUserId(), event.getScene(), event.getType()),
                Collectors.mapping(Event::getItemId, Collectors.toList())));
            for (Map.Entry<String, List<String>> entry : eventItems.entrySet()) {
                redisService.removeZSetValues(entry.getKey(), entry.getValue());
            }
        }
    }
}
