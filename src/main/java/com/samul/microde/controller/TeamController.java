package com.samul.microde.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.samul.microde.common.BaseResponse;
import com.samul.microde.common.ErrorCode;
import com.samul.microde.common.ResultUtils;
import com.samul.microde.exception.BusinessException;
import com.samul.microde.model.domain.Team;
import com.samul.microde.model.domain.User;
import com.samul.microde.model.dto.TeamQuery;
import com.samul.microde.model.request.TeamAddRequest;
import com.samul.microde.service.TeamService;
import com.samul.microde.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户接口
 *
 * @author <a href="https://github.com/SamulAlen">程序员艾伦</a>
 */
@Tag(name = "队伍管理", description = "队伍创建、加入、查询等接口")
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:3000"})
@Slf4j
public class TeamController {

    private static final String TEAM_LIST_CACHE_KEY_PREFIX = "microde:team:list:";

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/add")
    @Operation(summary = "创建队伍", description = "创建新的队伍")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User logininUser = userService.getLogininUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team, logininUser);

        // 清除队伍列表缓存
        clearTeamListCache();

        return ResultUtils.success(teamId);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除队伍", description = "根据ID删除队伍")
    public BaseResponse<Boolean> deleteTeam(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.removeById(id);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }

        // 清除队伍列表缓存
        clearTeamListCache();

        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @Operation(summary = "更新队伍", description = "更新队伍信息")
    public BaseResponse<Boolean> updateTeam(@RequestBody Team team) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.updateById(team);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }

        // 清除队伍列表缓存
        clearTeamListCache();

        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取队伍详情", description = "根据ID获取队伍详细信息")
    public BaseResponse<Team> getTeamById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    @GetMapping("/list")
    @Operation(summary = "获取队伍列表", description = "根据查询条件获取队伍列表")
    public BaseResponse<List<Team>> listTeams(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        List<Team> teamList = teamService.list(queryWrapper);
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/page")
    @Operation(summary = "分页获取队伍列表", description = "分页查询队伍列表，支持缓存")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 生成缓存key - 根据查询条件生成唯一key
        String cacheKey = generateTeamListCacheKey(teamQuery);

        // 尝试从缓存获取
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        Page<Team> cachedPage = (Page<Team>) valueOperations.get(cacheKey);

        if (cachedPage != null) {
            log.info("从Redis缓存获取队伍列表，key: {}", cacheKey);
            return ResultUtils.success(cachedPage);
        }

        // 缓存未命中，查询数据库
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);

        // 存入缓存，过期时间30秒
        valueOperations.set(cacheKey, resultPage, 30, TimeUnit.SECONDS);
        log.info("队伍列表已存入Redis，key: {}, pageSize: {}", cacheKey, resultPage.getRecords().size());

        return ResultUtils.success(resultPage);
    }

    /**
     * 生成队伍列表缓存key
     * 根据查询条件生成唯一key，确保不同查询条件有不同缓存
     */
    private String generateTeamListCacheKey(TeamQuery teamQuery) {
        // 将查询条件转换为字符串并生成hash
        String queryParams = String.format("%d_%d_%s_%s",
                teamQuery.getPageNum(),
                teamQuery.getPageSize(),
                teamQuery.getName() != null ? teamQuery.getName() : "",
                teamQuery.getMaxNum() != null ? teamQuery.getMaxNum() : "");
        String hash = DigestUtils.md5DigestAsHex(queryParams.getBytes());
        return TEAM_LIST_CACHE_KEY_PREFIX + hash;
    }

    /**
     * 清除所有队伍列表缓存
     * 在增删改队伍时调用，确保缓存数据一致性
     */
    private void clearTeamListCache() {
        try {
            // 使用模糊匹配删除所有队伍列表缓存
            redisTemplate.delete(redisTemplate.keys(TEAM_LIST_CACHE_KEY_PREFIX + "*"));
            log.info("已清除所有队伍列表缓存");
        } catch (Exception e) {
            log.error("清除队伍列表缓存失败", e);
        }
    }

}

