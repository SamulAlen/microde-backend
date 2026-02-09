package com.samul.microde.constant;

/**
 * Redis缓存Key常量类
 * 统一管理项目中共享的Redis缓存Key前缀
 *
 * @author Samul_Alen
 */
public class RedisCacheConstants {

    /**
     * 推荐结果缓存Key前缀
     * 用于: RecommendationServiceImpl, PrecomputeController
     */
    public static final String RECOMMEND_CACHE_KEY_PREFIX = "microde:recommend:";

    private RedisCacheConstants() {
        // 私有构造函数，防止实例化
    }
}
