package com.openclaw.delayqueue.mapper;

import com.openclaw.delayqueue.model.ApiConfigDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ApiConfigMapper {
    @Insert("""
            INSERT INTO delay_api_config (
              name, queue_type, delay_time, content,
              request_method, request_url, request_headers, request_body,
              callback_method, callback_url, callback_headers, callback_body,
              created_at, updated_at
            ) VALUES (
              #{name}, #{queueType}, #{delayTime}, #{content},
              #{requestMethod}, #{requestUrl}, #{requestHeaders}, #{requestBody},
              #{callbackMethod}, #{callbackUrl}, #{callbackHeaders}, #{callbackBody},
              #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ApiConfigDTO dto);

    @Update("""
            UPDATE delay_api_config SET
              name = #{name},
              queue_type = #{queueType},
              delay_time = #{delayTime},
              content = #{content},
              request_method = #{requestMethod},
              request_url = #{requestUrl},
              request_headers = #{requestHeaders},
              request_body = #{requestBody},
              callback_method = #{callbackMethod},
              callback_url = #{callbackUrl},
              callback_headers = #{callbackHeaders},
              callback_body = #{callbackBody},
              updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    void update(ApiConfigDTO dto);

    @Select("SELECT * FROM delay_api_config ORDER BY updated_at DESC")
    List<ApiConfigDTO> list();

    @Delete("DELETE FROM delay_api_config WHERE id = #{id}")
    void delete(@Param("id") Long id);
}
