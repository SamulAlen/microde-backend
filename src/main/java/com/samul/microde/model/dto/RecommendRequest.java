package com.samul.microde.model.dto;

import com.samul.microde.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 智能推荐请求参数
 *
 * @author <a href="https://github.com/SamulAlen">程序员艾伦</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RecommendRequest extends PageRequest {

    /**
     * 当前用户ID（可选，不传则从登录态获取）
     */
    private Long userId;

    /**
     * 推荐策略：all-综合推荐, skill-技能相似, complement-技能互补, activity-活跃用户
     */
    private String strategy;

    /**
     * 用户偏好的标签筛选
     */
    private List<String> preferredTags;

    /**
     * 最小相似度阈值 (0-100)
     */
    private Integer minSimilarity;
}
