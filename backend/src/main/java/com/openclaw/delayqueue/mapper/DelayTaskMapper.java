package com.openclaw.delayqueue.mapper;

import com.openclaw.delayqueue.model.DelayMessageDTO;
import com.openclaw.delayqueue.model.DelayTaskRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DelayTaskMapper {
    @Insert("""
            INSERT INTO delay_task (
              message_id, queue_type, content, delay_time, create_time, execute_time, status,
              request_method, request_url, request_headers, request_body,
              callback_method, callback_url, callback_headers, callback_body, updated_at
            ) VALUES (
              #{dto.messageId}, #{dto.queueType}, #{dto.content}, #{dto.delayTime}, #{dto.createTime},
              #{executeTime}, 'PENDING',
              #{dto.requestMethod}, #{dto.requestUrl}, #{dto.requestHeaders}, #{dto.requestBody},
              #{dto.callbackMethod}, #{dto.callbackUrl}, #{dto.callbackHeaders}, #{dto.callbackBody},
              #{updatedAt}
            )
            """)
    void insertPending(@Param("dto") DelayMessageDTO dto, @Param("executeTime") long executeTime, @Param("updatedAt") long updatedAt);

    @Select("SELECT * FROM delay_task WHERE message_id = #{messageId}")
    DelayTaskRecord findByMessageId(@Param("messageId") String messageId);

    @Select("SELECT * FROM delay_task ORDER BY create_time DESC LIMIT #{limit}")
    List<DelayTaskRecord> listRecent(@Param("limit") int limit);

    @Update("""
            UPDATE delay_task
            SET status = 'RUNNING', instance_id = #{instanceId}, updated_at = #{updatedAt}
            WHERE message_id = #{messageId}
            """)
    void markRunning(@Param("messageId") String messageId, @Param("instanceId") String instanceId, @Param("updatedAt") long updatedAt);

    @Update("""
            UPDATE delay_task SET
              status = #{status},
              request_status = #{requestStatus},
              request_response = #{requestResponse},
              request_error = #{requestError},
              callback_status = #{callbackStatus},
              callback_response = #{callbackResponse},
              callback_error = #{callbackError},
              executed_at = #{executedAt},
              updated_at = #{updatedAt}
            WHERE message_id = #{messageId}
            """)
    void complete(
            @Param("messageId") String messageId,
            @Param("status") String status,
            @Param("requestStatus") Integer requestStatus,
            @Param("requestResponse") String requestResponse,
            @Param("requestError") String requestError,
            @Param("callbackStatus") Integer callbackStatus,
            @Param("callbackResponse") String callbackResponse,
            @Param("callbackError") String callbackError,
            @Param("executedAt") long executedAt,
            @Param("updatedAt") long updatedAt
    );

    @Delete("DELETE FROM delay_task")
    void clear();
}
