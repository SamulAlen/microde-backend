package com.samul.microde.once;

import com.samul.microde.service.PrecomputeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 预计算定时任务
 * 定期执行相似度和互补度预计算，保持Redis缓存数据新鲜
 *
 * @author Samul_Alen
 */
@Component
@Slf4j
public class PrecomputeScheduledTask {

    @Resource
    private PrecomputeService precomputeService;

    /**
     * 全量预计算任务
     * 每天中午12点50分执行一次
     */
    @Scheduled(cron = "0 50 12 * * ?")
    public void fullPrecompute() {
        log.info("开始执行全量预计算任务...");
        long startTime = System.currentTimeMillis();

        try {
            // 全量重算相似度
            precomputeService.precomputeSimilarity(true);

            // 全量重算互补度
            precomputeService.precomputeComplement(true);

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("全量预计算任务完成！总耗时: {}ms ({}秒)", elapsedTime, elapsedTime / 1000.0);
        } catch (Exception e) {
            log.error("全量预计算任务执行失败", e);
        }
    }

    /**
     * 增量预计算任务
     * 每6小时执行一次，只计算缓存过期或不存在的用户
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void incrementalPrecompute() {
        log.info("开始执行增量预计算任务...");
        long startTime = System.currentTimeMillis();

        try {
            // 增量计算相似度（不强制全量重算）
            precomputeService.precomputeSimilarity(false);

            // 增量计算互补度
            precomputeService.precomputeComplement(false);

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("增量预计算任务完成！总耗时: {}ms", elapsedTime);
        } catch (Exception e) {
            log.error("增量预计算任务执行失败", e);
        }
    }

    /**
     * 活跃度缓存预热
     * 每小时执行一次，预热活跃用户的活跃度分数
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void activityScorePrecompute() {
        log.info("开始执行活跃度预计算任务...");

        try {
            // 这里可以添加活跃度预计算的逻辑
            // 例如：获取最近活跃的用户列表，预热他们的活跃度分数

            log.info("活跃度预计算任务完成");
        } catch (Exception e) {
            log.error("活跃度预计算任务执行失败", e);
        }
    }

    /**
     * 缓存清理任务
     * 每天中午13点00分执行一次，清理过期的缓存数据
     */
    @Scheduled(cron = "0 0 13 * * ?")
    public void cacheCleanup() {
        log.info("开始执行缓存清理任务...");

        try {
            // 这里可以添加缓存清理逻辑
            // 例如：删除超过30天未活跃用户的预计算数据

            log.info("缓存清理任务完成");
        } catch (Exception e) {
            log.error("缓存清理任务执行失败", e);
        }
    }
}
