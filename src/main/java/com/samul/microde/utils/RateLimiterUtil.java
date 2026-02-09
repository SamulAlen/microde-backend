package com.samul.microde.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的分布式限流工具
 * 使用令牌桶算法实现请求限流
 *
 * @author Samul_Alen
 */
@Component
@Slf4j
public class RateLimiterUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // Lua脚本：原子性实现令牌桶算法
    private static final String RATE_LIMIT_LUA =
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local expire = tonumber(ARGV[2]) " +
            "local current = tonumber(redis.call('get', key) or '0') " +
            "if current + 1 <= limit then " +
            "    redis.call('incrby', key, 1) " +
            "    redis.call('expire', key, expire) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 检查是否允许请求（令牌桶算法）
     *
     * @param key     限流键（通常为 userId:api 或 ip:api）
     * @param limit   时间窗口内最大请求数
     * @param expire  时间窗口（秒）
     * @return true-允许请求，false-拒绝请求
     */
    public boolean allowRequest(String key, int limit, int expire) {
        try {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(RATE_LIMIT_LUA, Long.class);
            Long result = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(key),
                    String.valueOf(limit),
                    String.valueOf(expire)
            );

            boolean allowed = result != null && result == 1L;
            if (!allowed) {
                log.warn("限流触发: key={}, limit={}, expire={}", key, limit, expire);
            }
            return allowed;
        } catch (Exception e) {
            log.error("限流检查异常，默认放行: key={}", key, e);
            // 异常情况默认放行，避免影响业务
            return true;
        }
    }

    /**
     * 推荐请求限流：10秒内最多3次请求
     *
     * @param userId 用户ID
     * @return true-允许请求，false-使用缓存
     */
    public boolean allowRecommendRequest(Long userId) {
        String key = "rate:limit:recommend:" + userId;
        // 10秒内最多3次请求
        return allowRequest(key, 3, 10);
    }

    /**
     * 获取剩余请求次数
     *
     * @param key    限流键
     * @param limit  限制次数
     * @return 剩余次数
     */
    public int getRemainingRequests(String key, int limit) {
        try {
            String current = (String) redisTemplate.opsForValue().get(key);
            int count = current != null ? Integer.parseInt(current) : 0;
            return Math.max(0, limit - count);
        } catch (Exception e) {
            log.error("获取剩余请求次数失败: key={}", key, e);
            return limit;
        }
    }

    /**
     * 重置限流计数
     *
     * @param key 限流键
     */
    public void resetRateLimit(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("重置限流计数: key={}", key);
        } catch (Exception e) {
            log.error("重置限流计数失败: key={}", key, e);
        }
    }

    /**
     * IP限流：防止恶意请求
     *
     * @param ip       IP地址
     * @param limit    时间窗口内最大请求数
     * @param expire   时间窗口（秒）
     * @return true-允许请求，false-拒绝请求
     */
    public boolean allowIpRequest(String ip, int limit, int expire) {
        String key = "rate:limit:ip:" + ip;
        return allowRequest(key, limit, expire);
    }

    /**
     * 记录请求时间（用于滑动窗口限流）
     *
     * @param key   限流键
     * @param score 时间戳
     */
    public void addRequestTimestamp(String key, long score) {
        try {
            redisTemplate.opsForZSet().add(key, String.valueOf(score), score);
            // 清理过期的请求记录（保留最近1分钟）
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, score - 60000);
        } catch (Exception e) {
            log.error("记录请求时间失败: key={}", key, e);
        }
    }

    /**
     * 获取时间窗口内的请求计数
     *
     * @param key        限流键
     * @param minScore   最小分数（时间戳）
     * @param maxScore   最大分数（时间戳）
     * @return 请求计数
     */
    public long getRequestCount(String key, long minScore, long maxScore) {
        try {
            Long count = redisTemplate.opsForZSet().count(key, minScore, maxScore);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("获取请求计数失败: key={}", key, e);
            return 0;
        }
    }
}
