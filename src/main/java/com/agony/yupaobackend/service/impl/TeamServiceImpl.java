package com.agony.yupaobackend.service.impl;

import com.agony.yupaobackend.common.ErrorCode;
import com.agony.yupaobackend.exception.BusinessException;
import com.agony.yupaobackend.mapper.TeamMapper;
import com.agony.yupaobackend.pojo.domain.Team;
import com.agony.yupaobackend.pojo.domain.User;
import com.agony.yupaobackend.pojo.domain.UserTeam;
import com.agony.yupaobackend.pojo.enums.TeamStatusEnum;
import com.agony.yupaobackend.service.TeamService;
import com.agony.yupaobackend.service.UserTeamService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

/**
 * @author 11971
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2023-12-19 14:18:00
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Autowired
    private UserTeamService userTeamService;

    // 校验信息
    //   a. 队伍人数 > 1 且 <= 20
    //   b. 队伍标题 <= 20
    //   c. 描述 <= 512
    //   d. status 是否公开（int）不传默认为 0（公开）
    //   e. 如果 status 是加密状态，一定要有密码，且密码 <= 32
    //   f. 超时时间 > 当前时间
    //   g. 校验用户最多创建 5 个队伍
    @Override
    public long addTeam(Team team, User loginUser) {
        // 非空校验
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 是否登录
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //   a. 队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum > 20 || maxNum < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不符合要求");
        }
        //   b. 队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不符合要求");
        }
        //   c. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //   d. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不符合要求");
        }
        //   e. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            String password = team.getPassword();
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码不符合要求");
            }
        }
        //   f. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }
        //   g. 校验用户最多创建 5 个队伍
        // TODO: 有 bug，可能同时创建 100 个队伍
        Long userId = team.getUserId();
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long count = this.count(queryWrapper);
        if (count >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建五个队伍");
        }
        // 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 5. 插入用户 => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建用户队伍失败");
        }
        return teamId;
    }
}




