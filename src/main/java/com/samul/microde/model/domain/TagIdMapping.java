package com.samul.microde.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 标签ID映射表
 * 用于 BitSet 优化 Jaccard 相似度计算
 *
 * @author Samul_Alen
 */
@Data
@TableName("tag_id_mapping")
public class TagIdMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 标签名称
     */
    @TableField("tag_name")
    private String tagName;

    /**
     * 标签分类
     * LANGUAGE - 编程语言
     * FRAMEWORK - 框架
     * DIRECTION - 技术方向
     * EXPERIENCE - 经验水平
     * STATUS - 状态/目标
     */
    @TableField("tag_category")
    private String tagCategory;

    /**
     * 是否启用
     * 1 - 启用
     * 0 - 禁用
     */
    @TableField("is_active")
    private Integer isActive;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
}
