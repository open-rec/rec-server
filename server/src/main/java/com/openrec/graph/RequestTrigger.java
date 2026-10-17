package com.openrec.graph;

import java.util.Collections;
import java.util.List;

/** Immutable request seeds shared by parallel recall and combine nodes. */
public record RequestTrigger(List<TriggerItem> triggerItems, List<TriggerUser> triggerUsers, String triggerSelf,
    String triggerQuery) {
    public static final String KEY = "requestTrigger";

    public record TriggerItem(String id, double score) {
    }

    public record TriggerUser(String id, double score) {
    }

    public RequestTrigger {
        triggerItems = triggerItems == null ? Collections.emptyList() : List.copyOf(triggerItems);
        triggerUsers = triggerUsers == null ? Collections.emptyList() : List.copyOf(triggerUsers);
        triggerSelf = nonBlank(triggerSelf);
        triggerQuery = nonBlank(triggerQuery);
    }

    public static RequestTrigger fromParams(GraphContext context) {
        return new RequestTrigger(Collections.emptyList(), Collections.emptyList(),
            context.getParams().getValueToString(RecParams.USER_ID),
            context.getParams().getValueToString(RecParams.QUERY));
    }

    public RequestTrigger withItems(List<TriggerItem> items) {
        return new RequestTrigger(items, triggerUsers, triggerSelf, triggerQuery);
    }

    public RequestTrigger withUsers(List<TriggerUser> users) {
        return new RequestTrigger(triggerItems, users, triggerSelf, triggerQuery);
    }

    /** Clicked users are the seeds; self is only a cold-start fallback. */
    public List<String> userSeedIds() {
        List<String> ids = triggerUsers.stream().map(TriggerUser::id)
            .filter(id -> id != null && !id.isBlank() && !id.equals(triggerSelf)).distinct().toList();
        return ids.isEmpty() && !triggerSelf.isEmpty() ? List.of(triggerSelf) : ids;
    }

    /** Older custom graphs without a trigger node may still consume request identity/text. */
    public static RequestTrigger resolve(GraphContext context) {
        Object trigger = context.getData(KEY);
        return trigger instanceof RequestTrigger ? (RequestTrigger)trigger : fromParams(context);
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
