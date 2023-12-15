package com.agony.yupaobackend.service;

import com.agony.yupaobackend.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author Agony
 * @Create 2023/12/15 15:28
 * @Version 1.0
 */
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;
    @Test
     void searchByTagsTest(){
        List<String> tagNameList = List.of("python");
        List<User> users = userService.searchUsersByTags(tagNameList);
        users.forEach(System.out::println);
    }
}