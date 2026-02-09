package com.samul.microde.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.samul.microde.model.dto.RecommendRequest;
import com.samul.microde.model.dto.RecommendationResult;

/**
 * 推荐降级服务接口
 * 当主推荐服务失败时提供轻量级的降级方案
 *
 * @author Samul_Alen
 */
public interface RecommendationFallbackService {

    /**
     * 轻量级推荐：只返回活跃度最高的用户
     * 不进行复杂的相似度/互补度计算
     *
     * @param request 推荐请求
     * @return 轻量级推荐结果
     */
    Page<RecommendationResult> lightweightRecommend(RecommendRequest request);

    /**
     * 随机推荐降级：返回随机用户
     *
     * @param request 推荐请求
     * @return 随机推荐结果
     */
    Page<RecommendationResult> randomRecommend(RecommendRequest request);

    /**
     * 基于标签的简单推荐
     *
     * @param request 推荐请求
     * @return 基于标签的推荐结果
     */
    Page<RecommendationResult> tagBasedRecommend(RecommendRequest request);
}
