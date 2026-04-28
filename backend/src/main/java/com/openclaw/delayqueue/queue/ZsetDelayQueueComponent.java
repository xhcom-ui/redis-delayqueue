package com.openclaw.delayqueue.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.delayqueue.model.DelayMessageDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class ZsetDelayQueueComponent {
    private static final String QUEUE_KEY = "delay:queue:zset";
    private static final String CONSUME_SCRIPT = """
            local msg = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1], 'LIMIT', 0, 1)
            if #msg > 0 then
              redis.call('ZREM', KEYS[1], msg[1])
              return msg[1]
            end
            return nil
            """;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<String> consumeScript;

    public ZsetDelayQueueComponent(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.consumeScript = new DefaultRedisScript<>(CONSUME_SCRIPT, String.class);
    }

    public void sendMessage(DelayMessageDTO dto) throws JsonProcessingException {
        long score = System.currentTimeMillis() + dto.getDelayTime() * 1000;
        redisTemplate.opsForZSet().add(QUEUE_KEY, objectMapper.writeValueAsString(dto), score);
    }

    public String consumeMessage() {
        return redisTemplate.execute(consumeScript, Collections.singletonList(QUEUE_KEY), String.valueOf(System.currentTimeMillis()));
    }

    public void clear() {
        redisTemplate.delete(QUEUE_KEY);
    }
}
