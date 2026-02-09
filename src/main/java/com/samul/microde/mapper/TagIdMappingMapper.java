package com.samul.microde.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samul.microde.model.domain.TagIdMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签ID映射 Mapper
 *
 * @author Samul_Alen
 */
@Mapper
public interface TagIdMappingMapper extends BaseMapper<TagIdMapping> {
}
