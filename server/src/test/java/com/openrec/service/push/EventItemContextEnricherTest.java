package com.openrec.service.push;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;
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

    @Test
    public void acceptsJsonStringsAndIgnoresNonObjectExtensions() {
        for (Object value : new Object[] {null, "", "null", "[]", "true", "broken", 12, java.util.List.of("a"),
            "{\"price\":\"NaN\"}"}) {
            assertContext(value, value, null, null);
        }
        assertContext("{\"unit_price\":19.5}", "{\"source\":\"search\"}", 19.5d, "search");
        assertContext("{\"price\":-1,\"unitPrice\":2}", null, 2d, null);
    }

    private void assertContext(Object itemFields, Object eventFields, Double price, String source) {
        QueryService query = mock(QueryService.class);
        Item item = new Item();
        item.setCategory("books");
        item.setExtFields(itemFields);
        when(query.queryItem("i")).thenReturn(item);
        EventItemContextEnricher enricher = new EventItemContextEnricher();
        ReflectionTestUtils.setField(enricher, "queryService", query);
        ReflectionTestUtils.setField(enricher, "objectMapper", new ObjectMapper());
        Event event = new Event();
        event.setItemId("i");
        event.setExtFields(eventFields);
        enricher.enrich(event);
        Map<?, ?> ext = (Map<?, ?>)event.getExtFields();
        assertEquals(source, ext.get("source"));
        Map<?, ?> context = (Map<?, ?>)ext.get(EventItemContextEnricher.FIELD);
        assertEquals("books", context.get("category"));
        assertEquals(price, context.get("price"));
    }
}
