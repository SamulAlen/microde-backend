package com.samul.microde.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.samul.microde.model.dto.RecommendRequest;
import com.samul.microde.model.dto.RecommendationResult;
import com.samul.microde.service.AsyncRecommendationService;
import com.samul.microde.service.RecommendationService;
import com.samul.microde.utils.RateLimiterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * 异步推荐服务实现
 * 提供异步计算能力，避免长时间阻塞用户请求
 *
 * @author Samul_Alen
 */
@Service
@Slf4j
public class AsyncRecommendationServiceImpl implements AsyncRecommendationService {

    @Resource
    private RecommendationService recommendationService;

    @Resource
    private RateLimiterUtil rateLimiterUtil;

    /**
     * 异步执行推荐计算
     *
     * @param request 推荐请求
     * @return CompletableFuture包装的推荐结果
     */
    @Async("recommendExecutor")
    @Override
    public CompletableFuture<Page<RecommendationResult>> recommendUsersAsync(RecommendRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("开始异步推荐计算: userId={}, strategy={}", request.getUserId(), request.getStrategy());

        try {
            // 执行推荐计算
            Page<RecommendationResult> result = recommendationService.recommendUsers(request);

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("异步推荐计算完成: userId={}, 耗时={}ms, 结果数={}",
                    request.getUserId(), elapsedTime, result.getRecords().size());

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("异步推荐计算失败: userId={}", request.getUserId(), e);
            // Java 8 兼容方式：创建一个异常完成的Future
            CompletableFuture<Page<RecommendationResult>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    /**
     * 带限流的异步推荐
     * 如果触发限流，返回缓存的推荐结果或降级结果
     *
     * @param request 推荐请求
     * @return CompletableFuture包装的推荐结果
     */
    @Async("recommendExecutor")
    @Override
    public CompletableFuture<Page<RecommendationResult>> recommendUsersWithRateLimitAsync(RecommendRequest request) {
        Long userId = request.getUserId();

        // 检查限流：10秒内最多3次请求
        if (!rateLimiterUtil.allowRecommendRequest(userId)) {
            log.warn("推荐请求触发限流，返回降级结果: userId={}", userId);

            // 返回降级结果（简单的随机推荐）
            Page<RecommendationResult> fallbackResult = createFallbackResult(request);

            return CompletableFuture.completedFuture(fallbackResult);
        }

        // 未触发限流，执行正常推荐
        return recommendUsersAsync(request);
    }

    /**
     * 创建降级结果
     * 当限流触发或计算失败时使用
     *
     * @param request 推荐请求
     * @return 降级推荐结果
     */
    private Page<RecommendationResult> createFallbackResult(RecommendRequest request) {
        log.info("创建降级推荐结果: userId={}", request.getUserId());

        Page<RecommendationResult> result = new Page<>(request.getPageNum(), request.getPageSize());
        result.setRecords(new java.util.ArrayList<>());
        result.setTotal(0);
        result.setPages(0);

        // 可以在这里添加一些简单的推荐逻辑
        // 例如：返回最近活跃的用户

        return result;
    }

    /**
     * 批量异步推荐（用于预热缓存）
     *
     * @param requests 推荐请求列表
     * @return CompletableFuture列表
     */
    @Async("recommendExecutor")
    @Override
    public CompletableFuture<java.util.List<Page<RecommendationResult>>> batchRecommendAsync(
            java.util.List<RecommendRequest> requests) {

        log.info("开始批量异步推荐: 任务数={}", requests.size());

        java.util.List<CompletableFuture<Page<RecommendationResult>>> futures = requests.stream()
                .map(this::recommendUsersAsync)
                .collect(java.util.stream.Collectors.toList());

        // 等待所有任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        // 收集结果
        return allFutures.thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList()));
    }
}
