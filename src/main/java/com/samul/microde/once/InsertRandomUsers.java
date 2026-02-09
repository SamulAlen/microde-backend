package com.samul.microde.once;

import com.google.gson.Gson;
import com.samul.microde.enums.TagCategory;
import com.samul.microde.mapper.UserMapper;
import com.samul.microde.model.domain.User;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 插入500个随机用户，用于测试推荐系统
 * 密码统一为：12345678
 */
@Component
public class InsertRandomUsers {

    private static final String SALT = "yupi";
    private static final String PASSWORD = "12345678";

    // 姓氏列表
    private static final String[] SURNAMES = {
            "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
            "徐", "孙", "马", "朱", "胡", "郭", "何", "高", "林", "罗"
    };

    // 名字列表
    private static final String[] NAMES = {
            "伟", "芳", "娜", "秀英", "敏", "静", "丽", "强", "磊", "军",
            "洋", "勇", "艳", "杰", "娟", "涛", "明", "超", "秀兰", "霞",
            "平", "刚", "桂英", "玉兰", "萍", "毅", "浩", "宇", "轩", "然"
    };

    // 手机号前缀
    private static final String[] PHONE_PREFIXES = {
            "130", "131", "132", "133", "135", "136", "137", "138", "139",
            "150", "151", "152", "153", "155", "156", "157", "158", "159",
            "180", "181", "182", "183", "185", "186", "187", "188", "189"
    };

    // 邮箱域名
    private static final String[] EMAIL_DOMAINS = {
            "qq.com", "163.com", "gmail.com", "outlook.com", "126.com", "sina.com"
    };

    @Resource
    private UserMapper userMapper;

    /**
     * 执行插入
     * 取消下面的注释来启用定时任务，应用启动后会自动执行
     * 注意：已禁用自动执行，请使用 InsertRandomUsersTest 测试类手动执行
     */
    // @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doInsert() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final int INSERT_USER_COUNT = 2000;
        System.out.println("开始插入 " + INSERT_USER_COUNT + " 个随机用户...");

        Random random = new Random();

        for (int i = 0; i < INSERT_USER_COUNT; i++) {
            try {
                User user = new User();

                // 生成随机用户名
                String username = generateRandomName(random);
                user.setUsername(username);

                // 生成随机账户名
                String userAccount = generateRandomAccount(username, random);
                user.setUserAccount(userAccount);

                // 加密密码（统一为12345678）
                String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + PASSWORD).getBytes());
                user.setUserPassword(encryptedPassword);

                // 随机性别
                user.setGender(random.nextInt(3)); // 0-男, 1-女, 2-保密

                // 随机手机号
                user.setPhone(generateRandomPhone(random));

                // 随机邮箱
                user.setEmail(generateRandomEmail(userAccount, random));

                // 随机标签（3-8个标签）
                List<String> tags = generateRandomTags(random);
                user.setTags(new Gson().toJson(tags));

                // 星球编号
                user.setPlanetCode(generateRandomPlanetCode(random));

                // 头像URL（使用随机头像服务）
                user.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + userAccount);

                // 状态和角色
                user.setUserStatus(0); // 正常
                user.setUserRole(0);   // 普通用户

                userMapper.insert(user);

                if ((i + 1) % 50 == 0) {
                    System.out.println("已插入 " + (i + 1) + " 个用户...");
                }
            } catch (Exception e) {
                System.err.println("插入第 " + (i + 1) + " 个用户时出错: " + e.getMessage());
            }
        }

        stopWatch.stop();
        System.out.println("插入完成！共插入 " + INSERT_USER_COUNT + " 个用户");
        System.out.println("耗时: " + stopWatch.getTotalTimeMillis() + " ms");
    }

    /**
     * 生成随机姓名
     */
    private String generateRandomName(Random random) {
        String surname = SURNAMES[random.nextInt(SURNAMES.length)];
        String name = NAMES[random.nextInt(NAMES.length)];

        // 30%概率生成双字名
        if (random.nextFloat() < 0.3) {
            String name2 = NAMES[random.nextInt(NAMES.length)];
            return surname + name + name2;
        }

        return surname + name;
    }

    /**
     * 生成随机账户名
     */
    private String generateRandomAccount(String username, Random random) {
        // 使用拼音+数字的组合
        String[] pinyin = {"user", "dev", "coder", "programmer", "java", "python", "web", "app", "tech", "data"};
        String prefix = pinyin[random.nextInt(pinyin.length)];
        int suffix = random.nextInt(10000);
        return prefix + suffix;
    }

    /**
     * 生成随机手机号
     */
    private String generateRandomPhone(Random random) {
        String prefix = PHONE_PREFIXES[random.nextInt(PHONE_PREFIXES.length)];
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成随机邮箱
     */
    private String generateRandomEmail(String userAccount, Random random) {
        String domain = EMAIL_DOMAINS[random.nextInt(EMAIL_DOMAINS.length)];
        return userAccount + "@" + domain;
    }

    /**
     * 生成随机标签
     */
    private List<String> generateRandomTags(Random random) {
        List<String> allTags = new ArrayList<>();

        // 收集所有标签
        for (TagCategory category : TagCategory.values()) {
            allTags.addAll(category.getTags());
        }

        // 打乱顺序
        Collections.shuffle(allTags);

        // 随机选择3-8个标签
        int tagCount = 3 + random.nextInt(6);
        return allTags.subList(0, Math.min(tagCount, allTags.size()));
    }

    /**
     * 生成随机星球编号
     */
    private String generateRandomPlanetCode(Random random) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 主函数，可直接运行
     */
    public static void main(String[] args) {
        InsertRandomUsers inserter = new InsertRandomUsers();
        inserter.doInsert();
    }
}
