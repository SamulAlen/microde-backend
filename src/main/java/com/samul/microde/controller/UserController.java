package com.samul.microde.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.samul.microde.common.BaseResponse;
import com.samul.microde.common.ErrorCode;
import com.samul.microde.common.ResultUtils;
import com.samul.microde.exception.BusinessException;
import com.samul.microde.model.domain.User;
import com.samul.microde.model.dto.RecommendRequest;
import com.samul.microde.model.dto.RecommendationResult;
import com.samul.microde.model.request.UserLoginRequest;
import com.samul.microde.model.request.UserRegisterRequest;
import com.samul.microde.service.RecommendationService;
import com.samul.microde.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.samul.microde.constant.UserConstant.ADMIN_ROLE;

/**
 * 用户接口
 *
 * @author <a href="https://github.com/SamulAlen">程序员艾伦</a>
 */
@Tag(name = "用户管理", description = "用户注册、登录、查询等接口")
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://localhost:3000"})
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RecommendationService recommendationService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册账号")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 校验
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户账号密码登录")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    @Operation(summary = "用户注销", description = "用户退出登录")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户
     *
     * @param request
     * @return
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前登录用户", description = "获取当前登录用户信息")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        User currentUser = userService.getLogininUser(request);
        long userId = currentUser.getId();

        // 尝试从缓存获取当前用户信息
        String cacheKey = "microde:user:current:" + userId;
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        User cachedUser = (User) valueOperations.get(cacheKey);

        if (cachedUser != null) {
            log.info("从Redis缓存获取当前用户信息，userId: {}", userId);
            return ResultUtils.success(cachedUser);
        }

        // 缓存未命中，查询数据库
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);

        // 存入缓存，过期时间30秒
        try {
            valueOperations.set(cacheKey, safetyUser, 30, TimeUnit.SECONDS);
            log.info("当前用户信息已存入Redis，userId: {}", userId);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }

        return ResultUtils.success(safetyUser);
    }


    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "根据用户名搜索用户（管理员权限）")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }

        // 生成缓存key - 根据搜索关键词生成
        String cacheKey = "microde:user:search:" + (StringUtils.isNotBlank(username) ? DigestUtils.md5DigestAsHex(username.getBytes()) : "all");

        // 尝试从缓存获取
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<User> cachedList = (List<User>) valueOperations.get(cacheKey);

        if (cachedList != null) {
            log.info("从Redis缓存获取用户搜索结果，username: {}", username);
            return ResultUtils.success(cachedList);
        }

        // 缓存未命中，查询数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());

        // 存入缓存，过期时间30秒
        try {
            valueOperations.set(cacheKey, list, 30, TimeUnit.SECONDS);
            log.info("用户搜索结果已存入Redis，username: {}", username);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }

        return ResultUtils.success(list);
    }

    /*@GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUserByTags(tagNameList);
        return ResultUtils.success(userList);
    }*/

    @GetMapping("/search/tags")
    @Operation(summary = "按标签搜索用户", description = "根据标签列表分页搜索用户")
    public BaseResponse<Page<User>> searchUsersByTags(long pageSize, long pageNum, @RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 生成缓存key - 根据标签列表和分页参数生成唯一key
        String cacheKey = generateTagSearchCacheKey(tagNameList, pageNum, pageSize);

        // 尝试从缓存获取
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        Page<User> cachedPage = (Page<User>) valueOperations.get(cacheKey);

        if (cachedPage != null) {
            log.info("从Redis缓存获取标签搜索结果，tags: {}, key: {}", tagNameList, cacheKey);
            return ResultUtils.success(cachedPage);
        }

        // 缓存未命中，查询数据库
        Page<User> userList = userService.searchUserByTags(tagNameList);

        // 存入缓存，过期时间30秒
        try {
            valueOperations.set(cacheKey, userList, 30, TimeUnit.SECONDS);
            log.info("标签搜索结果已存入Redis，tags: {}, key: {}", tagNameList, cacheKey);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }

        return ResultUtils.success(userList);
    }

    @GetMapping("/recommend")
    @Operation(summary = "推荐用户", description = "分页获取推荐用户列表")
    public BaseResponse<Page<User>> recommendUsers(@RequestParam(defaultValue = "12") long pageSize, @RequestParam(defaultValue = "1") long pageNum, HttpServletRequest request){
        User logininUser = userService.getLogininUser(request);
        String redisKey = String.format("microde:user:recommend:%s:%s:%s", logininUser.getId(), pageNum, pageSize);
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //如果有缓存，直接读取
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if (userPage != null){
            return ResultUtils.success(userPage);
        }
        //无缓存，查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        //写缓存,30s过期
        try {
            valueOperations.set(redisKey, userPage, 30, TimeUnit.SECONDS);
        } catch (Exception e){
            log.error("redis set key error", e);
        }
        return ResultUtils.success(userPage);
    }

    /**
     * 智能推荐用户
     * 基于标签相似度、技能互补、活跃度等多维度推荐
     *
     * 注意：由于MyBatis-Plus Page对象在Redis序列化/反序列化时存在问题，
     * 智能推荐结果不进行缓存，每次都实时计算。用户可通过"换一批"按钮获取新的推荐。
     *
     * @param request 推荐请求参数
     * @param httpRequest HTTP请求
     * @return 推荐结果分页
     */
    @PostMapping("/recommend/smart")
    @Operation(summary = "智能推荐用户", description = "基于标签相似度、技能互补、活跃度等多维度推荐")
    public BaseResponse<Page<RecommendationResult>> smartRecommend(
            @RequestBody RecommendRequest request,
            HttpServletRequest httpRequest) {
        // 获取当前登录用户
        User loginUser = userService.getLogininUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 设置当前用户ID
        request.setUserId(loginUser.getId());

        log.info("智能推荐请求: userId={}, strategy={}, pageNum={}, pageSize={}",
                loginUser.getId(), request.getStrategy(), request.getPageNum(), request.getPageSize());

        // 直接调用推荐服务（不使用缓存，避免Page对象序列化问题）
        Page<RecommendationResult> resultPage = recommendationService.recommendUsers(request);

        log.info("智能推荐完成: userId={}, 总记录数={}", loginUser.getId(), resultPage.getTotal());

        return ResultUtils.success(resultPage);
    }

    /**
     * 推荐反馈
     * 记录用户对推荐结果的反馈，用于优化推荐算法
     *
     * @param recommendedUserId 被推荐用户ID
     * @param feedback 反馈值 1-喜欢, -1-不感兴趣
     * @param httpRequest HTTP请求
     * @return 是否记录成功
     */
    @PostMapping("/recommend/feedback")
    @Operation(summary = "推荐反馈", description = "记录用户对推荐结果的反馈")
    public BaseResponse<Boolean> recommendFeedback(
            @RequestParam Long recommendedUserId,
            @RequestParam Integer feedback,
            HttpServletRequest httpRequest) {
        // 获取当前登录用户
        User loginUser = userService.getLogininUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 记录反馈
        Boolean result = recommendationService.recordFeedback(loginUser.getId(), recommendedUserId, feedback);

        return ResultUtils.success(result);
    }

    /**
     * 更新用户标签
     *
     * @param tags 标签列表
     * @param httpRequest HTTP请求
     * @return 是否更新成功
     */
    @PostMapping("/tags/update")
    @Operation(summary = "更新用户标签", description = "更新当前用户的标签信息")
    public BaseResponse<Boolean> updateUserTags(
            @RequestBody List<String> tags,
            HttpServletRequest httpRequest) {
        // 获取当前登录用户
        User loginUser = userService.getLogininUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 校验标签数量
        if (tags == null || tags.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
        }
        if (tags.size() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签数量不能超过10个");
        }

        // 校验标签内容
        for (String tag : tags) {
            if (StringUtils.isBlank(tag)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签内容不能为空");
            }
            if (tag.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "单个标签长度不能超过20个字符");
            }
        }

        // 将标签列表转换为JSON字符串
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String tagsJson = gson.toJson(tags);

        // 更新用户标签
        User updateUser = new User();
        updateUser.setId(loginUser.getId());
        updateUser.setTags(tagsJson);

        boolean result = userService.updateById(updateUser);

        // 清除旧推荐缓存（保持兼容性）
        String redisKey = String.format("microde:user:recommend:%s", loginUser.getId());
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("redis delete key error", e);
        }

        // 清除智能推荐缓存
        try {
            redisTemplate.delete(redisTemplate.keys("microde:user:recommend:smart:" + loginUser.getId() + ":*"));
            log.info("已清除用户{}的智能推荐缓存", loginUser.getId());
        } catch (Exception e) {
            log.error("清除智能推荐缓存失败", e);
        }

        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    @Operation(summary = "更新用户信息", description = "更新当前用户信息")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        // 验证参数是否为空
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 鉴权
        User loginUser = userService.getLogininUser(request);
        int result = userService.updateUser(user, loginUser);

        // 清除当前用户缓存
        try {
            String cacheKey = "microde:user:current:" + loginUser.getId();
            redisTemplate.delete(cacheKey);
            log.info("已清除用户{}的当前用户缓存", loginUser.getId());
        } catch (Exception e) {
            log.error("清除当前用户缓存失败", e);
        }

        // 清除用户搜索缓存（因为用户信息可能变化）
        try {
            redisTemplate.delete(redisTemplate.keys("microde:user:search:*"));
            log.info("已清除用户搜索缓存");
        } catch (Exception e) {
            log.error("清除用户搜索缓存失败", e);
        }

        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除用户", description = "删除指定用户（管理员权限）")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);

        // 清除用户搜索缓存
        try {
            redisTemplate.delete(redisTemplate.keys("microde:user:search:*"));
            log.info("已清除用户搜索缓存");
        } catch (Exception e) {
            log.error("清除用户搜索缓存失败", e);
        }

        return ResultUtils.success(b);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request) {
        try {
            User user = userService.getLogininUser(request);
            return user != null && user.getUserRole() == ADMIN_ROLE;
        } catch (BusinessException e) {
            return false;
        }
    }

    /**
     * 生成标签搜索缓存key
     * 根据标签列表和分页参数生成唯一key
     */
    private String generateTagSearchCacheKey(List<String> tagNameList, long pageNum, long pageSize) {
        // 将标签列表排序后转换为字符串，确保相同的标签组合（不同顺序）生成相同的key
        String sortedTags = tagNameList.stream().sorted().reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b);
        String queryParams = String.format("%s_%d_%d", sortedTags, pageNum, pageSize);
        String hash = DigestUtils.md5DigestAsHex(queryParams.getBytes());
        return "microde:user:search:tags:" + hash;
    }

}
