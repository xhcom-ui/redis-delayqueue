package com.openclaw.delayqueue.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.delayqueue.model.DelayMessageDTO;
import com.openclaw.delayqueue.queue.RedissonDelayQueueComponent;
import com.openclaw.delayqueue.queue.ZsetDelayQueueComponent;
import com.openclaw.delayqueue.service.DelayTaskExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DelayQueueConsumerRunner implements CommandLineRunner {
    private final ZsetDelayQueueComponent zsetComponent;
    private final RedissonDelayQueueComponent redissonComponent;
    private final DelayTaskExecutorService taskExecutorService;
    private final ObjectMapper objectMapper;

    public DelayQueueConsumerRunner(
            ZsetDelayQueueComponent zsetComponent,
            RedissonDelayQueueComponent redissonComponent,
            DelayTaskExecutorService taskExecutorService,
            ObjectMapper objectMapper
    ) {
        this.zsetComponent = zsetComponent;
        this.redissonComponent = redissonComponent;
        this.taskExecutorService = taskExecutorService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 100)
    public void consumeZset() {
        // ZSET 方案通过定时轮询到期 score，Lua 脚本保证取出和删除是原子操作。
        String payload = zsetComponent.consumeMessage();
        if (payload == null || payload.isBlank()) {
            return;
        }
        publish(payload);
    }

    @Override
    public void run(String... args) {
        // Redisson 延迟队列使用阻塞消费，单独后台线程避免占用 Spring 启动线程。
        Thread thread = new Thread(this::consumeRedisson, "redisson-delay-queue-consumer");
        thread.setDaemon(true);
        thread.start();
    }

    private void consumeRedisson() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                publish(redissonComponent.takeMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Redisson 延时队列消费异常", e);
            }
        }
    }

    private void publish(String payload) {
        try {
            DelayMessageDTO dto = objectMapper.readValue(payload, DelayMessageDTO.class);
            taskExecutorService.executeOnce(dto);
        } catch (Exception e) {
            log.error("延时队列消息处理失败 payload={}", payload, e);
        }
    }
}
