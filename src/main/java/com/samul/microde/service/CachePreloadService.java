package com.samul.microde.service;

import com.samul.microde.model.domain.Team;
import com.samul.microde.model.domain.User;

import java.util.List;

/**
 * 缓存预热服务
 * 应用启动时将所有用户和队伍数据加载到Redis
 *
 * @author Samul_Alen
 */
public interface CachePreloadService {

    /**
     * 预热所有用户数据到Redis
     * 将所有用户存入 Redis Hash: microde:users:all
     */
    void preloadAllUsers();

    /**
     * 预热所有队伍数据到Redis
     * 将所有队伍存入 Redis Hash: microde:teams:all
     */
    void preloadAllTeams();

    /**
     * 从Redis获取所有用户
     */
    List<User> getAllUsersFromCache();

    /**
     * 从Redis获取所有队伍
     */
    List<Team> getAllTeamsFromCache();

    /**
     * 从Redis根据ID获取单个用户
     */
    User getUserByIdFromCache(Long userId);

    /**
     * 从Redis根据ID获取单个队伍
     */
    Team getTeamByIdFromCache(Long teamId);

    /**
     * 同步数据库用户到Redis（定时任务调用）
     */
    void syncUsersToRedis();

    /**
     * 同步数据库队伍到Redis（定时任务调用）
     */
    void syncTeamsToRedis();
}
