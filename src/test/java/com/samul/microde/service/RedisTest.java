package com.samul.microde.service;

import com.samul.microde.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

/**
 * @author: Samul
 * @date: 2022/12/10
 * @ClassName: yupao-backend01
 * @Description:    Redis测试
 */
@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;


    @Test
    void test() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("SamulString", "fish");
        valueOperations.set("SamulInt", 1);
        valueOperations.set("SamulDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("Samul");
        valueOperations.set("SamulUser", user);

        // 查
        Object Samul = valueOperations.get("SamulString");
        Assertions.assertTrue("fish".equals((String) Samul));
        Samul = valueOperations.get("SamulInt");
        Assertions.assertTrue(1 == (Integer) Samul);
        Samul = valueOperations.get("SamulDouble");
        Assertions.assertTrue(2.0 == (Double) Samul);
        System.out.println(valueOperations.get("SamulUser"));
        valueOperations.set("SamulString", "fish");

        // 删
//        redisTemplate.delete("SamulString");
    }

}
