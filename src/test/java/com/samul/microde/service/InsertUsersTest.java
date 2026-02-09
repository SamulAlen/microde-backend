package com.samul.microde.service;

import com.samul.microde.mapper.UserMapper;
import com.samul.microde.model.domain.User;
import com.samul.microde.once.InsertUsers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
public class InsertUsersTest {
    @Resource
    UserService userService;

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        System.out.println("good");
        List<User> userList = new ArrayList<>();
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
            userList.add(user);
        }
        userService.saveBatch(userList, 100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    private ExecutorService executorService = new ThreadPoolExecutor(60, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 并发批量插入用户
     *
     */
    @Test
    public void doConCurrencyInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERt_USER = 500000;
        int batchSize = 5000;
        int j = 0;
        List<CompletableFuture<Void>> futureslist = new ArrayList<>();
        for (int i = 0; i < INSERt_USER/batchSize; i++) {
            List<User> userList = new ArrayList<>();
            while (true) {
                j++;
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
                userList.add(user);
                if (j % batchSize == 0){
                    break;
                }
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println(Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            }, executorService);
            futureslist.add(future);
        }
        CompletableFuture.allOf(futureslist.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
