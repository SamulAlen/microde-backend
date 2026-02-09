package com.samul.microde.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.samul.microde.model.domain.Team;
import com.samul.microde.model.domain.User;
import com.samul.microde.service.CachePreloadService;
import com.samul.microde.service.TeamService;
import com.samul.microde.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热服务实现
 *
 * @author Samul_Alen
 */
@Service
@Slf4j
public class CachePreloadServiceImpl implements CachePreloadService {

    private static final String ALL_USERS_CACHE_KEY = "microde:users:all";
    private static final String ALL_TEAMS_CACHE_KEY = "microde:teams:all";
    private static final long CACHE_EXPIRE_MINUTES = 10; // 缓存10分钟

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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
}
