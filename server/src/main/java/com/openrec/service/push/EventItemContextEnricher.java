package com.openrec.service.push;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.service.query.QueryService;

/** Freezes Item attributes inside an event before it enters Kafka history. */
@Component
public class EventItemContextEnricher {
    public static final String FIELD = "_openrecItemContext";

    @Autowired
    private QueryService queryService;
    @Autowired
    private ObjectMapper objectMapper;

    public void enrich(Event event) {
        if (event == null || event.getItemId() == null) {
            return;
        }
        Item item = queryService.queryItem(event.getItemId());
        if (item == null) {
            return;
        }
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("category", item.getCategory());
        context.put("subcategory", item.getSubcategory());
        Double price = price(item.getExtFields());
        if (price != null) {
            context.put("price", price);
        }
        Map<String, Object> ext = objectMap(event.getExtFields());
        ext.put(FIELD, context);
        event.setExtFields(ext);
    }

    private Map<String, Object> objectMap(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>(objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {}));
        } catch (IllegalArgumentException ignored) {
            return new LinkedHashMap<>();
        }
    }

    private Double price(Object value) {
        Map<String, Object> fields = objectMap(value);
        for (String name : new String[] {"price", "unitPrice", "unit_price"}) {
            Object raw = fields.get(name);
            if (raw == null) {
                continue;
            }
            try {
                double parsed = Double.parseDouble(String.valueOf(raw));
                if (Double.isFinite(parsed) && parsed >= 0d) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
