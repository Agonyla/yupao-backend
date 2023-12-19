package com.agony.yupaobackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.agony.yupaobackend.pojo.domain.Team;
import com.agony.yupaobackend.service.TeamService;
import com.agony.yupaobackend.mapper.TeamMapper;
import org.springframework.stereotype.Service;

/**
 * @author 11971
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2023-12-19 14:18:00
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

}




