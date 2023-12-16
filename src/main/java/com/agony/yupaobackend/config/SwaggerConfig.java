package com.agony.yupaobackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "test"})
public class SwaggerConfig {
    private Info createInfo(String title, String version) {
        return new Info()
                .title(title)
                .version(version)
                .description("Knife4j集成springdoc-openapi示例")
                .termsOfService("http://条款网址")
                .license(
                        new License().name("Apache 2.0")
                                .url("http://doc.xiaominfo.com"));
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(createInfo("鱼泡测试", "1.0.0"));
    }

}
