package com.samul.microde.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户推荐反馈表
 * 用于记录用户对推荐结果的反馈，优化推荐算法
 *
 * @author Samul_Alen
 */
@Data
@TableName("user_recommend_feedback")
public class UserRecommendFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 被推荐的用户ID
     */
    private Long recommendedUserId;

    /**
     * 反馈值
     * 1 - 喜欢/感兴趣
     * -1 - 不感兴趣/跳过
     */
    private Integer feedback;

    /**
     * 推荐策略
     * all / skill / complement / activity
     */
    private String recommendStrategy;

    /**
     * 推荐时的得分
     */
    private Double recommendScore;

    /**
     * 推荐时的匹配类型
     */
    private String matchType;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
