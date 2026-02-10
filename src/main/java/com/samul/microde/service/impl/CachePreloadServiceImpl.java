package com.samul.microde.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.samul.microde.config.ScheduledConfig;
import com.samul.microde.constant.RedisCacheConstants;
import com.samul.microde.model.domain.Team;
import com.samul.microde.model.domain.User;
import com.samul.microde.service.CachePreloadService;
import com.samul.microde.service.TeamService;
import com.samul.microde.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热服务实现
 * 负责应用启动时预热数据，以及定期同步数据到Redis
 *
 * @author Samul_Alen
 */
@Service
@Slf4j
public class CachePreloadServiceImpl implements CachePreloadService {

    private static final String ALL_USERS_CACHE_KEY = "microde:users:all";
    private static final String ALL_TEAMS_CACHE_KEY = "microde:teams:all";
    private static final long CACHE_EXPIRE_MINUTES = 10; // 缓存10分钟

    // 分布式锁的 key 前缀
    private static final String LOCK_KEY_PREFIX = "microde:lock:";

    // 锁的等待时间（毫秒）- 0表示不等待
    private static final long LOCK_WAIT_TIME = 0;

    // 锁的自动释放时间（毫秒）- -1表示使用看门狗机制自动续期
    private static final long LOCK_LEASE_TIME = -1;

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ScheduledConfig scheduledConfig;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 应用启动时自动执行预热
     */
    @PostConstruct
    public void init() {
        log.info("开始缓存预热...");
        try {
            preloadAllUsers();
            preloadAllTeams();
            log.info("缓存预热完成！");
        } catch (Exception e) {
            log.error("缓存预热失败", e);
        }
    }

    /**
     * 定时执行数据同步
     * 执行间隔通过 scheduled.tasks.cache-sync-interval 配置（单位：毫秒）
     * 默认：5分钟 (300000ms)
     * 使用分布式锁防止多实例同时执行
     */
    @Scheduled(fixedRateString = "${scheduled.tasks.cache-sync-interval:300000}")
    public void scheduledSync() {
        // 检查任务是否启用
        if (scheduledConfig != null && !scheduledConfig.getCacheSyncEnabled()) {
            log.debug("缓存同步任务已禁用，跳过执行");
            return;
        }

        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + "cache:sync");
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS)) {
                log.info("成功获取缓存同步锁，开始执行定时缓存同步任务...");
                try {
                    syncUsersToRedis();
                    syncTeamsToRedis();
                    log.info("定时缓存同步任务完成");
                } catch (Exception e) {
                    log.error("定时缓存同步任务执行失败", e);
                }
            } else {
                log.info("缓存同步任务已在其他实例执行，跳过本次执行");
            }
        } catch (InterruptedException e) {
            log.warn("获取缓存同步锁时被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放缓存同步锁");
            }
        }
    }

    @Override
    public void preloadAllUsers() {
        log.info("开始预热用户数据到Redis...");
        long startTime = System.currentTimeMillis();

        // 查询所有正常状态的用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userStatus", 0);
        List<User> allUsers = userService.list(queryWrapper);

        if (allUsers.isEmpty()) {
            log.warn("数据库中没有用户数据");
            return;
        }

        // 使用 Redis Hash 存储所有用户
        // key: microde:users:all
        // field: userId, value: User对象
        redisTemplate.delete(ALL_USERS_CACHE_KEY);

        for (User user : allUsers) {
            redisTemplate.opsForHash().put(ALL_USERS_CACHE_KEY, String.valueOf(user.getId()), user);
        }

        // 设置过期时间
        redisTemplate.expire(ALL_USERS_CACHE_KEY, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        log.info("用户数据预热完成！共加载 {} 个用户，耗时 {} ms", allUsers.size(), endTime - startTime);
    }

    @Override
    public void preloadAllTeams() {
        log.info("开始预热队伍数据到Redis...");
        long startTime = System.currentTimeMillis();

        // 查询所有队伍
        List<Team> allTeams = teamService.list();

        if (allTeams.isEmpty()) {
            log.warn("数据库中没有队伍数据");
            return;
        }

        // 使用 Redis Hash 存储所有队伍
        // key: microde:teams:all
        // field: teamId, value: Team对象
        redisTemplate.delete(ALL_TEAMS_CACHE_KEY);

        for (Team team : allTeams) {
            redisTemplate.opsForHash().put(ALL_TEAMS_CACHE_KEY, team.getId().toString(), team);
        }

        // 设置过期时间
        redisTemplate.expire(ALL_TEAMS_CACHE_KEY, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        log.info("队伍数据预热完成！共加载 {} 个队伍，耗时 {} ms", allTeams.size(), endTime - startTime);
    }

    @Override
    public List<User> getAllUsersFromCache() {
        try {
            Map<Object, Object> userMap = redisTemplate.opsForHash().entries(ALL_USERS_CACHE_KEY);

            if (userMap.isEmpty()) {
                log.warn("Redis中没有用户缓存，尝试从数据库加载...");
                syncUsersToRedis();
                userMap = redisTemplate.opsForHash().entries(ALL_USERS_CACHE_KEY);
            }

            List<User> userList = new ArrayList<>();
            for (Object value : userMap.values()) {
                if (value instanceof User) {
                    userList.add((User) value);
                }
            }

            log.debug("从Redis获取到 {} 个用户", userList.size());
            return userList;
        } catch (Exception e) {
            log.error("从Redis获取用户失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Team> getAllTeamsFromCache() {
        try {
            Map<Object, Object> teamMap = redisTemplate.opsForHash().entries(ALL_TEAMS_CACHE_KEY);

            if (teamMap.isEmpty()) {
                log.warn("Redis中没有队伍缓存，尝试从数据库加载...");
                syncTeamsToRedis();
                teamMap = redisTemplate.opsForHash().entries(ALL_TEAMS_CACHE_KEY);
            }

            List<Team> teamList = new ArrayList<>();
            for (Object value : teamMap.values()) {
                if (value instanceof Team) {
                    teamList.add((Team) value);
                }
            }

            log.debug("从Redis获取到 {} 个队伍", teamList.size());
            return teamList;
        } catch (Exception e) {
            log.error("从Redis获取队伍失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public User getUserByIdFromCache(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            Object userObj = redisTemplate.opsForHash().get(ALL_USERS_CACHE_KEY, userId.toString());
            if (userObj instanceof User) {
                return (User) userObj;
            }
        } catch (Exception e) {
            log.error("从Redis获取用户失败，userId: {}", userId, e);
        }
        return null;
    }

    @Override
    public Team getTeamByIdFromCache(Long teamId) {
        if (teamId == null) {
            return null;
        }
        try {
            Object teamObj = redisTemplate.opsForHash().get(ALL_TEAMS_CACHE_KEY, teamId.toString());
            if (teamObj instanceof Team) {
                return (Team) teamObj;
            }
        } catch (Exception e) {
            log.error("从Redis获取队伍失败，teamId: {}", teamId, e);
        }
        return null;
    }

    @Override
    public void syncUsersToRedis() {
        log.info("开始同步数据库用户到Redis...");
        preloadAllUsers();
    }

    @Override
    public void syncTeamsToRedis() {
        log.info("开始同步数据库队伍到Redis...");
        preloadAllTeams();
    }

    /**
     * 清理过期的预计算缓存
     * 删除超过配置时间的缓存数据
     */
    public void cleanupExpiredPrecomputeCache() {
        try {
            int deletedCount = 0;

            // 1. 清理推荐结果缓存 (microde:recommend:*)
            String recommendPattern = RedisCacheConstants.RECOMMEND_CACHE_KEY_PREFIX + "*";
            Set<String> recommendKeys = redisTemplate.keys(recommendPattern);
            if (recommendKeys != null && !recommendKeys.isEmpty()) {
                long count = redisTemplate.delete(recommendKeys);
                deletedCount += count;
                log.info("清理推荐结果缓存: {} 个", count);
            }

            // 2. 清理用户相似度缓存 (microde:similarity:*)
            String similarityPattern = "microde:similarity:*";
            Set<String> similarityKeys = redisTemplate.keys(similarityPattern);
            if (similarityKeys != null && !similarityKeys.isEmpty()) {
                long count = redisTemplate.delete(similarityKeys);
                deletedCount += count;
                log.info("清理用户相似度缓存: {} 个", count);
            }

            // 3. 清理用户互补度缓存 (microde:complement:*)
            String complementPattern = "microde:complement:*";
            Set<String> complementKeys = redisTemplate.keys(complementPattern);
            if (complementKeys != null && !complementKeys.isEmpty()) {
                long count = redisTemplate.delete(complementKeys);
                deletedCount += count;
                log.info("清理用户互补度缓存: {} 个", count);
            }

            // 4. 清理用户搜索缓存 (microde:user:search:*)
            String searchPattern = "microde:user:search:*";
            Set<String> searchKeys = redisTemplate.keys(searchPattern);
            if (searchKeys != null && !searchKeys.isEmpty()) {
                long count = redisTemplate.delete(searchKeys);
                deletedCount += count;
                log.info("清理用户搜索缓存: {} 个", count);
            }

            // 5. 清理当前用户缓存 (microde:user:current:*) - 这些会在下次登录时重建
            String currentPattern = "microde:user:current:*";
            Set<String> currentKeys = redisTemplate.keys(currentPattern);
            if (currentKeys != null && !currentKeys.isEmpty()) {
                long count = redisTemplate.delete(currentKeys);
                deletedCount += count;
                log.info("清理当前用户缓存: {} 个", count);
            }

            log.info("预计算缓存清理完成，共清理 {} 个缓存键", deletedCount);
        } catch (Exception e) {
            log.error("清理预计算缓存失败", e);
        }
    }
}
