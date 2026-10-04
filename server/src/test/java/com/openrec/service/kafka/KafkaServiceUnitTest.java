package com.openrec.service.kafka;

import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.proto.biz.push.PushCmd;
import org.junit.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class KafkaServiceUnitTest {
    @Test
    public void serializesEachDomainObjectToItsTopic() {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, String>> acknowledged = new CompletableFuture<>();
        acknowledged.complete(null);
        when(template.send(anyString(), anyString(), anyString())).thenReturn(acknowledged);
        KafkaService service = new KafkaService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", template);
        ReflectionTestUtils.setField(service, "itemTopic", "items");
        ReflectionTestUtils.setField(service, "userTopic", "users");
        ReflectionTestUtils.setField(service, "eventTopic", "events");
        Item item = new Item();
        item.setId("i");
        User user = new User();
        user.setId("u");
        Event event = new Event();
        event.setUserId("u");
        event.setItemId("i");
        service.writeItem(PushCmd.DELETE, item);
        service.writeUser(PushCmd.UPDATE, user);
        service.writeEvent(PushCmd.INSERT, event);
        verify(template).send(eq("items"), eq("i"), contains("\"operation\":\"DELETE\""));
        verify(template).send(eq("users"), eq("u"), contains("\"operation\":\"UPDATE\""));
        verify(template).send(eq("events"), eq("u"), contains("\"itemId\":\"i\""));
    }

    @Test
    public void failedAcknowledgementFailsPush() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        RuntimeException failure = new RuntimeException("broker unavailable");
        future.completeExceptionally(failure);
        try {
            sendWith(future);
            fail("unacknowledged delivery must not succeed");
        } catch (IllegalStateException error) {
            assertSame(failure, error.getCause());
        }
    }

    @Test
    public void pendingAcknowledgementTimesOut() {
        try {
            sendWith(new CompletableFuture<>());
            fail("pending delivery must not succeed");
        } catch (IllegalStateException error) {
            assertTrue(error.getCause() instanceof TimeoutException);
        }
    }

    @Test
    public void interruptionIsPreserved() {
        Thread.currentThread().interrupt();
        try {
            sendWith(new CompletableFuture<>());
            fail("interrupted delivery must not succeed");
        } catch (IllegalStateException error) {
            assertTrue(error.getCause() instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private void sendWith(CompletableFuture<SendResult<String, String>> future) {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        when(template.send(anyString(), anyString(), anyString())).thenReturn(future);
        KafkaService service = new KafkaService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", template);
        ReflectionTestUtils.setField(service, "itemTopic", "items");
        ReflectionTestUtils.setField(service, "ackTimeoutMs", 10L);
        Item item = new Item();
        item.setId("i");
        service.writeItem(PushCmd.INSERT, item);
    }
}
