package com.openclaw.delayqueue.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.delayqueue.model.DelayMessageDTO;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonDelayQueueComponent {
    private static final String QUEUE_NAME = "delay:queue:redisson";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public RedissonDelayQueueComponent(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    public void sendMessage(DelayMessageDTO dto) throws JsonProcessingException {
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(QUEUE_NAME);
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        delayedQueue.offer(objectMapper.writeValueAsString(dto), dto.getDelayTime(), TimeUnit.SECONDS);
    }

    public String takeMessage() throws InterruptedException {
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(QUEUE_NAME);
        return blockingQueue.take();
    }

    public void clear() {
        redissonClient.getBlockingQueue(QUEUE_NAME).clear();
        redissonClient.getDelayedQueue(redissonClient.<String>getBlockingQueue(QUEUE_NAME)).clear();
    }
}
