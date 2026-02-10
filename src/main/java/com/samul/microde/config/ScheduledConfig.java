package com.samul.microde.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 定时任务配置
 * 支持通过配置文件设置所有定时任务的执行时间和启用状态
 *
 * @author Samul_Alen
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "scheduled.tasks")
public class ScheduledConfig {

    // ========== 缓存同步任务 ==========
    /**
     * 缓存同步任务执行间隔（毫秒）
     * 默认：5分钟 (300000ms)
     */
    private Long cacheSyncInterval = 300000L;

    /**
     * 是否启用缓存同步任务
     * 默认：启用
     */
    private Boolean cacheSyncEnabled = true;

    // ========== 预计算任务 ==========
    /**
     * 全量预计算任务 cron 表达式
     * 默认：每天凌晨 2:00:00 执行
     */
    private String fullPrecomputeCron = "0 0 2 * * ?";

    /**
     * 增量预计算任务 cron 表达式
     * 默认：每6小时执行一次
     */
    private String incrementalPrecomputeCron = "0 0 */6 * * ?";

    /**
     * 活跃度预计算任务 cron 表达式
     * 默认：每小时执行一次
     */
    private String activityPrecomputeCron = "0 0 * * * ?";

    /**
     * 缓存清理任务 cron 表达式
     * 默认：每天凌晨 3:00:00 执行
     */
    private String cacheCleanupCron = "0 0 3 * * ?";

    /**
     * 是否启用全量预计算任务
     * 默认：启用
     */
    private Boolean fullPrecomputeEnabled = true;

    /**
     * 是否启用增量预计算任务
     * 默认：启用
     */
    private Boolean incrementalPrecomputeEnabled = true;

    /**
     * 是否启用活跃度预计算任务
     * 默认：启用
     */
    private Boolean activityPrecomputeEnabled = true;

    /**
     * 是否启用缓存清理任务
     * 默认：启用
     */
    private Boolean cacheCleanupEnabled = true;

    // ========== 预计算参数配置 ==========
    /**
     * 预计算相似度时，每个用户保留的最相似用户数量
     * 默认：200
     */
    private Integer similarityTopN = 200;

    /**
     * 预计算互补度时，每个用户保留的最互补用户数量
     * 默认：200
     */
    private Integer complementTopN = 200;

    /**
     * 预计算时的缓存过期时间（秒）
     * 默认：86400秒 (24小时)
     */
    private Long precomputeCacheExpireSeconds = 86400L;

    /**
     * 活跃度计算的时间窗口（天数）
     * 默认：30天
     */
    private Integer activityDaysWindow = 30;
}
