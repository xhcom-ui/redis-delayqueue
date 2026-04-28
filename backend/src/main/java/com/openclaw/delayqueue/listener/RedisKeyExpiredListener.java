package com.openclaw.delayqueue.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.delayqueue.model.DelayMessageDTO;
import com.openclaw.delayqueue.queue.NotifyDelayQueueComponent;
import com.openclaw.delayqueue.service.DelayTaskExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {
    private final NotifyDelayQueueComponent notifyComponent;
    private final DelayTaskExecutorService taskExecutorService;
    private final ObjectMapper objectMapper;

    public RedisKeyExpiredListener(
            RedisMessageListenerContainer listenerContainer,
            NotifyDelayQueueComponent notifyComponent,
            DelayTaskExecutorService taskExecutorService,
            ObjectMapper objectMapper
    ) {
        super(listenerContainer);
        this.notifyComponent = notifyComponent;
        this.taskExecutorService = taskExecutorService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = message.toString();
        if (!key.startsWith(NotifyDelayQueueComponent.PREFIX)
                || key.startsWith(NotifyDelayQueueComponent.PAYLOAD_PREFIX)) {
            return;
        }

        String messageId = key.substring(NotifyDelayQueueComponent.PREFIX.length());
        String payload = notifyComponent.getPayload(messageId);
        if (payload == null || payload.isBlank()) {
            return;
        }

        try {
            DelayMessageDTO dto = objectMapper.readValue(payload, DelayMessageDTO.class);
            if (taskExecutorService.executeOnce(dto)) {
                notifyComponent.removePayload(messageId);
            }
        } catch (Exception e) {
            log.error("Redis 过期通知队列消费失败 key={}", key, e);
        }
    }
}
