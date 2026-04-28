package com.openclaw.delayqueue.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/mock")
public class MockExternalController {
    @GetMapping("/external")
    public Map<String, Object> externalGet(HttpServletRequest request) {
        return response("external", request.getMethod(), request.getQueryString(), null);
    }

    @PostMapping("/external")
    public Map<String, Object> externalPost(HttpServletRequest request, @RequestBody(required = false) String body) {
        return response("external", request.getMethod(), request.getQueryString(), body);
    }

    @PostMapping("/callback")
    public Map<String, Object> callback(HttpServletRequest request, @RequestBody(required = false) String body) {
        return response("callback", request.getMethod(), request.getQueryString(), body);
    }

    private Map<String, Object> response(String type, String method, String query, String body) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("method", method);
        data.put("query", query);
        data.put("body", body);
        data.put("receivedAt", System.currentTimeMillis());
        return data;
    }
}
