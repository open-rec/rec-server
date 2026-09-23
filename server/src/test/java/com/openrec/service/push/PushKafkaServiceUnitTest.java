package com.openrec.service.push;

import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.UserReq;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.service.kafka.KafkaService;
import com.openrec.proto.biz.push.PushCmd;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class PushKafkaServiceUnitTest {
    @Test
    public void failedAcknowledgementStopsBatchAndPropagatesFailure() {
        KafkaService kafka = mock(KafkaService.class);
        PushKafkaService service = new PushKafkaService();
        ReflectionTestUtils.setField(service, "kafkaService", kafka);
        ReflectionTestUtils.setField(service, "eventItemContextEnricher", mock(EventItemContextEnricher.class));
        Item first = new Item(), second = new Item(), third = new Item();
        first.setId("first");
        second.setId("second");
        third.setId("third");
        ItemReq request = new ItemReq();
        request.setData(Arrays.asList(first, second, third));
        IllegalStateException failure = new IllegalStateException("Kafka delivery failed");
        doThrow(failure).when(kafka).writeItem(PushCmd.INSERT, second);
        try {
            service.pushItem(request);
            fail("batch must not succeed after a failed delivery");
        } catch (IllegalStateException error) {
            assertSame(failure, error);
        }
        verify(kafka).writeItem(PushCmd.INSERT, first);
        verify(kafka, never()).writeItem(PushCmd.INSERT, third);
    }

    @Test
    public void delegatesEveryElementToKafka() {
        KafkaService kafka = mock(KafkaService.class);
        PushKafkaService service = new PushKafkaService();
        ReflectionTestUtils.setField(service, "kafkaService", kafka);
        ReflectionTestUtils.setField(service, "eventItemContextEnricher", mock(EventItemContextEnricher.class));
        Item i1 = new Item(), i2 = new Item();
        i1.setId("i1");
        i2.setId("i2");
        ItemReq items = new ItemReq();
        items.setData(Arrays.asList(i1, i2));
        User u1 = new User(), u2 = new User();
        u1.setId("u1");
        u2.setId("u2");
        UserReq users = new UserReq();
        users.setData(Arrays.asList(u1, u2));
        Event e1 = new Event(), e2 = new Event();
        EventReq events = new EventReq();
        events.setData(Arrays.asList(e1, e2));
        service.pushItem(items);
        service.pushUser(users);
        service.pushEvent(events);
        verify(kafka, times(2)).writeItem(eq(PushCmd.INSERT), any(Item.class));
        verify(kafka, times(2)).writeUser(eq(PushCmd.INSERT), any(User.class));
        verify(kafka, times(2)).writeEvent(eq(PushCmd.INSERT), any(Event.class));
    }
}
