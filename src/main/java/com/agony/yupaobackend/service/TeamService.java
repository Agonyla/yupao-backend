package com.agony.yupaobackend.service;

import com.agony.yupaobackend.pojo.domain.Team;
import com.agony.yupaobackend.pojo.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
