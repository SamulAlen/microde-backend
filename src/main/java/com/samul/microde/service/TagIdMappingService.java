package com.samul.microde.service;

import java.util.Map;

/**
 * 标签ID映射服务
 * 用于 BitSet 优化
 *
 * @author Samul_Alen
 */
public interface TagIdMappingService {

    /**
     * 获取标签名称到ID的映射
     */
    Map<String, Integer> getTagIdMap();
}
