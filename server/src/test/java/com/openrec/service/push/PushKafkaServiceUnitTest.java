package com.openrec.service.push;

import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.UserReq;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.service.kafka.KafkaService;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.mockito.Mockito.*;

public class PushKafkaServiceUnitTest {
    @Test public void delegatesEveryElementToKafka() {
        KafkaService kafka = mock(KafkaService.class);
        PushKafkaService service = new PushKafkaService();
        ReflectionTestUtils.setField(service, "kafkaService", kafka);
        Item i1 = new Item(), i2 = new Item(); ItemReq items = new ItemReq(); items.setData(Arrays.asList(i1, i2));
        User u1 = new User(), u2 = new User(); UserReq users = new UserReq(); users.setData(Arrays.asList(u1, u2));
        Event e1 = new Event(), e2 = new Event(); EventReq events = new EventReq(); events.setData(Arrays.asList(e1, e2));
        service.pushItem(items); service.pushUser(users); service.pushEvent(events);
        verify(kafka, times(2)).writeItem(any(Item.class));
        verify(kafka, times(2)).writeUser(any(User.class));
        verify(kafka, times(2)).writeEvent(any(Event.class));
    }
}
