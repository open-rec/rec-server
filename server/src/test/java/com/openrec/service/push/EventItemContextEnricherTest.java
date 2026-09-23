package com.openrec.service.push;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.service.query.QueryService;

public class EventItemContextEnricherTest {
    @Test
    public void freezesItemContextAndPreservesExistingEventFields() {
        QueryService query = mock(QueryService.class);
        Item item = new Item();
        item.setId("i");
        item.setCategory("books");
        item.setSubcategory("fiction");
        Map<String, Object> itemExt = new LinkedHashMap<>();
        itemExt.put("unitPrice", "19.5");
        item.setExtFields(itemExt);
        when(query.queryItem("i")).thenReturn(item);
        EventItemContextEnricher enricher = new EventItemContextEnricher();
        ReflectionTestUtils.setField(enricher, "queryService", query);
        ReflectionTestUtils.setField(enricher, "objectMapper", new ObjectMapper());
        Event event = new Event();
        event.setItemId("i");
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("source", "search");
        event.setExtFields(ext);
        enricher.enrich(event);
        Map<String, Object> actual = (Map<String, Object>)event.getExtFields();
        assertEquals("search", actual.get("source"));
        Map<String, Object> context = (Map<String, Object>)actual.get(EventItemContextEnricher.FIELD);
        assertEquals("books", context.get("category"));
        assertEquals("fiction", context.get("subcategory"));
        assertEquals(19.5d, (Double)context.get("price"), 0d);
    }
}
