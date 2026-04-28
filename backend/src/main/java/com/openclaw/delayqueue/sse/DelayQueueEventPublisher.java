package com.openclaw.delayqueue.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.delayqueue.model.ExecutionLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DelayQueueEventPublisher {
    private static final String CONSUMED_PREFIX = "delay:queue:consumed:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;
    private final String sseChannel;

    public DelayQueueEventPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${delay-queue.instance-id}") String instanceId,
            @Value("${delay-queue.sse-channel}") String sseChannel
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
        this.sseChannel = sseChannel;
    }

    public boolean acquireConsumeLock(String messageId) {
        // 多实例同时拿到同一条消息时，只有 SETNX 成功的实例继续执行外部接口和回调。
        String consumedKey = CONSUMED_PREFIX + messageId;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(consumedKey, instanceId, 24, TimeUnit.HOURS);
        return Boolean.TRUE.equals(locked);
    }

    public void publish(ExecutionLog log) throws JsonProcessingException {
        redisTemplate.convertAndSend(sseChannel, objectMapper.writeValueAsString(log));
    }

    public void clearIdempotentKeys() {
        var keys = redisTemplate.keys(CONSUMED_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
