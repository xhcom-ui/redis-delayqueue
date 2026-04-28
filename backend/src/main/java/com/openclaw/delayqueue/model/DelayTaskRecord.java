package com.openclaw.delayqueue.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DelayTaskRecord {
    private Long id;
    private String messageId;
    private String queueType;
    private String content;
    private Long delayTime;
    private Long createTime;
    private Long executeTime;
    private String status;
    private String instanceId;
    private String requestMethod;
    private String requestUrl;
    private String requestHeaders;
    private String requestBody;
    private Integer requestStatus;
    private String requestResponse;
    private String requestError;
    private String callbackMethod;
    private String callbackUrl;
    private String callbackHeaders;
    private String callbackBody;
    private Integer callbackStatus;
    private String callbackResponse;
    private String callbackError;
    private Long executedAt;
    private Long updatedAt;
}
