package com.agony.yupaobackend.service;

import com.agony.yupaobackend.pojo.domain.User;
import com.agony.yupaobackend.pojo.domain.UserTeam;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @Author Agony
 * @Create 2023/12/15 15:28
 * @Version 1.0
 */
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserTeamService userTeamService;

    @Test
    void searchByTagsTest() {
        List<String> tagNameList = Arrays.asList("男");
        List<User> users = userService.searchUsersByTags(tagNameList);
        users.forEach(System.out::println);
    }

    @Test
    void dateTest() {
        Date date = new Date();
        System.out.println(date);
    }

    @Test
    void queryWrapperTest() {
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("joinTime"); // 假设 joinTime 是加入时间的字段名
        queryWrapper.last("limit 2"); // 限制结果数量为2
        List<UserTeam> list = userTeamService.list(queryWrapper);
        UserTeam userTeam = list.get(0);
        Long userId = userTeam.getUserId();
        System.out.println(userId);
        list.forEach(System.out::println);
    }
}