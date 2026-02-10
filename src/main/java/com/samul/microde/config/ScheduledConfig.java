package com.samul.microde.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Scheduled Tasks Configuration
 * Supports setting execution time and enable status via configuration file
 *
 * @author Samul_Alen
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "scheduled.tasks")
public class ScheduledConfig {

    // ========== Cache Sync Task ==========
    /**
     * Cache sync task interval (milliseconds)
     * Default: 5 minutes (300000ms)
     */
    private Long cacheSyncInterval = 300000L;

    /**
     * Whether to enable cache sync task
     * Default: enabled
     */
    private Boolean cacheSyncEnabled = true;

    // ========== Precompute Tasks ==========
    /**
     * Full precompute task cron expression
     * Default: 2:00:00 AM daily
     */
    private String fullPrecomputeCron = "0 0 2 * * ?";

    /**
     * Incremental precompute task cron expression
     * Default: every 6 hours
     */
    private String incrementalPrecomputeCron = "0 0 */6 * * ?";

    /**
     * Activity precompute task cron expression
     * Default: every hour
     */
    private String activityPrecomputeCron = "0 0 * * * ?";

    /**
     * Cache cleanup task cron expression
     * Default: 3:00:00 AM daily
     */
    private String cacheCleanupCron = "0 0 3 * * ?";

    /**
     * Whether to enable full precompute task
     * Default: enabled
     */
    private Boolean fullPrecomputeEnabled = true;

    /**
     * Whether to enable incremental precompute task
     * Default: enabled
     */
    private Boolean incrementalPrecomputeEnabled = true;

    /**
     * Whether to enable activity precompute task
     * Default: enabled
     */
    private Boolean activityPrecomputeEnabled = true;

    /**
     * Whether to enable cache cleanup task
     * Default: enabled
     */
    private Boolean cacheCleanupEnabled = true;

    // ========== Precompute Parameters ==========
    /**
     * Number of most similar users to keep per user when precomputing similarity
     * Default: 200
     */
    private Integer similarityTopN = 200;

    /**
     * Number of most complementary users to keep per user when precomputing complement
     * Default: 200
     */
    private Integer complementTopN = 200;

    /**
     * Cache expiration time for precomputed data (seconds)
     * Default: 86400 seconds (24 hours)
     */
    private Long precomputeCacheExpireSeconds = 86400L;

    /**
     * Time window for activity score calculation (days)
     * Default: 30 days
     */
    private Integer activityDaysWindow = 30;
}
