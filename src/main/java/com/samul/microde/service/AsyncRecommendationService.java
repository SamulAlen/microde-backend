package com.samul.microde.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.samul.microde.model.dto.RecommendRequest;
import com.samul.microde.model.dto.RecommendationResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步推荐服务接口
 * 提供异步计算能力，避免长时间阻塞用户请求
 *
 * @author Samul_Alen
 */
public interface AsyncRecommendationService {

    /**
     * 异步执行推荐计算
     *
     * @param request 推荐请求
     * @return CompletableFuture包装的推荐结果
     */
    CompletableFuture<Page<RecommendationResult>> recommendUsersAsync(RecommendRequest request);

    /**
     * 带限流的异步推荐
     * 如果触发限流，返回缓存的推荐结果或降级结果
     *
     * @param request 推荐请求
     * @return CompletableFuture包装的推荐结果
     */
    CompletableFuture<Page<RecommendationResult>> recommendUsersWithRateLimitAsync(RecommendRequest request);

    /**
     * 批量异步推荐（用于预热缓存）
     *
     * @param requests 推荐请求列表
     * @return CompletableFuture列表
     */
    CompletableFuture<List<Page<RecommendationResult>>> batchRecommendAsync(List<RecommendRequest> requests);
}
