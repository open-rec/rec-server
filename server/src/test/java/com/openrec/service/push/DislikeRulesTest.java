package com.openrec.service.push;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.junit.Test;

public class DislikeRulesTest {
    @Test
    public void parsesIdCategoryAndMultipleTags() {
        assertEquals(new LinkedHashSet<>(Arrays.asList("id:item-1", "category:sports", "tag:nba", "tag:cba")),
            DislikeRules.parse("{\"id\":\"item-1\",\"category\":\"sports\",\"tags\":[\"nba\",\"cba\"]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyRules() {
        DislikeRules.parse("{}");
    }
}
