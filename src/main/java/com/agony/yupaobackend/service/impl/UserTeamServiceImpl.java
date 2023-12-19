package com.agony.yupaobackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.agony.yupaobackend.pojo.domain.UserTeam;
import com.agony.yupaobackend.service.UserTeamService;
import com.agony.yupaobackend.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
 * @author 11971
 * @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
 * @createDate 2023-12-19 14:18:00
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
        implements UserTeamService {

}




