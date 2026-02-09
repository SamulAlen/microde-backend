package com.samul.microde.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.samul.microde.common.BaseResponse;
import com.samul.microde.common.ErrorCode;
import com.samul.microde.common.ResultUtils;
import com.samul.microde.exception.BusinessException;
import com.samul.microde.model.domain.Team;
import com.samul.microde.model.domain.User;
import com.samul.microde.model.domain.UserTeam;
import com.samul.microde.enums.TeamStatusEnum;
import com.samul.microde.model.request.UserTeamJoinRequest;
import com.samul.microde.service.TeamService;
import com.samul.microde.service.UserTeamService;
import com.samul.microde.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户队伍关系接口
 *
 * @author Samul_Alen
 */
@Tag(name = "用户队伍关系", description = "用户加入、退出队伍等接口")
@RestController
@RequestMapping("/userTeam")
@CrossOrigin(origins = {"http://localhost:3000"})
@Slf4j
public class UserTeamController {

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    /**
     * 加入队伍
     */
    @PostMapping("/join")
    @Operation(summary = "加入队伍", description = "用户加入指定队伍，支持私有和加密队伍")
    public BaseResponse<Boolean> joinTeam(@RequestBody UserTeamJoinRequest userTeamJoinRequest, HttpServletRequest request) {
        log.info("=== 加入队伍请求开始 ===");
        log.info("请求体对象: {}", userTeamJoinRequest);
        if (userTeamJoinRequest == null) {
            log.error("加入队伍失败：请求参数为空");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long teamId = userTeamJoinRequest.getTeamId();
        String password = userTeamJoinRequest.getPassword();
        log.info("解析参数 - teamId: {}, password(长度:{}): '{}'", teamId, password != null ? password.length() : 0, password);
        if (teamId == null || teamId <= 0) {
            log.error("队伍id不合法: {}", teamId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍id不合法");
        }
        // 获取当前登录用户
        User loginUser = userService.getLogininUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 查询队伍信息
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        log.info("队伍信息 - status: {}, storedPassword: {}", team.getStatus(), team.getPassword());
        // 校验队伍状态
        int status = team.getStatus();
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不合法");
        }
        // 私有队伍不能加入
        if (TeamStatusEnum.PRIVATE.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "私有队伍不能加入");
        }
        // 加密队伍需要校验密码
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍需要输入密码");
            }
            // 去除首尾空格后比较
            String trimmedPassword = password.trim();
            String storedPassword = team.getPassword();
            if (storedPassword != null) {
                storedPassword = storedPassword.trim();
            }
            log.info("密码校验 - input: '{}', stored: '{}'", trimmedPassword, storedPassword);
            if (!trimmedPassword.equals(storedPassword)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 校验队伍是否已满
        long userId = loginUser.getId();
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        long count = userTeamService.count(queryWrapper);
        if (count >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
        }
        // 不能重复加入队伍
        queryWrapper.eq("userId", userId);
        count = userTeamService.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已加入该队伍");
        }
        // 插入用户队伍关系
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new java.util.Date());
        boolean result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 退出队伍
     */
    @PostMapping("/quit")
    @Operation(summary = "退出队伍", description = "用户退出当前所在队伍")
    public BaseResponse<Boolean> quitTeam(@RequestBody UserTeamJoinRequest userTeamJoinRequest, HttpServletRequest request) {
        if (userTeamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = userTeamJoinRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍id不合法");
        }
        // 获取当前登录用户
        User loginUser = userService.getLogininUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = loginUser.getId();
        // 查询队伍信息
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        // 队长不能退出队伍
        if (team.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "队长不能退出队伍");
        }
        // 删除用户队伍关系
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(queryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取队伍成员列表
     */
    @GetMapping("/listMembers/{teamId}")
    @Operation(summary = "获取队伍成员列表", description = "根据队伍ID获取所有成员信息")
    public BaseResponse<List<User>> getTeamMembers(@PathVariable Long teamId, HttpServletRequest request) {
        log.info("=== 获取队伍成员列表开始 ===");
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍id不合法");
        }
        // 获取当前登录用户
        User loginUser = userService.getLogininUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = loginUser.getId();
        log.info("当前用户ID: {}, 队伍ID: {}", userId, teamId);
        // 查询队伍信息
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        log.info("队伍信息 - status: {}, 队长ID: {}", team.getStatus(), team.getUserId());
        // 检查用户是否是队长或已加入队伍
        boolean isLeader = team.getUserId().equals(userId);
        QueryWrapper<UserTeam> checkQuery = new QueryWrapper<>();
        checkQuery.eq("teamId", teamId);
        checkQuery.eq("userId", userId);
        long memberCount = userTeamService.count(checkQuery);
        boolean isMember = memberCount > 0;
        log.info("权限检查 - isLeader: {}, isMember: {}, memberCount: {}", isLeader, isMember, memberCount);
        // 加密队伍：只有队长和已加入的成员才能查看所有成员，其他人只能看到队长
        if (team.getStatus() == TeamStatusEnum.SECRET.getValue() && !isLeader && !isMember) {
            log.info("加密队伍，用户未加入，只返回队长信息");
            User leader = userService.getById(team.getUserId());
            return ResultUtils.success(Collections.singletonList(leader));
        }
        // 公开队伍、私密队伍、或已加入的加密队伍成员：返回所有成员
        log.info("返回所有成员");
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        log.info("user_team表查询结果数量: {}", userTeamList.size());
        List<Long> userIdList = userTeamList.stream()
                .map(UserTeam::getUserId)
                .collect(Collectors.toList());
        log.info("用户ID列表: {}", userIdList);
        List<User> userList = userService.listByIds(userIdList);
        log.info("返回的用户数量: {}", userList.size());
        return ResultUtils.success(userList);
    }

}
