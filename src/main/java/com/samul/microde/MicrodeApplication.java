package com.samul.microde;



import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 微扣伙伴匹配系统启动类
 *
 * @author <a href="https://github.com/SamulAlen">程序员艾伦</a>
 *
 */
@SpringBootApplication
@MapperScan("com.samul.microde.mapper")
@EnableScheduling
public class MicrodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicrodeApplication.class, args);
    }

}

// https://github.com/SamulAlen
