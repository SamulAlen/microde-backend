package com.samul.microde.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置类
 * 用于推荐系统的异步计算
 *
 * @author Samul_Alen
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * 推荐计算专用线程池
     * 核心线程数根据CPU核心数配置，最大线程数为核心数的2倍
     */
    @Bean("recommendExecutor")
    public Executor recommendExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：CPU核心数
        executor.setCorePoolSize(cores);

        // 最大线程数：CPU核心数的2倍
        executor.setMaxPoolSize(cores * 2);

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程名称前缀
        executor.setThreadNamePrefix("recommend-async-");

        // 拒绝策略：调用者运行策略，确保任务不会丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 线程空闲时间
        executor.setKeepAliveSeconds(60);

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("推荐异步线程池初始化完成: 核心线程数={}, 最大线程数={}", cores, cores * 2);

        return executor;
    }
}
