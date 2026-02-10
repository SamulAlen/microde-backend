package com.samul.microde.job;

import com.samul.microde.config.ScheduledConfig;
import com.samul.microde.service.CachePreloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 缓存同步定时任务
 * 支持通过配置文件设置执行间隔和启用状态
 * 定时从数据库同步用户和队伍数据到Redis
 *
 * @author Samul_Alen
 */
@Component
@Slf4j
public class CacheSyncJob {

    @Resource
    private CachePreloadService cachePreloadService;

    @Resource
    private ScheduledConfig scheduledConfig;

    /**
     * 定时执行数据同步
     * 执行间隔通过 scheduled.tasks.cache-sync-interval 配置（单位：毫秒）
     * 默认：2分钟 (120000ms)
     */
    @Scheduled(fixedRateString = "${scheduled.tasks.cache-sync-interval:120000}")
    public void syncCache() {
        // 检查任务是否启用
        if (!scheduledConfig.getCacheSyncEnabled()) {
            log.debug("缓存同步任务已禁用，跳过执行");
            return;
        }

        log.info("开始执行缓存同步任务...");
        try {
            cachePreloadService.syncUsersToRedis();
            cachePreloadService.syncTeamsToRedis();
            log.info("缓存同步任务完成");
        } catch (Exception e) {
            log.error("缓存同步任务执行失败", e);
        }
    }

    /**
     * 应用启动时执行一次同步
     */
    @PostConstruct
    public void initSync() {
        if (!scheduledConfig.getCacheSyncEnabled()) {
            log.info("缓存同步任务已禁用，跳过启动时同步");
            return;
        }
        log.info("应用启动，开始执行初始缓存同步...");
        syncCache();
    }
}
