package com.samul.microde.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.samul.microde.model.domain.User;
import com.samul.microde.model.dto.RecommendRequest;
import com.samul.microde.model.dto.RecommendationResult;
import com.samul.microde.service.CachePreloadService;
import com.samul.microde.service.RecommendationFallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐降级服务实现
 * 当主推荐服务失败时提供轻量级的降级方案
 *
 * @author Samul_Alen
 */
@Service
@Slf4j
public class RecommendationFallbackServiceImpl implements RecommendationFallbackService {

    @Resource
    private CachePreloadService cachePreloadService;

    /**
     * 轻量级推荐：只返回活跃度最高的用户
     * 不进行复杂的相似度/互补度计算
     *
     * @param request 推荐请求
     * @return 轻量级推荐结果
     */
    @Override
    public Page<RecommendationResult> lightweightRecommend(RecommendRequest request) {
        log.info("执行轻量级推荐降级: userId={}, strategy={}",
                request.getUserId(), request.getStrategy());

        long startTime = System.currentTimeMillis();

        // 从缓存获取所有用户
        List<User> allUsers = cachePreloadService.getAllUsersFromCache();

        if (CollectionUtils.isEmpty(allUsers)) {
            log.warn("缓存中没有用户数据，返回空结果");
            return createEmptyPage(request);
        }

        // 排除当前用户和非正常用户
        Long currentUserId = request.getUserId();
        List<User> candidateUsers = allUsers.stream()
                .filter(user -> user.getUserStatus() == 0) // 只选正常用户
                .filter(user -> currentUserId == null || user.getId() != currentUserId)
                .collect(Collectors.toList());

        // 计算活跃度得分并排序
        List<RecommendationResult> results = candidateUsers.stream()
                .limit(100) // 降级方案只计算前100个
                .map(this::createLightweightResult)
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .collect(Collectors.toList());

        // 分页
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, results.size());

        List<RecommendationResult> pageResults = new ArrayList<>();
        if (fromIndex < results.size()) {
            pageResults = results.subList(fromIndex, toIndex);
        }

        Page<RecommendationResult> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setRecords(pageResults);
        resultPage.setTotal(results.size());
        resultPage.setPages((results.size() + pageSize - 1) / pageSize);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("轻量级推荐完成: 耗时={}ms, 结果数={}", elapsedTime, pageResults.size());

        return resultPage;
    }

    /**
     * 随机推荐降级：返回随机用户
     *
     * @param request 推荐请求
     * @return 随机推荐结果
     */
    @Override
    public Page<RecommendationResult> randomRecommend(RecommendRequest request) {
        log.info("执行随机推荐降级: userId={}", request.getUserId());

        List<User> allUsers = cachePreloadService.getAllUsersFromCache();

        if (CollectionUtils.isEmpty(allUsers)) {
            return createEmptyPage(request);
        }

        Long currentUserId = request.getUserId();
        List<User> candidateUsers = allUsers.stream()
                .filter(user -> user.getUserStatus() == 0)
                .filter(user -> currentUserId == null || user.getId() != currentUserId)
                .collect(Collectors.toList());

        // 随机打乱
        Collections.shuffle(candidateUsers);

        // 取前50个
        List<RecommendationResult> results = candidateUsers.stream()
                .limit(50)
                .map(user -> {
                    RecommendationResult result = new RecommendationResult();
                    result.setUserId(user.getId());
                    result.setUsername(user.getUsername());
                    result.setAvatarUrl(user.getAvatarUrl());
                    result.setTags(parseTags(user.getTags()));
                    result.setProfile(user.getPlanetCode());
                    result.setSimilarity(0.5); // 固定分数
                    result.setMatchType("随机推荐");
                    result.setReasons(Collections.singletonList("系统推荐"));
                    return result;
                })
                .collect(Collectors.toList());

        // 分页
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, results.size());

        List<RecommendationResult> pageResults = new ArrayList<>();
        if (fromIndex < results.size()) {
            pageResults = results.subList(fromIndex, toIndex);
        }

        Page<RecommendationResult> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setRecords(pageResults);
        resultPage.setTotal(results.size());
        resultPage.setPages((results.size() + pageSize - 1) / pageSize);

        return resultPage;
    }

    /**
     * 基于标签的简单推荐
     *
     * @param request 推荐请求
     * @return 基于标签的推荐结果
     */
    @Override
    public Page<RecommendationResult> tagBasedRecommend(RecommendRequest request) {
        log.info("执行基于标签的推荐降级: userId={}, tags={}",
                request.getUserId(), request.getPreferredTags());

        List<User> allUsers = cachePreloadService.getAllUsersFromCache();

        if (CollectionUtils.isEmpty(allUsers)) {
            return createEmptyPage(request);
        }

        Long currentUserId = request.getUserId();
        List<String> preferredTags = request.getPreferredTags();

        List<User> candidateUsers = allUsers.stream()
                .filter(user -> user.getUserStatus() == 0)
                .filter(user -> currentUserId == null || user.getId() != currentUserId)
                .collect(Collectors.toList());

        // 如果有偏好标签，优先选择有交集的用户
        List<RecommendationResult> results = new ArrayList<>();

        if (!CollectionUtils.isEmpty(preferredTags)) {
            // 先添加标签匹配的用户
            for (User user : candidateUsers) {
                List<String> userTags = parseTags(user.getTags());
                for (String tag : preferredTags) {
                    if (userTags.contains(tag)) {
                        RecommendationResult result = createLightweightResult(user);
                        result.setSimilarity(0.8); // 标签匹配的用户给高分
                        result.setMatchType("标签匹配");
                        result.setReasons(Collections.singletonList("与您有相同的标签"));
                        results.add(result);
                        break;
                    }
                }
                if (results.size() >= 50) break;
            }
        }

        // 如果结果不够，补充其他用户
        if (results.size() < 20) {
            for (User user : candidateUsers) {
                if (results.stream().noneMatch(r -> r.getUserId().equals(user.getId()))) {
                    results.add(createLightweightResult(user));
                    if (results.size() >= 20) break;
                }
            }
        }

        // 分页
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, results.size());

        List<RecommendationResult> pageResults = new ArrayList<>();
        if (fromIndex < results.size()) {
            pageResults = results.subList(fromIndex, toIndex);
        }

        Page<RecommendationResult> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setRecords(pageResults);
        resultPage.setTotal(results.size());
        resultPage.setPages((results.size() + pageSize - 1) / pageSize);

        return resultPage;
    }

    /**
     * 创建轻量级推荐结果
     * 只计算活跃度，不计算相似度和互补度
     */
    private RecommendationResult createLightweightResult(User user) {
        RecommendationResult result = new RecommendationResult();
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setAvatarUrl(user.getAvatarUrl());
        result.setTags(parseTags(user.getTags()));
        result.setProfile(user.getPlanetCode());

        // 只计算活跃度得分
        double activityScore = calculateLightweightActivityScore(user);
        result.setSimilarity(activityScore);
        result.setMatchType("活跃用户");
        result.setReasons(Collections.singletonList("该用户活跃度高"));

        return result;
    }

    /**
     * 轻量级活跃度计算
     * 简化版的活跃度计算，只考虑关键因素
     */
    private double calculateLightweightActivityScore(User user) {
        double score = 0.0;

        // 注册时间（0-0.4分）
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

        // 头像（0-0.3分）
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            score += 0.3;
        }

        // 标签（0-0.3分）
        if (user.getTags() != null && !user.getTags().isEmpty()) {
            score += 0.3;
        }

        return Math.min(score, 1.0);
    }

    /**
     * 创建空分页结果
     */
    private Page<RecommendationResult> createEmptyPage(RecommendRequest request) {
        Page<RecommendationResult> emptyPage = new Page<>(request.getPageNum(), request.getPageSize());
        emptyPage.setRecords(new ArrayList<>());
        emptyPage.setTotal(0);
        emptyPage.setPages(0);
        return emptyPage;
    }

    /**
     * 解析标签JSON
     */
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
