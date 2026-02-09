package com.samul.microde.controller;

import com.samul.microde.common.BaseResponse;
import com.samul.microde.common.ErrorCode;
import com.samul.microde.common.ResultUtils;
import com.samul.microde.constant.RedisCacheConstants;
import com.samul.microde.service.PrecomputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预计算控制器
 * 用于手动触发预计算任务（测试用）
 *
 * @author Samul_Alen
 */
@Tag(name = "预计算管理", description = "推荐预计算、缓存刷新等接口")
@RestController
@RequestMapping("/precompute")
@Slf4j
public class PrecomputeController {

    @Resource
    private PrecomputeService precomputeService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 手动触发全量预计算
     * POST /precompute/full
     */
    @PostMapping("/full")
    @Operation(summary = "触发全量预计算", description = "手动触发全量预计算任务，重算所有用户的相似度和互补度")
    public BaseResponse<Map<String, Object>> triggerFullPrecompute() {
        log.info("手动触发全量预计算任务...");
        long startTime = System.currentTimeMillis();

        try {
            // 全量重算相似度
            precomputeService.precomputeSimilarity(true);

            // 全量重算互补度
            precomputeService.precomputeComplement(true);

            long elapsedTime = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("elapsedTime", elapsedTime);
            result.put("elapsedTimeSeconds", elapsedTime / 1000.0);
            result.put("message", "全量预计算完成");

            log.info("全量预计算任务完成！总耗时: {}ms ({}秒)", elapsedTime, elapsedTime / 1000.0);

            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("全量预计算任务执行失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "预计算失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发增量预计算
     * POST /precompute/incremental
     */
    @PostMapping("/incremental")
    @Operation(summary = "触发增量预计算", description = "手动触发增量预计算任务，只计算缓存过期或不存在的用户")
    public BaseResponse<Map<String, Object>> triggerIncrementalPrecompute() {
        log.info("手动触发增量预计算任务...");
        long startTime = System.currentTimeMillis();

        try {
            // 增量计算相似度（不强制全量重算）
            precomputeService.precomputeSimilarity(false);

            // 增量计算互补度
            precomputeService.precomputeComplement(false);

            long elapsedTime = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("elapsedTime", elapsedTime);
            result.put("message", "增量预计算完成");

            log.info("增量预计算任务完成！总耗时: {}ms", elapsedTime);

            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("增量预计算任务执行失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "预计算失败: " + e.getMessage());
        }
    }

    /**
     * 为指定用户预计算相似度
     * POST /precompute/user/similarity?userId=123
     */
    @PostMapping("/user/similarity")
    @Operation(summary = "预计算用户相似度", description = "为指定用户预计算与其他用户的相似度")
    public BaseResponse<Map<String, Object>> precomputeUserSimilarity(Long userId) {
        log.info("为用户 {} 预计算相似度", userId);

        try {
            precomputeService.precomputeUserSimilarity(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("userId", userId);
            result.put("message", "用户相似度预计算完成");

            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("用户相似度预计算失败: userId={}", userId, e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "预计算失败: " + e.getMessage());
        }
    }

    /**
     * 为指定用户预计算互补度
     * POST /precompute/user/complement?userId=123
     */
    @PostMapping("/user/complement")
    @Operation(summary = "预计算用户互补度", description = "为指定用户预计算与其他用户的技能互补度")
    public BaseResponse<Map<String, Object>> precomputeUserComplement(Long userId) {
        log.info("为用户 {} 预计算互补度", userId);

        try {
            precomputeService.precomputeUserComplement(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("userId", userId);
            result.put("message", "用户互补度预计算完成");

            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("用户互补度预计算失败: userId={}", userId, e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "预计算失败: " + e.getMessage());
        }
    }

    /**
     * 换一批 - 清除当前用户/策略的推荐缓存
     * POST /precompute/refresh?userId=1&strategy=all
     *
     * @param userId 用户ID
     * @param strategy 推荐策略
     * @param preferredTags 偏好标签
     * @return 成功响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "换一批推荐", description = "清除当前用户/策略的推荐缓存，获取新的推荐结果")
    public BaseResponse<Map<String, Object>> refreshRecommendations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "all") String strategy,
            @RequestParam(required = false) List<String> preferredTags) {

        log.info("用户 {} 请求换一批推荐, strategy={}, tags={}", userId, strategy, preferredTags);

        try {
            // 清除当前用户/策略的所有缓存
            String pattern = buildCachePattern(userId, strategy, preferredTags);
            clearCacheByPattern(pattern);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "已清除缓存，将重新计算推荐结果");

            log.info("用户 {} 换一批成功，已清除缓存模式: {}", userId, pattern);

            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("换一批失败: userId={}", userId, e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "换一批失败: " + e.getMessage());
        }
    }

    /**
     * 构建缓存匹配模式
     */
    private String buildCachePattern(Long userId, String strategy, List<String> preferredTags) {
        StringBuilder pattern = new StringBuilder(RedisCacheConstants.RECOMMEND_CACHE_KEY_PREFIX);

        // 用户ID
        pattern.append("userId:").append(userId != null ? userId : "guest");

        // 策略
        pattern.append(":strategy:").append(strategy != null ? strategy : "all");

        // 标签部分不匹配具体标签，只匹配是否有标签
        if (preferredTags != null && !preferredTags.isEmpty()) {
            pattern.append(":tags:*");
        }

        // 匹配所有分页
        pattern.append(":page:*");
        pattern.append(":size:*");

        return pattern.toString();
    }

    /**
     * 按模式清除缓存
     * 使用 keys 命令查找匹配的键并删除
     */
    private void clearCacheByPattern(String pattern) {
        try {
            // 使用 keys 命令查找匹配的键
            // 注意：在生产环境中如果键很多，可能需要使用 scan
            Set<String> keys = (Set<String>) redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("按模式清除缓存: pattern={}, 清除数量={}", pattern, keys.size());
            } else {
                log.info("按模式清除缓存: pattern={}, 没有匹配的键", pattern);
            }
        } catch (Exception e) {
            log.error("按模式清除缓存失败: pattern={}", pattern, e);
        }
    }
}
