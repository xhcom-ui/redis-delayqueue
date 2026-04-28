package com.openclaw.delayqueue.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.delayqueue.model.ExecutionLog;
import com.openclaw.delayqueue.sse.SseServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SseRedisEventSubscriber {
    private final SseServer sseServer;
    private final ObjectMapper objectMapper;

    public SseRedisEventSubscriber(SseServer sseServer, ObjectMapper objectMapper) {
        this.sseServer = sseServer;
        this.objectMapper = objectMapper;
    }

    public void onMessage(String payload) {
        try {
            sseServer.broadcast(objectMapper.readValue(payload, ExecutionLog.class));
        } catch (Exception e) {
            log.warn("解析 Redis SSE 事件失败 payload={}", payload, e);
        }
    }
}
