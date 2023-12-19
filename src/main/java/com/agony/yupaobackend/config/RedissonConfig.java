package com.agony.yupaobackend.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author Agony
 * @Create 2023/12/19 10:34
 * @Version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
// @Data
public class RedissonConfig {


    private String host;

    private String port;

    @Bean
    public RedissonClient redissonClient() {
        // 1. 创建配置
        Config config = new Config();
        // String redisAddress = String.format("redis://%s:%s", host, port);
        String redisAddress = "redis://127.0.0.1:6379";
        //  使用单个Redis，没有开集群 useClusterServers()  设置地址和使用库
        config.useSingleServer().setAddress(redisAddress).setDatabase(3);
        // 2. 创建实例
        return Redisson.create(config);
    }
}
