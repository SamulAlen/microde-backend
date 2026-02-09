package com.samul.microde.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Spring Session 配置
 * 配置使用JSON序列化，方便在Redis-Insight中查看
 *
 * @author: SamulAlen
 * @date: 2026/02/07
 */
@Configuration
@EnableRedisHttpSession(
    maxInactiveIntervalInSeconds = 86400,  // 24小时
    redisNamespace = "spring:session"
)
public class SpringSessionConfig {

    /**
     * 配置Session使用JSON序列化
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
}
