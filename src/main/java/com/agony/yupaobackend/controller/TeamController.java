package com.agony.yupaobackend.controller;

import com.agony.yupaobackend.common.BaseResponse;
import com.agony.yupaobackend.common.ErrorCode;
import com.agony.yupaobackend.common.ResultUtils;
import com.agony.yupaobackend.exception.BusinessException;
import com.agony.yupaobackend.pojo.domain.Team;
import com.agony.yupaobackend.pojo.domain.User;
import com.agony.yupaobackend.pojo.domain.UserTeam;
import com.agony.yupaobackend.pojo.dto.TeamQuery;
import com.agony.yupaobackend.pojo.request.*;
import com.agony.yupaobackend.pojo.vo.TeamUserVO;
import com.agony.yupaobackend.service.TeamService;
import com.agony.yupaobackend.service.UserService;
import com.agony.yupaobackend.service.UserTeamService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author Agony
 * @Create 2023/11/27 20:49
 * @Version 1.0
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(value = {"http://localhost:5173/"}, allowCredentials = "true")
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;


    @Autowired
    private TeamService teamService;

    @Autowired
    private UserTeamService userTeamService;

    /**
     * 添加队伍
     *
     * @param teamAddRequest 队伍
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    /**
     * 更新队伍
     *
     * @param teamUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍更新失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 删除队伍
     *
     * @param teamDeleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody TeamDeleteRequest teamDeleteRequest, HttpServletRequest request) {
        if (teamDeleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "删除队伍请求有误");
        }
        Long teamId = teamDeleteRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "删除队伍id有误");
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(teamId, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍删除失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取队伍
     *
     * @param teamId
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long teamId) {
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return ResultUtils.success(team);
    }

    /**
     * 搜索队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean isAdmin = userService.isAdmin(request);
        // 1. 查询队伍列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);
        queryHasJoinTeamNum(teamList, loginUser);
        return ResultUtils.success(teamList);
    }


    /**
     * 获取批量队伍分页
     *
     * @param teamQuery
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listPageTeams(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }

    /**
     * 加入队伍
     *
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入队伍请求有误");
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入队伍失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param request
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "退出退伍请求有误");
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "退出队伍失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取我创建的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        queryHasJoinTeamNum(teamList, loginUser);
        return ResultUtils.success(teamList);
    }
    
    /**
     * 获取我加入的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        // 取出不重复的队伍 id
        // teamId userId
        // 1, 2
        // 1, 3
        // 2, 3
        // result
        // 1 => 2, 3
        // 2 => 3
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        queryHasJoinTeamNum(teamList, loginUser);
        return ResultUtils.success(teamList);

    }

    /**
     * 查询加入队伍的人数
     *
     * @param teamList
     * @param loginUser
     */
    private void queryHasJoinTeamNum(List<TeamUserVO> teamList, User loginUser) {
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        // 2. 判断当前用户是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try {
            userTeamQueryWrapper.eq("userId", loginUser.getId());
            userTeamQueryWrapper.in("teamId", teamIdList);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            // 已加入的队伍 id 集合
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        } catch (Exception e) {
        }
        // 3. 查询已加入队伍的人数
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId", teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper);
        // 队伍 id => 加入这个队伍的用户列表
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team -> team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size()));
    }
}
