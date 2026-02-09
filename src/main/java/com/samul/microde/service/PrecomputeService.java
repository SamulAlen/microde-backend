package com.samul.microde.service;

import com.samul.microde.model.domain.User;

import java.util.List;

/**
 * 预计算服务接口
 * 负责离线/准实时计算推荐所需的数据，减少实时请求时的计算量
 *
 * @author Samul_Alen
 */
public interface PrecomputeService {

    /**
     * 预计算所有用户的相似度数据
     * 为每个用户计算与其最相似的200个用户的相似度得分
     *
     * @param forceFullRecompute 是否全量重算，false则只增量计算
     */
    void precomputeSimilarity(boolean forceFullRecompute);

    /**
     * 预计算所有用户的互补度数据
     * 为每个用户计算与其最互补的200个用户的互补度得分
     *
     * @param forceFullRecompute 是否全量重算
     */
    void precomputeComplement(boolean forceFullRecompute);

    /**
     * 预计算单个用户的活跃度得分
     *
     * @param userId 用户ID
     * @return 活跃度得分 (0-1)
     */
    Double precomputeActivityScore(Long userId);

    /**
     * 为指定用户预计算相似度
     *
     * @param userId 用户ID
     */
    void precomputeUserSimilarity(Long userId);

    /**
     * 为指定用户预计算互补度
     *
     * @param userId 用户ID
     */
    void precomputeUserComplement(Long userId);

    /**
     * 获取与用户最相似的用户ID列表 (从Redis缓存)
     *
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 最相似的用户ID列表
     */
    List<Long> getTopSimilarUsers(Long userId, int limit);

    /**
     * 获取与用户最互补的用户ID列表 (从Redis缓存)
     *
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 最互补的用户ID列表
     */
    List<Long> getTopComplementUsers(Long userId, int limit);

    /**
     * 基于标签筛选用户 (精准候选集)
     * 先从Redis中找出有指定标签交集的用户，缩小候选范围
     *
     * @param tags 标签列表
     * @return 符合条件的用户ID列表
     */
    List<Long> findUsersByTags(List<String> tags);

    /**
     * 基于标签和当前用户获取精准候选集
     * 结合用户当前标签，找出有交集的用户
     *
     * @param currentUserId 当前用户ID
     * @param tags 偏好标签
     * @param limit 候选集大小限制
     * @return 候选用户ID列表
     */
    List<Long> getPrecisionCandidateUsers(Long currentUserId, List<String> tags, int limit);
}
