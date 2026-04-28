package com.openclaw.delayqueue.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.delayqueue.model.DelayMessageDTO;
import com.openclaw.delayqueue.model.DelayTaskRecord;
import com.openclaw.delayqueue.model.ExecutionLog;
import com.openclaw.delayqueue.model.HttpCallResult;
import com.openclaw.delayqueue.repository.DelayTaskRepository;
import com.openclaw.delayqueue.sse.DelayQueueEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class DelayTaskExecutorService {
    private final DelayQueueEventPublisher eventPublisher;
    private final DelayTaskRepository taskRepository;
    private final HttpExecutor httpExecutor;
    private final ObjectMapper objectMapper;
    private final String instanceId;

    public DelayTaskExecutorService(
            DelayQueueEventPublisher eventPublisher,
            DelayTaskRepository taskRepository,
            HttpExecutor httpExecutor,
            ObjectMapper objectMapper,
            @Value("${delay-queue.instance-id}") String instanceId
    ) {
        this.eventPublisher = eventPublisher;
        this.taskRepository = taskRepository;
        this.httpExecutor = httpExecutor;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
    }

    public boolean executeOnce(DelayMessageDTO dto) {
        // 幂等锁放在真正执行 HTTP 前，保证多台服务部署时外部接口只会被调用一次。
        if (!eventPublisher.acquireConsumeLock(dto.getMessageId())) {
            return false;
        }

        taskRepository.markRunning(dto.getMessageId(), instanceId);
        HttpCallResult requestResult = null;
        HttpCallResult callbackResult = null;
        String status = "SUCCESS";
        try {
            requestResult = httpExecutor.execute(dto.getRequestMethod(), dto.getRequestUrl(), dto.getRequestHeaders(), dto.getRequestBody());
            if (requestResult != null && !requestResult.success()) {
                status = "REQUEST_FAILED";
            }

            callbackResult = httpExecutor.execute(
                    dto.getCallbackMethod(),
                    dto.getCallbackUrl(),
                    dto.getCallbackHeaders(),
                    buildCallbackBody(dto, requestResult)
            );
            if (callbackResult != null && !callbackResult.success()) {
                status = "CALLBACK_FAILED";
            }
        } catch (Exception e) {
            status = "FAILED";
            requestResult = new HttpCallResult(null, null, e.getMessage());
            log.error("延时任务执行失败 messageId={}", dto.getMessageId(), e);
        } finally {
            taskRepository.complete(dto.getMessageId(), status, requestResult, callbackResult);
            publishLog(dto, status, requestResult, callbackResult);
        }
        return true;
    }

    public java.util.List<DelayTaskRecord> listRecent(int limit) {
        return taskRepository.listRecent(limit);
    }

    private String buildCallbackBody(DelayMessageDTO dto, HttpCallResult requestResult) throws JsonProcessingException {
        if (dto.getCallbackBody() != null && !dto.getCallbackBody().isBlank()) {
            return dto.getCallbackBody();
        }
        // 未配置回调 Body 时，自动把本次外部接口执行结果作为回调入参。
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", dto.getMessageId());
        payload.put("queueType", dto.getQueueType());
        payload.put("content", dto.getContent());
        payload.put("requestStatus", requestResult == null ? null : requestResult.statusCode());
        payload.put("requestResponse", requestResult == null ? null : requestResult.responseBody());
        payload.put("requestError", requestResult == null ? null : requestResult.errorMessage());
        payload.put("executedAt", System.currentTimeMillis());
        return objectMapper.writeValueAsString(payload);
    }

    private void publishLog(
            DelayMessageDTO dto,
            String status,
            HttpCallResult requestResult,
            HttpCallResult callbackResult
    ) {
        try {
            String errorMessage = requestResult != null && requestResult.errorMessage() != null
                    ? requestResult.errorMessage()
                    : callbackResult == null ? null : callbackResult.errorMessage();
            eventPublisher.publish(new ExecutionLog(
                    UUID.randomUUID().toString(),
                    dto.getMessageId(),
                    dto.getQueueType(),
                    dto.getContent(),
                    instanceId,
                    status,
                    requestResult == null ? null : requestResult.statusCode(),
                    callbackResult == null ? null : callbackResult.statusCode(),
                    errorMessage,
                    System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.warn("发布 SSE 执行日志失败 messageId={}", dto.getMessageId(), e);
        }
    }
}
