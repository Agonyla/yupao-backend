package com.agony.yupaobackend.service.impl;

import com.agony.yupaobackend.common.ErrorCode;
import com.agony.yupaobackend.exception.BusinessException;
import com.agony.yupaobackend.mapper.TeamMapper;
import com.agony.yupaobackend.pojo.domain.Team;
import com.agony.yupaobackend.pojo.domain.User;
import com.agony.yupaobackend.pojo.domain.UserTeam;
import com.agony.yupaobackend.pojo.dto.TeamQuery;
import com.agony.yupaobackend.pojo.enums.TeamStatusEnum;
import com.agony.yupaobackend.pojo.request.TeamJoinRequest;
import com.agony.yupaobackend.pojo.request.TeamQuitRequest;
import com.agony.yupaobackend.pojo.request.TeamUpdateRequest;
import com.agony.yupaobackend.pojo.vo.TeamUserVO;
import com.agony.yupaobackend.pojo.vo.UserVO;
import com.agony.yupaobackend.service.TeamService;
import com.agony.yupaobackend.service.UserService;
import com.agony.yupaobackend.service.UserTeamService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private UserService userService;

    @Autowired
    private RedissonClient redissonClient;

    // 校验信息
    //   a. 队伍人数 > 1 且 <= 20
    //   b. 队伍标题 <= 20
    //   c. 描述 <= 512
    //   d. status 是否公开（int）不传默认为 0（公开）
    //   e. 如果 status 是加密状态，一定要有密码，且密码 <= 32
    //   f. 超时时间 > 当前时间
    //   g. 校验用户最多创建 5 个队伍
    @Override
    @Transactional(rollbackFor = Exception.class)
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
        Long userId = loginUser.getId();
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

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // id查询
        Long id = teamQuery.getId();
        if (id != null && id > 0) {
            queryWrapper.eq("id", id);
        }
        // id集合查询
        List<Long> idList = teamQuery.getIdList();
        if (CollectionUtils.isNotEmpty(idList)) {
            queryWrapper.in("id", idList);
        }
        // 关键词查询 (同时对队伍名称和描述搜索)
        String searchText = teamQuery.getSearchText();
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
        }
        // 队伍名称
        String name = teamQuery.getName();
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name", name);
        }
        // 描述
        String description = teamQuery.getDescription();
        if (StringUtils.isNotBlank(description)) {
            queryWrapper.like("description", description);
        }
        // 最大人数
        Integer maxNum = teamQuery.getMaxNum();
        if (maxNum != null && maxNum > 0) {
            queryWrapper.eq("maxNum", maxNum);
        }
        // 创建用户id
        Long userId = teamQuery.getUserId();
        if (userId != null && userId > 0) {
            queryWrapper.eq("userId", userId);
        }
        // 状态
        Integer status = teamQuery.getStatus();
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            statusEnum = TeamStatusEnum.PUBLIC;
        }
        if (!isAdmin && TeamStatusEnum.PRIVATE.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        queryWrapper.eq("status", statusEnum.getValue());
        // 不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        // 关联查询创建人的用户信息
        ArrayList<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList) {
            Long teamUserId = team.getUserId();
            if (teamUserId == null) {
                continue;
            }
            User user = userService.getById(teamUserId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍id有误");
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 只有管理员和队伍创建者可以更新
        if (!userService.isAdmin(loginUser) && !Objects.equals(loginUser.getId(), oldTeam.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍密码不能为空");
            }
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getId();
        Team team = getTeamById(teamId);
        // 队伍过期
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        // 私有队伍
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(team.getStatus());
        if (TeamStatusEnum.PRIVATE.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        // 加密队伍，密码校验
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || !Objects.equals(teamJoinRequest.getPassword(), password)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码有误");
            }
        }
        // 该用户已加入的队伍数量 => 用户最多加入 5 个队伍
        Long userId = loginUser.getId();
        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("Agony:join_team");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId", userId);
                    long count = userTeamService.count(queryWrapper);
                    if (count > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建或加入五个队伍");
                    }
                    // 不能加入自己的队伍，不能重复加入已加入的队伍（幂等性）
                    queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId", userId);
                    queryWrapper.eq("teamId", teamId);
                    count = userTeamService.count(queryWrapper);
                    if (count > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复加入队伍");
                    }
                    // 已加入队伍数量
                    long hasJoinedNum = this.countTeamUserByTeamId(teamId);
                    if (hasJoinedNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
                    }
                    // 新增队伍 - 用户关联信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 只能自己释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍信息有误");
        }
        // 校验队伍是否存在
        Long teamId = teamQuitRequest.getTeamId();
        Team team = getTeamById(teamId);
        // 校验是否已加入队伍
        Long userId = loginUser.getId();
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setUserId(userId);
        queryUserTeam.setTeamId(teamId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(queryUserTeam);
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        // 只剩一人，队伍解散
        if (teamHasJoinNum == 1) {
            this.removeById(teamId);
        } else {
            // 队伍至少还剩两人
            // 是队长
            if (Objects.equals(team.getUserId(), userId)) {
                // 把队伍转移给最早加入的用户
                // 1. 查询已加入队伍的所有用户和加入时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队长失败");
                }
            }
        }
        // 移除关系
        return userTeamService.remove(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long teamId, User loginUser) {
        Team team = getTeamById(teamId);
        Long userId = team.getUserId();
        if (!Objects.equals(userId, loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        // 删除队员
        boolean result = userTeamService.remove(queryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 删除队伍
        return this.removeById(teamId);
    }


    /**
     * 获取队伍
     *
     * @param teamId
     * @return
     */
    public Team getTeamById(long teamId) {
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍id有误");
        }
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 获取队伍的人数
     *
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        return userTeamService.count(queryWrapper);
    }
}




