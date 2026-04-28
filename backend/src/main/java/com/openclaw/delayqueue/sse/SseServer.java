package com.openclaw.delayqueue.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.delayqueue.model.ExecutionLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseServer {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sentEventIds = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SseServer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter connect(String clientId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> remove(clientId));
        emitter.onTimeout(() -> remove(clientId));
        emitter.onError(error -> remove(clientId));
        emitters.put(clientId, emitter);
        sentEventIds.put(clientId, ConcurrentHashMap.newKeySet());
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(clientId);
        }
        return emitter;
    }

    public void broadcast(ExecutionLog logEvent) {
        emitters.forEach((clientId, emitter) -> send(clientId, emitter, logEvent));
    }

    private void send(String clientId, SseEmitter emitter, ExecutionLog logEvent) {
        Set<String> eventIds = sentEventIds.computeIfAbsent(clientId, key -> ConcurrentHashMap.newKeySet());
        if (!eventIds.add(logEvent.getEventId())) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .id(logEvent.getEventId())
                    .name("delay-log")
                    .data(objectMapper.writeValueAsString(logEvent), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.warn("SSE 推送失败，移除连接 clientId={}", clientId, e);
            remove(clientId);
        }
    }

    private void remove(String clientId) {
        emitters.remove(clientId);
        sentEventIds.remove(clientId);
    }
}
