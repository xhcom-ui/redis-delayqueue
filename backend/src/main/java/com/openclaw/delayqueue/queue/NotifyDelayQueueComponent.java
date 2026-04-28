package com.openclaw.delayqueue.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.delayqueue.model.DelayMessageDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class NotifyDelayQueueComponent {
    public static final String PREFIX = "delay:queue:notify:";
    public static final String PAYLOAD_PREFIX = "delay:queue:notify:payload:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public NotifyDelayQueueComponent(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendMessage(DelayMessageDTO dto) throws JsonProcessingException {
        String id = dto.getMessageId();
        String payload = objectMapper.writeValueAsString(dto);
        redisTemplate.opsForValue().set(PAYLOAD_PREFIX + id, payload, dto.getDelayTime() + 300, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(PREFIX + id, "1", dto.getDelayTime(), TimeUnit.SECONDS);
    }

    public String getPayload(String messageId) {
        return redisTemplate.opsForValue().get(PAYLOAD_PREFIX + messageId);
    }

    public void removePayload(String messageId) {
        redisTemplate.delete(PAYLOAD_PREFIX + messageId);
    }

    public void clear() {
        deleteByPattern(PREFIX + "*");
        deleteByPattern(PAYLOAD_PREFIX + "*");
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
