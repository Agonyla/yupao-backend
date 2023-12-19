package com.agony.yupaobackend.service;

import com.agony.yupaobackend.pojo.domain.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;


/**
 * @Author Agony
 * @Create 2023/12/18 21:24
 * @Version 1.0
 * @Description: Redis测试
 */
@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;


    @Test
    void test() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("AgonyString", "Agony");
        valueOperations.set("AgonyInt", 1);
        valueOperations.set("AgonyDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("Agony");
        valueOperations.set("AgonyUser", user);

        // 查
        Object agony = valueOperations.get("AgonyString");
        Assertions.assertEquals("Agony", (String) agony);
        agony = valueOperations.get("AgonyInt");
        Assertions.assertEquals(1, (int) (Integer) agony);
        agony = valueOperations.get("AgonyDouble");
        Assertions.assertEquals(2.0, (Double) agony);
        System.out.println(valueOperations.get("AgonyUser"));
        // 改
        valueOperations.set("AgonyString", "lalala");

        //删
//        redisTemplate.delete("shayuString");
    }

}
