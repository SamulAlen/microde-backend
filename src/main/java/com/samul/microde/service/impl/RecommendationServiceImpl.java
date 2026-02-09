package com.samul.microde.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.samul.microde.mapper.UserMapper;
import com.samul.microde.model.domain.User;
import com.samul.microde.model.dto.RecommendRequest;
import com.samul.microde.model.dto.RecommendationResult;
import com.samul.microde.constant.RedisCacheConstants;
import com.samul.microde.service.CachePreloadService;
import com.samul.microde.service.PrecomputeService;
import com.samul.microde.service.RecommendationService;
import com.samul.microde.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 推荐服务实现类
 *
 * @author <a href="https://github.com/SamulAlen">程序员艾伦</a>
 */
@Service
@Slf4j
public class RecommendationServiceImpl extends ServiceImpl<UserMapper, User>
        implements RecommendationService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Resource
    private CachePreloadService cachePreloadService;

    @Resource
    private PrecomputeService precomputeService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final Gson GSON = new Gson();

    // 最大候选用户数量 - 使用预计算后，候选集更精准，数量可以减少
    private static final int MAX_CANDIDATE_USERS = 200;

    // 缓存过期时间（5分钟）
    private static final long CACHE_EXPIRE_MINUTES = 5;

    /**
     * 技能互补映射表
     * 前端技能 <-> 后端技能
     */
    private static final Map<String, List<String>> COMPLEMENT_MAP = new HashMap<String, List<String>>() {{
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

    @Override
    public Page<RecommendationResult> recommendUsers(RecommendRequest request) {
        // 获取当前用户
        Long currentUserId = request.getUserId();
        User currentUser = null;
        if (currentUserId != null) {
            // 优先从Redis获取当前用户
            currentUser = cachePreloadService.getUserByIdFromCache(currentUserId);
            if (currentUser == null) {
                currentUser = userMapper.selectById(currentUserId);
            }
        }

        // 使用精准候选集筛选（优化：从2000用户减少到100-200用户）
        List<Long> candidateUserIds;
        try {
            candidateUserIds = precomputeService.getPrecisionCandidateUsers(
                    currentUserId,
                    request.getPreferredTags(),
                    MAX_CANDIDATE_USERS
            );
            log.info("使用预计算精准候选集筛选，获得 {} 个候选用户", candidateUserIds.size());
        } catch (Exception e) {
            log.warn("获取精准候选集失败，使用降级方案: {}", e.getMessage());
            // 降级方案：从所有用户中随机选择
            List<User> allUsers = cachePreloadService.getAllUsersFromCache();
            if (CollectionUtils.isEmpty(allUsers)) {
                log.warn("Redis中没有用户数据，返回空结果");
                return new Page<>(request.getPageNum(), request.getPageSize(), 0);
            }

            List<User> candidateUsers = new ArrayList<>(allUsers);
            Collections.shuffle(candidateUsers);
            if (candidateUsers.size() > MAX_CANDIDATE_USERS) {
                candidateUsers = candidateUsers.subList(0, MAX_CANDIDATE_USERS);
            }
            candidateUserIds = candidateUsers.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
        }

        // 获取候选用户详情
        List<User> candidateUsers = new ArrayList<>();
        for (Long userId : candidateUserIds) {
            User user = cachePreloadService.getUserByIdFromCache(userId);
            if (user != null && user.getUserStatus() == 0) {
                // 排除当前用户
                if (currentUserId == null || user.getId() != currentUserId) {
                    candidateUsers.add(user);
                }
            }
        }

        // 转换为推荐结果并计算得分
        List<RecommendationResult> allResults = new ArrayList<>();
        for (User user : candidateUsers) {
            // 根据偏好标签筛选（在精准筛选基础上再做二次确认）
            List<String> preferredTags = request.getPreferredTags();
            if (!CollectionUtils.isEmpty(preferredTags)) {
                List<String> userTags = parseTags(user.getTags());
                boolean matchAny = false;
                for (String tag : preferredTags) {
                    if (userTags.contains(tag)) {
                        matchAny = true;
                        break;
                    }
                }
                if (!matchAny) {
                    continue;
                }
            }

            // 计算推荐得分（使用动态权重）
            RecommendationResult result = calculateRecommendationScoreWithDynamicWeight(
                    currentUser, user, request.getStrategy(), currentUserId);

            // 过滤低于最小相似度的结果
            Integer minSimilarity = request.getMinSimilarity();
            if (minSimilarity == null || result.getSimilarity() * 100 >= minSimilarity) {
                allResults.add(result);
            }
        }

        // 根据得分排序
        allResults.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

        // 内存分页
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allResults.size());

        List<RecommendationResult> pageResults = new ArrayList<>();
        if (fromIndex < allResults.size()) {
            pageResults = allResults.subList(fromIndex, toIndex);
        }

        // 构造分页结果
        Page<RecommendationResult> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setRecords(pageResults);
        resultPage.setTotal(allResults.size());
        // 手动设置总页数
        resultPage.setPages((allResults.size() + pageSize - 1) / pageSize);

        log.info("返回分页结果: 当前页={}, 每页大小={}, 总记录数={}, 返回记录数={}",
                pageNum, pageSize, allResults.size(), pageResults.size());

        return resultPage;
    }

    /**
     * 生成缓存键
     * 基于用户ID、策略、偏好标签、分页信息生成唯一键
     */
    private String generateCacheKey(RecommendRequest request) {
        StringBuilder keyBuilder = new StringBuilder(RedisCacheConstants.RECOMMEND_CACHE_KEY_PREFIX);

        // 添加用户ID
        keyBuilder.append("userId:").append(request.getUserId() != null ? request.getUserId() : "guest");

        // 添加策略
        keyBuilder.append(":strategy:").append(request.getStrategy() != null ? request.getStrategy() : "all");

        // 添加偏好标签（排序后确保一致性）
        List<String> preferredTags = request.getPreferredTags();
        if (!CollectionUtils.isEmpty(preferredTags)) {
            List<String> sortedTags = new ArrayList<>(preferredTags);
            Collections.sort(sortedTags);
            keyBuilder.append(":tags:").append(String.join(",", sortedTags));
        }

        // 添加最小相似度
        if (request.getMinSimilarity() != null) {
            keyBuilder.append(":minSim:").append(request.getMinSimilarity());
        }

        // 添加分页信息（这是关键，不同分页应该有不同缓存）
        keyBuilder.append(":page:").append(request.getPageNum());
        keyBuilder.append(":size:").append(request.getPageSize());

        return keyBuilder.toString();
    }

    /**
     * 计算推荐得分并生成推荐结果（旧版，保留兼容）
     */
    private RecommendationResult calculateRecommendationScore(User currentUser, User candidateUser, String strategy) {
        return calculateRecommendationScoreWithDynamicWeight(currentUser, candidateUser, strategy,
                currentUser != null ? currentUser.getId() : null);
    }

    /**
     * 计算推荐得分（使用动态权重调整和预计算数据）
     *
     * @param currentUser     当前用户
     * @param candidateUser   候选用户
     * @param strategy        推荐策略
     * @param currentUserId   当前用户ID
     * @return 推荐结果
     */
    private RecommendationResult calculateRecommendationScoreWithDynamicWeight(
            User currentUser, User candidateUser, String strategy, Long currentUserId) {

        RecommendationResult result = new RecommendationResult();
        result.setUserId(candidateUser.getId());
        result.setUsername(candidateUser.getUsername());
        result.setAvatarUrl(candidateUser.getAvatarUrl());
        result.setTags(parseTags(candidateUser.getTags()));
        result.setProfile(candidateUser.getPlanetCode()); // 使用 planetCode 作为 profile

        List<String> reasons = new ArrayList<>();
        double finalScore = 0.0;

        // 获取当前用户和候选用户的标签
        List<String> currentTags = currentUser != null ? parseTags(currentUser.getTags()) : new ArrayList<>();
        List<String> candidateTags = parseTags(candidateUser.getTags());

        // 动态权重配置
        double similarityWeight = 0.3;   // 默认相似度权重
        double complementWeight = 0.3;    // 默认互补度权重
        double activityWeight = 0.2;      // 默认活跃度权重
        double randomWeight = 0.05;       // 优化后随机因子权重（从20%降到5%）
        double precomputedWeight = 0.15;  // 预计算数据权重

        // 根据策略动态调整权重（核心优化）
        if ("similar".equals(strategy) || "skill".equals(strategy)) {
            // 相似优先策略：提高相似度权重，降低互补度权重
            similarityWeight = 0.50;
            complementWeight = 0.10;
            activityWeight = 0.25;
            randomWeight = 0.05;
            precomputedWeight = 0.10;
            log.debug("使用相似优先策略: 相似度={}%, 互补度={}%, 活跃度={}%, 随机={}%, 预计算={}%",
                    similarityWeight * 100, complementWeight * 100, activityWeight * 100,
                    randomWeight * 100, precomputedWeight * 100);
        } else if ("complement".equals(strategy)) {
            // 互补优先策略：提高互补度权重，降低相似度权重
            similarityWeight = 0.10;
            complementWeight = 0.50;
            activityWeight = 0.25;
            randomWeight = 0.05;
            precomputedWeight = 0.10;
            log.debug("使用互补优先策略: 相似度={}%, 互补度={}%, 活跃度={}%, 随机={}%, 预计算={}%",
                    similarityWeight * 100, complementWeight * 100, activityWeight * 100,
                    randomWeight * 100, precomputedWeight * 100);
        } else if ("activity".equals(strategy)) {
            // 活跃度优先策略
            similarityWeight = 0.15;
            complementWeight = 0.15;
            activityWeight = 0.55;
            randomWeight = 0.05;
            precomputedWeight = 0.10;
        }
        // 默认综合策略保持初始权重

        // 计算各项得分（优先使用预计算数据）
        double similarityScore = 0.0;
        double complementScore = 0.0;
        double activityScore = calculateActivityScore(candidateUser.getId());
        double precomputedScore = 0.0;

        // 尝试从预计算数据获取相似度和互补度
        if (currentUserId != null) {
            try {
                // 获取预计算的相似度排名分数
                List<Long> topSimilar = precomputeService.getTopSimilarUsers(currentUserId, 200);
                int similarRank = topSimilar.indexOf(candidateUser.getId());
                if (similarRank >= 0) {
                    // 排名越前，分数越高：200名是0分，第1名是1分
                    precomputedScore += (200 - similarRank) / 200.0;
                }

                // 获取预计算的互补度排名分数
                List<Long> topComplement = precomputeService.getTopComplementUsers(currentUserId, 200);
                int complementRank = topComplement.indexOf(candidateUser.getId());
                if (complementRank >= 0) {
                    precomputedScore += (200 - complementRank) / 200.0;
                }

                // 预计算分数归一化（两个排名综合）
                precomputedScore = Math.min(precomputedScore / 2, 1.0);

                log.debug("用户 {} 的预计算分数: {}", candidateUser.getId(), precomputedScore);
            } catch (Exception e) {
                log.debug("获取预计算数据失败，使用实时计算: {}", e.getMessage());
            }
        }

        // 如果没有预计算数据，使用实时计算
        if (precomputedScore == 0.0) {
            similarityScore = calculateTagSimilarity(currentTags, candidateTags);
            complementScore = calculateComplementScore(currentTags, candidateTags);
        } else {
            // 有预计算数据时，实时计算作为参考
            similarityScore = calculateTagSimilarity(currentTags, candidateTags);
            complementScore = calculateComplementScore(currentTags, candidateTags);
        }

        // 使用动态权重计算最终得分
        finalScore = similarityScore * similarityWeight
                + complementScore * complementWeight
                + activityScore * activityWeight
                + precomputedScore * precomputedWeight
                + Math.random() * randomWeight;

        // 生成推荐理由
        if (similarityScore > 0.5) {
            reasons.add("你们有相似的技能背景");
        }
        if (complementScore > 0.3) {
            reasons.add("对方技能可以补充你的技术栈");
        }
        if (activityScore > 0.7) {
            reasons.add("该用户活跃度高");
        }
        if (precomputedScore > 0.5) {
            reasons.add("基于大数据的智能匹配");
        }

        // 设置匹配类型
        if ("similar".equals(strategy) || "skill".equals(strategy)) {
            result.setMatchType("相似匹配");
        } else if ("complement".equals(strategy)) {
            result.setMatchType("互补匹配");
        } else if ("activity".equals(strategy)) {
            result.setMatchType("活跃用户");
        } else {
            result.setMatchType("综合匹配");
        }

        result.setSimilarity(Math.min(finalScore, 1.0));
        result.setReasons(reasons.isEmpty() ? Collections.singletonList("系统推荐") : reasons);

        return result;
    }

    @Override
    public Double calculateTagSimilarity(List<String> tags1, List<String> tags2) {
        if (CollectionUtils.isEmpty(tags1) || CollectionUtils.isEmpty(tags2)) {
            return 0.0;
        }

        // Jaccard 相似系数: |A ∩ B| / |A ∪ B|
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

    @Override
    public Double calculateComplementScore(List<String> myTags, List<String> otherTags) {
        if (CollectionUtils.isEmpty(myTags) || CollectionUtils.isEmpty(otherTags)) {
            return 0.0;
        }

        Set<String> myTagSet = new HashSet<>(myTags);
        double complementScore = 0.0;

        // 计算对方标签中有多少是与我互补的
        for (String tag : otherTags) {
            if (COMPLEMENT_MAP.containsKey(tag)) {
                // 检查这个标签是否与我的标签互补
                List<String> complementTags = COMPLEMENT_MAP.get(tag);
                for (String myTag : myTags) {
                    if (complementTags.contains(myTag)) {
                        complementScore += 0.5;
                    }
                }
            } else if (!myTagSet.contains(tag)) {
                // 对方有而我没有的技能，也有一定的互补性
                complementScore += 0.2;
            }
        }

        // 归一化到 0-1
        return Math.min(complementScore / otherTags.size(), 1.0);
    }

    @Override
    public Double calculateActivityScore(Long userId) {
        // 优先从Redis获取用户
        User user = cachePreloadService.getUserByIdFromCache(userId);
        if (user == null) {
            // Redis中没有则从数据库获取
            user = userMapper.selectById(userId);
        }
        if (user == null) {
            return 0.0;
        }

        double score = 0.0;

        // 1. 最近登录时间（假设从创建时间判断活跃度）
        long daysSinceCreation = (System.currentTimeMillis() - user.getCreateTime().getTime()) / (1000 * 60 * 60 * 24);
        if (daysSinceCreation < 30) {
            score += 0.4; // 新用户活跃度高
        } else if (daysSinceCreation < 90) {
            score += 0.3;
        } else {
            score += 0.1;
        }

        // 2. 资料完善度
        if (StringUtils.isNotBlank(user.getAvatarUrl())) {
            score += 0.2;
        }
        if (StringUtils.isNotBlank(user.getTags())) {
            List<String> tags = parseTags(user.getTags());
            if (!CollectionUtils.isEmpty(tags)) {
                score += 0.2;
            }
        }
        if (StringUtils.isNotBlank(user.getEmail())) {
            score += 0.1;
        }
        if (StringUtils.isNotBlank(user.getPhone())) {
            score += 0.1;
        }

        return Math.min(score, 1.0);
    }

    @Override
    public Boolean recordFeedback(Long userId, Long recommendedUserId, Integer feedback) {
        // TODO: 实现反馈记录功能
        // 1. 将反馈存储到数据库（可创建新表 user_recommend_feedback）
        // 2. 根据反馈调整推荐权重
        log.info("记录推荐反馈: userId={}, recommendedUserId={}, feedback={}", userId, recommendedUserId, feedback);
        return true;
    }

    @Override
    public List<String> parseTags(String tagsJson) {
        if (StringUtils.isBlank(tagsJson)) {
            return new ArrayList<>();
        }
        try {
            return GSON.fromJson(tagsJson, new TypeToken<List<String>>() {
            }.getType());
        } catch (Exception e) {
            log.error("解析标签失败: {}", tagsJson, e);
            return new ArrayList<>();
        }
    }
}
