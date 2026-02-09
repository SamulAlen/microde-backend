package com.samul.microde.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.samul.microde.model.dto.RecommendRequest;
import com.samul.microde.model.dto.RecommendationResult;

import java.util.List;

/**
 * 推荐服务接口
 *
 * @author <a href="https://github.com/SamulAlen">程序员艾伦</a>
 */
public interface RecommendationService {

    /**
     * 智能推荐用户
     *
     * @param request 推荐请求参数
     * @return 推荐结果分页
     */
    Page<RecommendationResult> recommendUsers(RecommendRequest request);

    /**
     * 计算标签相似度（Jaccard 相似系数）
     * Jaccard(A, B) = |A ∩ B| / |A ∪ B|
     *
     * @param tags1 标签列表1
     * @param tags2 标签列表2
     * @return 相似度 0-1
     */
    Double calculateTagSimilarity(List<String> tags1, List<String> tags2);

    /**
     * 计算技能互补度
     * 基于候选用户标签中有多少是当前用户没有的
     *
     * @param myTags    当前用户标签
     * @param otherTags 候选用户标签
     * @return 互补度 0-1
     */
    Double calculateComplementScore(List<String> myTags, List<String> otherTags);

    /**
     * 计算用户活跃度得分
     * 基于最近登录时间、资料完善度等因素
     *
     * @param userId 用户ID
     * @return 活跃度得分 0-1
     */
    Double calculateActivityScore(Long userId);

    /**
     * 记录推荐反馈
     *
     * @param userId           当前用户ID
     * @param recommendedUserId 被推荐用户ID
     * @param feedback         反馈值 1-喜欢, -1-不感兴趣
     * @return 是否记录成功
     */
    Boolean recordFeedback(Long userId, Long recommendedUserId, Integer feedback);

    /**
     * 解析用户标签
     * 从 JSON 字符串解析为标签列表
     *
     * @param tagsJson 标签 JSON 字符串
     * @return 标签列表
     */
    List<String> parseTags(String tagsJson);
}
