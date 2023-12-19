package com.agony.yupaobackend.controller;

import com.agony.yupaobackend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Agony
 * @Create 2023/11/27 20:49
 * @Version 1.0
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(value = {"http://localhost:5173/"}, allowCredentials = "true")
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;


}
