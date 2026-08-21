package com.openrec.service.push;

import java.util.LinkedHashSet;
import java.util.Set;

import com.openrec.proto.model.DislikeValue;
import com.openrec.util.JsonUtil;

/** Converts a dislike event value into compact Redis rule members. */
public final class DislikeRules {
    public static final String ID_PREFIX = "id:";
    public static final String CATEGORY_PREFIX = "category:";
    public static final String TAG_PREFIX = "tag:";

    private DislikeRules() {}

    public static Set<String> parse(String value) {
        try {
            DislikeValue dislike = JsonUtil.jsonToObj(value, DislikeValue.class);
            Set<String> rules = new LinkedHashSet<>();
            add(rules, ID_PREFIX, dislike == null ? null : dislike.getId());
            add(rules, CATEGORY_PREFIX, dislike == null ? null : dislike.getCategory());
            if (dislike != null && dislike.getTags() != null) {
                for (String tag : dislike.getTags()) { add(rules, TAG_PREFIX, tag); }
            }
            if (rules.isEmpty()) { throw new IllegalArgumentException("dislike value has no id, category or tags"); }
            return rules;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                "dislike value must be JSON: {\"id\":\"...\",\"category\":\"...\",\"tags\":[\"...\"]}", e);
        }
    }

    private static void add(Set<String> rules, String prefix, String value) {
        if (value != null && !value.trim().isEmpty()) { rules.add(prefix + value.trim()); }
    }
}
