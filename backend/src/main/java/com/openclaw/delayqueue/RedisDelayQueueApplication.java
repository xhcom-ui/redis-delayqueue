package com.openclaw.delayqueue;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.openclaw.delayqueue.mapper")
@SpringBootApplication
public class RedisDelayQueueApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisDelayQueueApplication.class, args);
    }
}
