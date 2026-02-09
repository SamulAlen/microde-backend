package com.samul.microde.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.samul.microde.mapper.TagIdMappingMapper;
import com.samul.microde.model.domain.TagIdMapping;
import com.samul.microde.service.TagIdMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标签ID映射服务实现
 *
 * @author Samul_Alen
 */
@Service
@Slf4j
public class TagIdMappingServiceImpl implements TagIdMappingService {

    @Resource
    private TagIdMappingMapper tagIdMappingMapper;

    private Map<String, Integer> tagIdMap = new HashMap<>();

    @PostConstruct
    public void init() {
        loadTagMappings();
    }

    @Override
    public Map<String, Integer> getTagIdMap() {
        return tagIdMap;
    }

    private void loadTagMappings() {
        try {
            QueryWrapper<TagIdMapping> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_active", 1);
            List<TagIdMapping> mappings = tagIdMappingMapper.selectList(queryWrapper);

            tagIdMap = mappings.stream()
                    .collect(Collectors.toMap(
                            TagIdMapping::getTagName,
                            TagIdMapping::getId
                    ));

            log.info("标签映射加载完成，共 {} 个标签", tagIdMap.size());
        } catch (Exception e) {
            log.error("加载标签映射失败", e);
        }
    }
}
