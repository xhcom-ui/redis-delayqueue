package com.openclaw.delayqueue.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLog {
    private String eventId;
    private String messageId;
    private String queueType;
    private String content;
    private String instanceId;
    private String status;
    private Integer requestStatus;
    private Integer callbackStatus;
    private String errorMessage;
    private Long executedAt;
}
