package com.openrec.service.kafka;

import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.proto.biz.push.PushCmd;
import org.junit.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class KafkaServiceUnitTest {
    @Test public void serializesEachDomainObjectToItsTopic() {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        KafkaService service = new KafkaService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", template);
        ReflectionTestUtils.setField(service, "itemTopic", "items");
        ReflectionTestUtils.setField(service, "userTopic", "users");
        ReflectionTestUtils.setField(service, "eventTopic", "events");
        Item item = new Item(); item.setId("i"); User user = new User(); user.setId("u");
        Event event = new Event(); event.setUserId("u"); event.setItemId("i");
        service.writeItem(PushCmd.DELETE, item); service.writeUser(PushCmd.UPDATE, user);
        service.writeEvent(PushCmd.INSERT, event);
        verify(template).send(eq("items"), eq("i"), contains("\"operation\":\"DELETE\""));
        verify(template).send(eq("users"), eq("u"), contains("\"operation\":\"UPDATE\""));
        verify(template).send(eq("events"), eq("u"), contains("\"itemId\":\"i\""));
    }
}
