package com.openclaw.delayqueue.repository;

import com.openclaw.delayqueue.mapper.ApiConfigMapper;
import com.openclaw.delayqueue.model.ApiConfigDTO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ApiConfigRepository {
    private final ApiConfigMapper apiConfigMapper;

    public ApiConfigRepository(ApiConfigMapper apiConfigMapper) {
        this.apiConfigMapper = apiConfigMapper;
    }

    public ApiConfigDTO save(ApiConfigDTO dto) {
        long now = System.currentTimeMillis();
        if (dto.getId() == null) {
            dto.setCreatedAt(now);
            dto.setUpdatedAt(now);
            apiConfigMapper.insert(dto);
        } else {
            dto.setUpdatedAt(now);
            apiConfigMapper.update(dto);
        }
        return dto;
    }

    public List<ApiConfigDTO> list() {
        return apiConfigMapper.list();
    }

    public void delete(Long id) {
        apiConfigMapper.delete(id);
    }
}
