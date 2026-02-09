package com.samul.microde.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samul.microde.model.domain.UserRecommendFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户推荐反馈 Mapper
 *
 * @author Samul_Alen
 */
@Mapper
public interface UserRecommendFeedbackMapper extends BaseMapper<UserRecommendFeedback> {
}
