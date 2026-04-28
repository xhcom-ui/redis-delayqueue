package com.openclaw.delayqueue.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiConfigDTO {
    private Long id;
    private String name;
    private String queueType;
    private Long delayTime;
    private String content;
    private String requestMethod;
    private String requestUrl;
    private String requestHeaders;
    private String requestBody;
    private String callbackMethod;
    private String callbackUrl;
    private String callbackHeaders;
    private String callbackBody;
    private Long createdAt;
    private Long updatedAt;
}
