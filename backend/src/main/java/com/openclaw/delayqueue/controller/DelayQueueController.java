package com.openclaw.delayqueue.controller;

import com.openclaw.delayqueue.model.ApiResponse;
import com.openclaw.delayqueue.model.ApiConfigDTO;
import com.openclaw.delayqueue.model.DelayMessageDTO;
import com.openclaw.delayqueue.model.DelayTaskRecord;
import com.openclaw.delayqueue.queue.NotifyDelayQueueComponent;
import com.openclaw.delayqueue.queue.RedissonDelayQueueComponent;
import com.openclaw.delayqueue.queue.ZsetDelayQueueComponent;
import com.openclaw.delayqueue.repository.ApiConfigRepository;
import com.openclaw.delayqueue.repository.DelayTaskRepository;
import com.openclaw.delayqueue.service.DelayTaskExecutorService;
import com.openclaw.delayqueue.sse.DelayQueueEventPublisher;
import com.openclaw.delayqueue.sse.SseServer;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/delay-queue")
public class DelayQueueController {
    private final ZsetDelayQueueComponent zsetComponent;
    private final NotifyDelayQueueComponent notifyComponent;
    private final RedissonDelayQueueComponent redissonComponent;
    private final DelayQueueEventPublisher eventPublisher;
    private final SseServer sseServer;
    private final DelayTaskRepository taskRepository;
    private final DelayTaskExecutorService taskExecutorService;
    private final ApiConfigRepository apiConfigRepository;

    public DelayQueueController(
            ZsetDelayQueueComponent zsetComponent,
            NotifyDelayQueueComponent notifyComponent,
            RedissonDelayQueueComponent redissonComponent,
            DelayQueueEventPublisher eventPublisher,
            SseServer sseServer,
            DelayTaskRepository taskRepository,
            DelayTaskExecutorService taskExecutorService,
            ApiConfigRepository apiConfigRepository
    ) {
        this.zsetComponent = zsetComponent;
        this.notifyComponent = notifyComponent;
        this.redissonComponent = redissonComponent;
        this.eventPublisher = eventPublisher;
        this.sseServer = sseServer;
        this.taskRepository = taskRepository;
        this.taskExecutorService = taskExecutorService;
        this.apiConfigRepository = apiConfigRepository;
    }

    @PostMapping("/send")
    public ApiResponse<DelayMessageDTO> sendMessage(@RequestBody DelayMessageDTO dto) throws Exception {
        normalize(dto);
        taskRepository.insertPending(dto);
        switch (dto.getQueueType()) {
            case "zset" -> zsetComponent.sendMessage(dto);
            case "notify" -> notifyComponent.sendMessage(dto);
            case "redisson" -> redissonComponent.sendMessage(dto);
            default -> throw new IllegalArgumentException("不支持的队列类型: " + dto.getQueueType());
        }
        return ApiResponse.ok(dto);
    }

    @PostMapping("/clear")
    public ApiResponse<String> clear() {
        zsetComponent.clear();
        notifyComponent.clear();
        redissonComponent.clear();
        eventPublisher.clearIdempotentKeys();
        taskRepository.clear();
        return ApiResponse.ok("cleared");
    }

    @GetMapping("/tasks")
    public ApiResponse<List<DelayTaskRecord>> tasks() {
        return ApiResponse.ok(taskExecutorService.listRecent(100));
    }

    @GetMapping("/configs")
    public ApiResponse<List<ApiConfigDTO>> configs() {
        return ApiResponse.ok(apiConfigRepository.list());
    }

    @PostMapping("/configs")
    public ApiResponse<ApiConfigDTO> saveConfig(@RequestBody ApiConfigDTO dto) {
        normalizeConfig(dto);
        return ApiResponse.ok(apiConfigRepository.save(dto));
    }

    @PostMapping("/configs/{id}/delete")
    public ApiResponse<String> deleteConfig(@PathVariable Long id) {
        apiConfigRepository.delete(id);
        return ApiResponse.ok("deleted");
    }

    @GetMapping("/sse/{clientId}")
    public SseEmitter sse(@PathVariable String clientId) {
        return sseServer.connect(clientId);
    }

    private void normalize(DelayMessageDTO dto) {
        if (dto.getQueueType() == null || dto.getQueueType().isBlank()) {
            dto.setQueueType("redisson");
        }
        dto.setQueueType(dto.getQueueType().toLowerCase(Locale.ROOT));
        if (dto.getMessageId() == null || dto.getMessageId().isBlank()) {
            dto.setMessageId(UUID.randomUUID().toString());
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            dto.setContent("延时接口任务");
        }
        if (dto.getDelayTime() == null || dto.getDelayTime() < 1) {
            dto.setDelayTime(1L);
        }
        if (dto.getRequestMethod() == null || dto.getRequestMethod().isBlank()) {
            dto.setRequestMethod("POST");
        }
        dto.setRequestMethod(dto.getRequestMethod().toUpperCase(Locale.ROOT));
        if (dto.getRequestUrl() == null || dto.getRequestUrl().isBlank()) {
            throw new IllegalArgumentException("外部接口 URL 不能为空");
        }
        if (dto.getCallbackMethod() == null || dto.getCallbackMethod().isBlank()) {
            dto.setCallbackMethod("POST");
        }
        dto.setCallbackMethod(dto.getCallbackMethod().toUpperCase(Locale.ROOT));
        if (dto.getCallbackUrl() == null || dto.getCallbackUrl().isBlank()) {
            throw new IllegalArgumentException("回调 URL 不能为空");
        }
        dto.setCreateTime(System.currentTimeMillis());
    }

    private void normalizeConfig(ApiConfigDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("配置名称不能为空");
        }
        if (dto.getQueueType() == null || dto.getQueueType().isBlank()) {
            dto.setQueueType("redisson");
        }
        dto.setQueueType(dto.getQueueType().toLowerCase(Locale.ROOT));
        if (dto.getDelayTime() == null || dto.getDelayTime() < 1) {
            dto.setDelayTime(5L);
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            dto.setContent(dto.getName());
        }
        if (dto.getRequestUrl() == null || dto.getRequestUrl().isBlank()) {
            throw new IllegalArgumentException("外部接口 URL 不能为空");
        }
        if (dto.getRequestMethod() == null || dto.getRequestMethod().isBlank()) {
            dto.setRequestMethod("POST");
        }
        dto.setRequestMethod(dto.getRequestMethod().toUpperCase(Locale.ROOT));
        if (dto.getCallbackMethod() == null || dto.getCallbackMethod().isBlank()) {
            dto.setCallbackMethod("POST");
        }
        dto.setCallbackMethod(dto.getCallbackMethod().toUpperCase(Locale.ROOT));
        if (dto.getCallbackUrl() == null || dto.getCallbackUrl().isBlank()) {
            throw new IllegalArgumentException("回调 URL 不能为空");
        }
    }
}
