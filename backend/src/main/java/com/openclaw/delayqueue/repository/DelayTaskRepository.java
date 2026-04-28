package com.openclaw.delayqueue.repository;

import com.openclaw.delayqueue.mapper.DelayTaskMapper;
import com.openclaw.delayqueue.model.DelayMessageDTO;
import com.openclaw.delayqueue.model.DelayTaskRecord;
import com.openclaw.delayqueue.model.HttpCallResult;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DelayTaskRepository {
    private final DelayTaskMapper delayTaskMapper;

    public DelayTaskRepository(DelayTaskMapper delayTaskMapper) {
        this.delayTaskMapper = delayTaskMapper;
    }

    public void insertPending(DelayMessageDTO dto) {
        long now = System.currentTimeMillis();
        long executeTime = dto.getCreateTime() + dto.getDelayTime() * 1000;
        delayTaskMapper.insertPending(dto, executeTime, now);
    }

    public DelayTaskRecord findByMessageId(String messageId) {
        return delayTaskMapper.findByMessageId(messageId);
    }

    public List<DelayTaskRecord> listRecent(int limit) {
        return delayTaskMapper.listRecent(limit);
    }

    public void markRunning(String messageId, String instanceId) {
        delayTaskMapper.markRunning(messageId, instanceId, System.currentTimeMillis());
    }

    public void complete(String messageId, String status, HttpCallResult requestResult, HttpCallResult callbackResult) {
        long now = System.currentTimeMillis();
        delayTaskMapper.complete(
                messageId,
                status,
                requestResult == null ? null : requestResult.statusCode(),
                requestResult == null ? null : requestResult.responseBody(),
                requestResult == null ? null : requestResult.errorMessage(),
                callbackResult == null ? null : callbackResult.statusCode(),
                callbackResult == null ? null : callbackResult.responseBody(),
                callbackResult == null ? null : callbackResult.errorMessage(),
                now,
                now
        );
    }

    public void clear() {
        delayTaskMapper.clear();
    }
}
