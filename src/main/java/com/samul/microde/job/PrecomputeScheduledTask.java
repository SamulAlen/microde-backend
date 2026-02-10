package com.samul.microde.job;

import com.samul.microde.config.ScheduledConfig;
import com.samul.microde.service.CachePreloadService;
import com.samul.microde.service.PrecomputeService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 预计算定时任务
 * 定期执行相似度、互补度和活跃度预计算，保持Redis缓存数据新鲜
 * 所有任务的时间和启用状态均可通过配置文件设置
 * 使用分布式锁防止多实例重复执行
 *
 * @author Samul_Alen
 */
@Component
@Slf4j
public class PrecomputeScheduledTask {

    // 分布式锁的 key 前缀
    private static final String LOCK_KEY_PREFIX = "microde:lock:";

    // 锁的等待时间（毫秒）- 0表示不等待
    private static final long LOCK_WAIT_TIME = 0;

    // 锁的自动释放时间（毫秒）- -1表示使用看门狗机制自动续期
    private static final long LOCK_LEASE_TIME = -1;

    @Resource
    private PrecomputeService precomputeService;

    @Resource
    private ScheduledConfig scheduledConfig;

    @Resource
    private CachePreloadService cachePreloadService;

    @Resource
    private RedissonClient redissonClient;

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

        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + "full:precompute");
        try {
            // 尝试获取锁，使用看门狗机制（leaseTime = -1）
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS)) {
                log.info("成功获取全量预计算任务锁，开始执行...");
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
            } else {
                log.info("全量预计算任务已在其他实例执行，跳过本次执行");
            }
        } catch (InterruptedException e) {
            log.warn("获取全量预计算任务锁时被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            // 只释放自己持有的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放全量预计算任务锁");
            }
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

        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + "incremental:precompute");
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS)) {
                log.info("成功获取增量预计算任务锁，开始执行...");
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
            } else {
                log.info("增量预计算任务已在其他实例执行，跳过本次执行");
            }
        } catch (InterruptedException e) {
            log.warn("获取增量预计算任务锁时被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放增量预计算任务锁");
            }
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

        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + "activity:precompute");
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS)) {
                log.info("成功获取活跃度预计算任务锁，开始执行...");

                try {
                    // 预热活跃用户的活跃度分数
                    // 实际的活跃度计算在 RecommendationService 中进行
                    // 这里可以添加一些预热逻辑，比如预热高频访问用户的活跃度

                    log.info("活跃度预计算任务完成");
                } catch (Exception e) {
                    log.error("活跃度预计算任务执行失败", e);
                }
            } else {
                log.info("活跃度预计算任务已在其他实例执行，跳过本次执行");
            }
        } catch (InterruptedException e) {
            log.warn("获取活跃度预计算任务锁时被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放活跃度预计算任务锁");
            }
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

        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + "cache:cleanup");
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS)) {
                log.info("成功获取缓存清理任务锁，开始执行...");

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
            } else {
                log.info("缓存清理任务已在其他实例执行，跳过本次执行");
            }
        } catch (InterruptedException e) {
            log.warn("获取缓存清理任务锁时被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放缓存清理任务锁");
            }
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

        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + "startup:precompute");
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS)) {
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
            } else {
                log.info("应用启动预计算已在其他实例执行，跳过本次执行");
            }
        } catch (InterruptedException e) {
            log.warn("获取启动预计算任务锁时被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放启动预计算任务锁");
            }
        }
    }
}
