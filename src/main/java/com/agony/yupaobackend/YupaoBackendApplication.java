package com.agony.yupaobackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.agony.yupaobackend.mapper")
public class YupaoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YupaoBackendApplication.class, args);
    }

}
