package com.samul.microde.service.impl;

import com.samul.microde.common.ErrorCode;
import com.samul.microde.exception.BusinessException;
import com.samul.microde.model.domain.User;
import com.samul.microde.service.CachePreloadService;
import com.samul.microde.service.PrecomputeService;
import com.samul.microde.service.TagIdMappingService;
import com.samul.microde.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 预计算服务实现类
 *
 * @author Samul_Alen
 */
@Service
@Slf4j
public class PrecomputeServiceImpl implements PrecomputeService {

    private static final String SIMILARITY_CACHE_KEY_PREFIX = "microde:similarity:";
    private static final String COMPLEMENT_CACHE_KEY_PREFIX = "microde:complement:";
    private static final String ACTIVITY_CACHE_KEY_PREFIX = "microde:activity:";
    private static final String TAG_USERS_CACHE_KEY_PREFIX = "microde:tags:users:";

    // 每个用户保留的最相似/最互补用户数量
    private static final int TOP_USERS_LIMIT = 200;

    // 缓存过期时间 (24小时)
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Resource
    private UserService userService;

    @Resource
    private CachePreloadService cachePreloadService;

    @Resource
    private TagIdMappingService tagIdMappingService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void precomputeSimilarity(boolean forceFullRecompute) {
        log.info("开始预计算用户相似度数据...");

        long startTime = System.currentTimeMillis();
        List<User> allUsers = cachePreloadService.getAllUsersFromCache();

        if (CollectionUtils.isEmpty(allUsers)) {
            log.warn("没有用户数据，跳过相似度预计算");
            return;
        }

        // 获取标签到ID的映射
        Map<String, Integer> tagIdMap = getTagIdMap();
        if (tagIdMap.isEmpty()) {
            log.warn("标签映射为空，跳过相似度预计算");
            return;
        }

        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();

        // 批量计算所有用户的相似度
        int processedCount = 0;
        for (User user : allUsers) {
            String cacheKey = SIMILARITY_CACHE_KEY_PREFIX + user.getId();

            // 如果不是全量重算且缓存已存在，跳过
            if (!forceFullRecompute && Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                continue;
            }

            List<String> userTags = parseTags(user.getTags());
            if (CollectionUtils.isEmpty(userTags)) {
                continue;
            }

            // 计算与其他所有用户的相似度
            Map<Long, Double> similarityMap = new HashMap<>();
            for (User otherUser : allUsers) {
                if (user.getId() == otherUser.getId()) {
                    continue;
                }

                List<String> otherTags = parseTags(otherUser.getTags());
                if (CollectionUtils.isEmpty(otherTags)) {
                    continue;
                }

                double similarity = calculateJaccardSimilarity(userTags, otherTags);
                if (similarity > 0) {
                    similarityMap.put(otherUser.getId(), similarity);
                }
            }

            // 取前N个最相似的用户
            similarityMap.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(TOP_USERS_LIMIT)
                    .forEach(entry -> {
                        zSetOps.add(cacheKey, entry.getKey().toString(), entry.getValue());
                    });

            // 设置过期时间
            redisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            processedCount++;

            // 每100个用户打印一次进度
            if (processedCount % 100 == 0) {
                log.info("相似度预计算进度: {}/{}", processedCount, allUsers.size());
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("相似度预计算完成！处理 {} 个用户，耗时 {} ms", processedCount, endTime - startTime);
    }

    @Override
    public void precomputeComplement(boolean forceFullRecompute) {
        log.info("开始预计算用户互补度数据...");

        long startTime = System.currentTimeMillis();
        List<User> allUsers = cachePreloadService.getAllUsersFromCache();

        if (CollectionUtils.isEmpty(allUsers)) {
            log.warn("没有用户数据，跳过互补度预计算");
            return;
        }

        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        int processedCount = 0;

        for (User user : allUsers) {
            String cacheKey = COMPLEMENT_CACHE_KEY_PREFIX + user.getId();

            // 如果不是全量重算且缓存已存在，跳过
            if (!forceFullRecompute && Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                continue;
            }

            List<String> userTags = parseTags(user.getTags());
            if (CollectionUtils.isEmpty(userTags)) {
                continue;
            }

            // 计算与其他所有用户的互补度
            Map<Long, Double> complementMap = new HashMap<>();
            for (User otherUser : allUsers) {
                if (user.getId() == otherUser.getId()) {
                    continue;
                }

                List<String> otherTags = parseTags(otherUser.getTags());
                if (CollectionUtils.isEmpty(otherTags)) {
                    continue;
                }

                double complementScore = calculateComplementScoreOptimized(userTags, otherTags);
                if (complementScore > 0) {
                    complementMap.put(otherUser.getId(), complementScore);
                }
            }

            // 取前N个最互补的用户
            complementMap.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(TOP_USERS_LIMIT)
                    .forEach(entry -> {
                        zSetOps.add(cacheKey, entry.getKey().toString(), entry.getValue());
                    });

            redisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            processedCount++;

            if (processedCount % 100 == 0) {
                log.info("互补度预计算进度: {}/{}", processedCount, allUsers.size());
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("互补度预计算完成！处理 {} 个用户，耗时 {} ms", processedCount, endTime - startTime);
    }

    @Override
    public Double precomputeActivityScore(Long userId) {
        User user = cachePreloadService.getUserByIdFromCache(userId);
        if (user == null) {
            user = userService.getById(userId);
        }
        if (user == null) {
            return 0.0;
        }

        double score = 0.0;

        // 1. 注册时间 (0.4分)
        long daysSinceCreation = (System.currentTimeMillis() - user.getCreateTime().getTime()) / (1000 * 60 * 60 * 24);
        if (daysSinceCreation < 30) {
            score += 0.4;
        } else if (daysSinceCreation < 90) {
            score += 0.3;
        } else if (daysSinceCreation < 180) {
            score += 0.2;
        } else {
            score += 0.1;
        }

        // 2. 资料完善度 (0.6分)
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            score += 0.15;
        }
        if (user.getTags() != null && !user.getTags().isEmpty()) {
            List<String> tags = parseTags(user.getTags());
            if (!CollectionUtils.isEmpty(tags)) {
                double tagCount = Math.min(tags.size(), 5) * 0.12; // 最多5个标签，每个0.12分
                score += Math.min(tagCount, 0.3);
            }
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            score += 0.1;
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            score += 0.05;
        }

        Double activityScore = Math.min(score, 1.0);

        // 缓存活跃度
        String cacheKey = ACTIVITY_CACHE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(cacheKey, activityScore.toString(), CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return activityScore;
    }

    @Override
    public void precomputeUserSimilarity(Long userId) {
        log.info("为用户 {} 预计算相似度", userId);
        User currentUser = cachePreloadService.getUserByIdFromCache(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        List<User> allUsers = cachePreloadService.getAllUsersFromCache();
        List<String> currentTags = parseTags(currentUser.getTags());

        if (CollectionUtils.isEmpty(currentTags)) {
            log.warn("用户 {} 没有标签，跳过相似度预计算", userId);
            return;
        }

        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        String cacheKey = SIMILARITY_CACHE_KEY_PREFIX + userId;
        redisTemplate.delete(cacheKey);

        Map<Long, Double> similarityMap = new HashMap<>();
        for (User otherUser : allUsers) {
            if (userId == otherUser.getId()) {
                continue;
            }

            List<String> otherTags = parseTags(otherUser.getTags());
            if (CollectionUtils.isEmpty(otherTags)) {
                continue;
            }

            double similarity = calculateJaccardSimilarity(currentTags, otherTags);
            if (similarity > 0) {
                similarityMap.put(otherUser.getId(), similarity);
            }
        }

        // 取前200个
        similarityMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(TOP_USERS_LIMIT)
                .forEach(entry -> {
                    zSetOps.add(cacheKey, entry.getKey().toString(), entry.getValue());
                });

        redisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.info("用户 {} 相似度预计算完成，计算了 {} 个相似用户", userId, similarityMap.size());
    }

    @Override
    public void precomputeUserComplement(Long userId) {
        log.info("为用户 {} 预计算互补度", userId);
        User currentUser = cachePreloadService.getUserByIdFromCache(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        List<User> allUsers = cachePreloadService.getAllUsersFromCache();
        List<String> currentTags = parseTags(currentUser.getTags());

        if (CollectionUtils.isEmpty(currentTags)) {
            log.warn("用户 {} 没有标签，跳过互补度预计算", userId);
            return;
        }

        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        String cacheKey = COMPLEMENT_CACHE_KEY_PREFIX + userId;
        redisTemplate.delete(cacheKey);

        Map<Long, Double> complementMap = new HashMap<>();
        for (User otherUser : allUsers) {
            if (userId == otherUser.getId()) {
                continue;
            }

            List<String> otherTags = parseTags(otherUser.getTags());
            if (CollectionUtils.isEmpty(otherTags)) {
                continue;
            }

            double complementScore = calculateComplementScoreOptimized(currentTags, otherTags);
            if (complementScore > 0) {
                complementMap.put(otherUser.getId(), complementScore);
            }
        }

        complementMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(TOP_USERS_LIMIT)
                .forEach(entry -> {
                    zSetOps.add(cacheKey, entry.getKey().toString(), entry.getValue());
                });

        redisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.info("用户 {} 互补度预计算完成，计算了 {} 个互补用户", userId, complementMap.size());
    }

    @Override
    public List<Long> getTopSimilarUsers(Long userId, int limit) {
        String cacheKey = SIMILARITY_CACHE_KEY_PREFIX + userId;
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();

        Set<Object> results = zSetOps.reverseRange(cacheKey, 0, limit - 1);

        if (CollectionUtils.isEmpty(results)) {
            return new ArrayList<>();
        }

        return results.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getTopComplementUsers(Long userId, int limit) {
        String cacheKey = COMPLEMENT_CACHE_KEY_PREFIX + userId;
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();

        Set<Object> results = zSetOps.reverseRange(cacheKey, 0, limit - 1);

        if (CollectionUtils.isEmpty(results)) {
            return new ArrayList<>();
        }

        return results.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findUsersByTags(List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return new ArrayList<>();
        }

        // 先尝试从缓存获取
        String cacheKey = TAG_USERS_CACHE_KEY_PREFIX + String.join(",", tags);
        List<Long> cachedUsers = (List<Long>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUsers != null) {
            return cachedUsers;
        }

        // 从Redis获取所有用户并筛选
        List<User> allUsers = cachePreloadService.getAllUsersFromCache();
        List<Long> matchedUserIds = new ArrayList<>();

        for (User user : allUsers) {
            List<String> userTags = parseTags(user.getTags());
            if (CollectionUtils.isEmpty(userTags)) {
                continue;
            }

            // 检查是否有交集
            for (String tag : tags) {
                if (userTags.contains(tag)) {
                    matchedUserIds.add(user.getId());
                    break;
                }
            }
        }

        // 缓存结果 (1小时过期)
        redisTemplate.opsForValue().set(cacheKey, matchedUserIds, 1, TimeUnit.HOURS);

        return matchedUserIds;
    }

    @Override
public List<Long> getPrecisionCandidateUsers(Long currentUserId, List<String> preferredTags, int limit) {
        // 第一步：基于用户偏好标签筛选出有交集的用户
        List<Long> candidateIds;

        if (CollectionUtils.isEmpty(preferredTags)) {
            // 没有偏好标签，获取活跃度高的用户
            List<User> allUsers = cachePreloadService.getAllUsersFromCache();
            candidateIds = allUsers.stream()
                    .sorted((u1, u2) -> {
                        Double score1 = getActivityScore(u1.getId());
                        Double score2 = getActivityScore(u2.getId());
                        return score2.compareTo(score1);
                    })
                    .limit(limit * 2)
                    .map(User::getId)
                    .collect(Collectors.toList());
        } else {
            // 有偏好标签，找出有标签交集的用户
            List<Long> tagMatchedUsers = findUsersByTags(preferredTags);

            // 如果标签匹配的用户太少，放宽条件
            if (tagMatchedUsers.size() < limit) {
                List<User> allUsers = cachePreloadService.getAllUsersFromCache();
                candidateIds = allUsers.stream()
                        .filter(u -> u.getId() != currentUserId)
                        .map(User::getId)
                        .collect(Collectors.toList());
            } else {
                candidateIds = tagMatchedUsers;
            }
        }

        // 第二步：从预计算的相似度/互补度数据中获取高得分用户
        Set<Long> topSimilarUsers = new HashSet<>(getTopSimilarUsers(currentUserId, 100));
        Set<Long> topComplementUsers = new HashSet<>(getTopComplementUsers(currentUserId, 100));

        // 优先选择高相似度 + 高互补度的用户
        List<Long> precisionCandidates = new ArrayList<>();
        Set<Long> seen = new HashSet<>(candidateIds);

        // 先添加既在候选集中又高相似/互补的用户
        for (Long userId : candidateIds) {
            if (topSimilarUsers.contains(userId) || topComplementUsers.contains(userId)) {
                precisionCandidates.add(userId);
                seen.add(userId);
            }
        }

        // 如果数量不够，从候选集中补充
        for (Long userId : candidateIds) {
            if (!seen.contains(userId) && precisionCandidates.size() < limit) {
                precisionCandidates.add(userId);
                seen.add(userId);
            }
        }

        return precisionCandidates.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 优化的互补度计算 (基于预定义映射)
     */
    private double calculateComplementScoreOptimized(List<String> myTags, List<String> otherTags) {
        if (CollectionUtils.isEmpty(myTags) || CollectionUtils.isEmpty(otherTags)) {
            return 0.0;
        }

        Set<String> myTagSet = new HashSet<>(myTags);
        double complementScore = 0.0;

        // 技能互补映射表
        Map<String, List<String>> COMPLEMENT_MAP = new HashMap<String, List<String>>() {{
            put("React", Arrays.asList("Spring Boot", "Java", "Go", "Node.js"));
            put("Vue", Arrays.asList("Spring Boot", "Java", "Django", "Flask"));
            put("Spring Boot", Arrays.asList("React", "Vue", "Angular", "iOS", "Android"));
            put("Java", Arrays.asList("React", "Vue", "iOS", "Android", "Flutter"));
            put("Python", Arrays.asList("React", "Vue", "iOS", "Android", "DevOps"));
            put("前端", Arrays.asList("后端", "Java", "Go", "C++"));
            put("后端", Arrays.asList("前端", "React", "Vue", "iOS"));
            put("Android", Arrays.asList("iOS", "后端", "Java"));
            put("iOS", Arrays.asList("Android", "后端", "Swift"));
            put("Flutter", Arrays.asList("后端", "Java", "Go"));
        }};

        // 计算对方标签中有多少是与我互补的
        for (String tag : otherTags) {
            if (COMPLEMENT_MAP.containsKey(tag)) {
                List<String> complementTags = COMPLEMENT_MAP.get(tag);
                for (String myTag : myTags) {
                    if (complementTags.contains(myTag)) {
                        complementScore += 0.5;
                    }
                }
            } else if (!myTagSet.contains(tag)) {
                complementScore += 0.2;
            }
        }

        // 归一化到 0-1
        return Math.min(complementScore / otherTags.size(), 1.0);
    }

    /**
     * 获取标签到ID的映射
     */
    private Map<String, Integer> getTagIdMap() {
        try {
            return tagIdMappingService.getTagIdMap();
        } catch (Exception e) {
            log.error("获取标签映射失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 计算Jaccard相似度
     */
    private double calculateJaccardSimilarity(List<String> tags1, List<String> tags2) {
        if (CollectionUtils.isEmpty(tags1) || CollectionUtils.isEmpty(tags2)) {
            return 0.0;
        }

        Set<String> set1 = new HashSet<>(tags1);
        Set<String> set2 = new HashSet<>(tags2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * 从缓存获取用户的活跃度得分
     */
    private Double getActivityScore(Long userId) {
        String cacheKey = ACTIVITY_CACHE_KEY_PREFIX + userId;
        String score = (String) redisTemplate.opsForValue().get(cacheKey);
        if (score != null) {
            return Double.parseDouble(score);
        }
        return precomputeActivityScore(userId);
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.fromJson(tagsJson, new com.google.gson.reflect.TypeToken<List<String>>() {
            }.getType());
        } catch (Exception e) {
            log.error("解析标签失败: {}", tagsJson, e);
            return new ArrayList<>();
        }
    }
}
