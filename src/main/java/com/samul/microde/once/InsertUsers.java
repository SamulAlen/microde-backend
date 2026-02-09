package com.samul.microde.once;
import java.util.Date;

import com.samul.microde.mapper.UserMapper;
import com.samul.microde.model.domain.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

@Component
public class InsertUsers {

    @Resource
    UserMapper userMapper;
    // @Scheduled(fixedDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        //    @Scheduled(initialDelay = 5000,fixedRate = Long.MAX_VALUE )
        System.out.println("good");
        final int INSERt_USER = 1000;
        for (int i = 0; i < INSERt_USER; i++) {
            User user = new User();
            user.setUsername("假用户");
            user.setUserAccount("fake");
            user.setAvatarUrl("");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("123");
            user.setEmail("123@qq.com");
            user.setTags("[]");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("111111");
            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
    public static void main(String[] args) {
        new InsertUsers().doInsertUser();
    }
}

