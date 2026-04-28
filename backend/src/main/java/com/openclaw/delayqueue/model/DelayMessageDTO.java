package com.openclaw.delayqueue.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DelayMessageDTO implements Serializable {
    private String messageId;
    private String queueType;
    private String content;
    private Long delayTime;
    private Long createTime;
    private String requestMethod;
    private String requestUrl;
    private String requestHeaders;
    private String requestBody;
    private String callbackMethod;
    private String callbackUrl;
    private String callbackHeaders;
    private String callbackBody;
}
