package com.samul.microde;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.security.NoSuchAlgorithmException;

/**
 * 微扣伙伴匹配系统启动类测试
 *
 * @author <a href="https://github.com/SamulAlen">程序员艾伦</a>
 *
 */
@SpringBootTest
class MicrodeApplicationTests {

    @Test
    void testDigest() throws NoSuchAlgorithmException {
        String newPassword = DigestUtils.md5DigestAsHex(("abcd" + "mypassword").getBytes());
        System.out.println(newPassword);
    }


    @Test
    void contextLoads() {

    }

}

