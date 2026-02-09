package com.samul.microde.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 *
 *  自定义序列化
 *
 */
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建RedisTemplate对象
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置连接工厂
        redisTemplate.setConnectionFactory(connectionFactory);

        // 创建Json序列化工具
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();

        // 设置Key的序列化 - 使用String序列化
        redisTemplate.setKeySerializer(RedisSerializer.string());
        // 设置Value的序列化 - 使用JSON序列化
        redisTemplate.setValueSerializer(jsonRedisSerializer);

        // 设置Hash Key的序列化 - 使用String序列化
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        // 设置Hash Value的序列化 - 使用JSON序列化
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);

        return redisTemplate;
    }
}