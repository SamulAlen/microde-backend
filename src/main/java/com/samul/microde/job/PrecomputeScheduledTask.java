package com.samul.microde.job;

import com.samul.microde.config.ScheduledConfig;
import com.samul.microde.service.CachePreloadService;
import com.samul.microde.service.PrecomputeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 预计算定时任务
 * 定期执行相似度、互补度和活跃度预计算，保持Redis缓存数据新鲜
 * 所有任务的时间和启用状态均可通过配置文件设置
 *
 * @author Samul_Alen
 */
@Component
@Slf4j
public class PrecomputeScheduledTask {

    @Resource
    private PrecomputeService precomputeService;

    @Resource
    private ScheduledConfig scheduledConfig;

    @Resource
    private CachePreloadService cachePreloadService;

    /**
     * 全量预计算任务
     * 通过 scheduled.tasks.full-precompute-cron 配置执行时间
     * 默认：每天凌晨 2:00:00
     */
    @Scheduled(cron = "${scheduled.tasks.full-precompute-cron:0 0 2 * * ?}")
    public void fullPrecompute() {
        if (!scheduledConfig.getFullPrecomputeEnabled()) {
            log.debug("全量预计算任务已禁用，跳过执行");
            return;
        }

        log.info("开始执行全量预计算任务...");
        long startTime = System.currentTimeMillis();

        try {
            // 全量重算相似度
            precomputeService.precomputeSimilarity(true);
            log.info("全量相似度预计算完成");

            // 全量重算互补度
            precomputeService.precomputeComplement(true);
            log.info("全量互补度预计算完成");

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("全量预计算任务完成！总耗时: {}ms ({}秒)", elapsedTime, elapsedTime / 1000.0);
        } catch (Exception e) {
            log.error("全量预计算任务执行失败", e);
        }
    }

    /**
     * 增量预计算任务
     * 通过 scheduled.tasks.incremental-precompute-cron 配置执行时间
     * 默认：每6小时执行一次
     */
    @Scheduled(cron = "${scheduled.tasks.incremental-precompute-cron:0 0 */6 * * ?}")
    public void incrementalPrecompute() {
        if (!scheduledConfig.getIncrementalPrecomputeEnabled()) {
            log.debug("增量预计算任务已禁用，跳过执行");
            return;
        }

        log.info("开始执行增量预计算任务...");
        long startTime = System.currentTimeMillis();

        try {
            // 增量计算相似度（不强制全量重算）
            precomputeService.precomputeSimilarity(false);
            log.info("增量相似度预计算完成");

            // 增量计算互补度
            precomputeService.precomputeComplement(false);
            log.info("增量互补度预计算完成");

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("增量预计算任务完成！总耗时: {}ms", elapsedTime);
        } catch (Exception e) {
            log.error("增量预计算任务执行失败", e);
        }
    }

    /**
     * 活跃度缓存预热
     * 通过 scheduled.tasks.activity-precompute-cron 配置执行时间
     * 默认：每小时执行一次
     */
    @Scheduled(cron = "${scheduled.tasks.activity-precompute-cron:0 0 * * * ?}")
    public void activityScorePrecompute() {
        if (!scheduledConfig.getActivityPrecomputeEnabled()) {
            log.debug("活跃度预计算任务已禁用，跳过执行");
            return;
        }

        log.info("开始执行活跃度预计算任务...");

        try {
            // 预热活跃用户的活跃度分数
            // 实际的活跃度计算在 RecommendationService 中进行
            // 这里可以添加一些预热逻辑，比如预热高频访问用户的活跃度

            log.info("活跃度预计算任务完成");
        } catch (Exception e) {
            log.error("活跃度预计算任务执行失败", e);
        }
    }

    /**
     * 缓存清理任务
     * 通过 scheduled.tasks.cache-cleanup-cron 配置执行时间
     * 默认：每天凌晨 3:00:00
     */
    @Scheduled(cron = "${scheduled.tasks.cache-cleanup-cron:0 0 3 * * ?}")
    public void cacheCleanup() {
        if (!scheduledConfig.getCacheCleanupEnabled()) {
            log.debug("缓存清理任务已禁用，跳过执行");
            return;
        }

        log.info("开始执行缓存清理任务...");

        try {
            // 清理过期的预计算缓存
            // 删除超过配置时间的缓存数据
            if (cachePreloadService instanceof com.samul.microde.service.impl.CachePreloadServiceImpl) {
                ((com.samul.microde.service.impl.CachePreloadServiceImpl) cachePreloadService)
                    .cleanupExpiredPrecomputeCache();
            }

            log.info("缓存清理任务完成");
        } catch (Exception e) {
            log.error("缓存清理任务执行失败", e);
        }
    }

    /**
     * 应用启动时执行一次增量预计算
     * 确保启动时有可用的推荐数据
     */
    @PostConstruct
    public void initPrecompute() {
        if (!scheduledConfig.getIncrementalPrecomputeEnabled()) {
            log.info("预计算任务已禁用，跳过启动时预计算");
            return;
        }

        log.info("应用启动，开始执行初始预计算...");
        long startTime = System.currentTimeMillis();

        try {
            // 启动时执行增量预计算，避免启动时间过长
            precomputeService.precomputeSimilarity(false);
            precomputeService.precomputeComplement(false);

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("应用启动预计算完成！总耗时: {}ms ({}秒)", elapsedTime, elapsedTime / 1000.0);
        } catch (Exception e) {
            log.error("应用启动预计算失败", e);
        }
    }
}
