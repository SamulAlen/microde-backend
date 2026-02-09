package com.samul.microde.job;

import com.samul.microde.service.CachePreloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 缓存同步定时任务
 * 每2分钟从数据库同步用户和队伍数据到Redis
 *
 * @author Samul_Alen
 */
@Component
@Slf4j
public class CacheSyncJob {

    @Resource
    private CachePreloadService cachePreloadService;

    /**
     * 每2分钟执行一次数据同步
     * fixedRate = 120000 (2分钟)
     */
    @Scheduled(fixedRate = 120000)
    public void syncCache() {
        log.info("开始执行缓存同步任务...");
        try {
            cachePreloadService.syncUsersToRedis();
            cachePreloadService.syncTeamsToRedis();
            log.info("缓存同步任务完成");
        } catch (Exception e) {
            log.error("缓存同步任务执行失败", e);
        }
    }
}
