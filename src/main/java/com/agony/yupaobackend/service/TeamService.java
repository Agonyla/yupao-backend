package com.agony.yupaobackend.service;

import com.agony.yupaobackend.pojo.domain.Team;
import com.agony.yupaobackend.pojo.domain.User;
import com.agony.yupaobackend.pojo.dto.TeamQuery;
import com.agony.yupaobackend.pojo.vo.TeamUserVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author 11971
 * @description 针对表【team(队伍)】的数据库操作Service
 * @createDate 2023-12-19 14:18:00
 */
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 搜索队伍
     *
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);
}
