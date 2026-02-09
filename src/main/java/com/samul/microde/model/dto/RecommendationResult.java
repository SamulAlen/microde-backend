package com.samul.microde.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 推荐结果
 *
 * @author <a href="https://github.com/SamulAlen">程序员艾伦</a>
 */
@Data
public class RecommendationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 用户标签列表
     */
    private List<String> tags;

    /**
     * 个人简介
     */
    private String profile;

    /**
     * 相似度得分 (0-1)
     */
    private Double similarity;

    /**
     * 推荐理由列表
     */
    private List<String> reasons;

    /**
     * 匹配类型：similar-相似, complement-互补, activity-活跃
     */
    private String matchType;
}
