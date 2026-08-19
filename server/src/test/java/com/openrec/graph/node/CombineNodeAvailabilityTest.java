package com.openrec.graph.node;

import org.junit.Assert;
import org.junit.Test;

import com.openrec.proto.model.Item;

public class CombineNodeAvailabilityTest {

    private Item item(String scene, int status, String expireTime) {
        Item item = new Item();
        item.setId("item-1");
        item.setScene(scene);
        item.setStatus(status);
        item.setExpireTime(expireTime);
        return item;
    }

    @Test
    public void keepsActiveItemWithoutExpiry() {
        Assert.assertTrue(CombineNode.isAvailable(item("scene-1", 1, "0"), "scene-1", 100, true));
    }

    @Test
    public void filtersDisabledExpiredAndCrossSceneItems() {
        Assert.assertFalse(CombineNode.isAvailable(item("scene-1", 0, "0"), "scene-1", 100, true));
        Assert.assertFalse(CombineNode.isAvailable(item("scene-1", 1, "99"), "scene-1", 100, true));
        Assert.assertFalse(CombineNode.isAvailable(item("scene-2", 1, "0"), "scene-1", 100, true));
    }

    @Test
    public void filtersMalformedExpiryConservatively() {
        Assert.assertFalse(CombineNode.isAvailable(item("scene-1", 1, "invalid"), "scene-1", 100, true));
    }

    @Test
    public void canIgnoreLegacyExpiryWhileStillCheckingStatusAndScene() {
        Assert.assertTrue(CombineNode.isAvailable(item("scene-1", 1, "99"), "scene-1", 100, false));
        Assert.assertFalse(CombineNode.isAvailable(item("scene-1", 0, "99"), "scene-1", 100, false));
    }
}
