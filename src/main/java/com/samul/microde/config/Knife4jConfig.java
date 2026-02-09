package com.samul.microde.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Knife4j 接口文档配置
 *
 * 基于 OpenAPI 3 规范，兼容 Spring Boot 2.6+
 *
 * @author: SamulAlen
 * @date: 2026/02/07
 */
@Configuration
@Profile({"dev", "test"})
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("微扣伙伴匹配系统-接口文档")
                        .version("1.1")
                        .description("众里寻他千百度，慕然回首那人却在灯火阑珊处")
                        .contact(new Contact()
                                .name("SamulAlen")
                                .email("2721525758n@qq.com")
                                .url("https://github.com/SamulAlen"))
                        .termsOfService("https://www.baidu.com")
                        .license(new License()
                                .name("Swagger-的使用(详细教程)")
                                .url("https://blog.csdn.net/xhmico/article/details/125353535")));
    }
}
