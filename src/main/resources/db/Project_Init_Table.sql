-- ====================================
-- 用户中心项目数据库初始化脚本
-- 创建日期: 2026-02-09
-- 说明: 本脚本包含所有基础表的创建语句
-- ====================================

-- 设置字符集
SET NAMES utf8mb4;
SET
    FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT 'id',
    `username`     varchar(256)          DEFAULT NULL COMMENT '用户昵称',
    `userAccount`  varchar(256)          DEFAULT NULL COMMENT '账号',
    `avatarUrl`    varchar(1024)         DEFAULT NULL COMMENT '用户头像',
    `gender`       tinyint(4)            DEFAULT NULL COMMENT '性别',
    `userPassword` varchar(512) NOT NULL COMMENT '密码',
    `phone`        varchar(128)          DEFAULT NULL COMMENT '电话',
    `email`        varchar(512)          DEFAULT NULL COMMENT '邮箱',
    `tags`         varchar(1024)         DEFAULT NULL COMMENT '标签 json 列表',
    `profile`      varchar(512)          DEFAULT NULL COMMENT '个人简介',
    `userStatus`   int(11)      NOT NULL DEFAULT '0' COMMENT '状态 0 - 正常',
    `userRole`     int(11)      NOT NULL DEFAULT '0' COMMENT '用户角色 0 - 普通用户 1 - 管理员',
    `planetCode`   varchar(512)          DEFAULT NULL COMMENT '星球编号',
    `createTime`   datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     tinyint(4)   NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniIdx_userAccount` (`userAccount`),
    KEY `idx_planetCode` (`planetCode`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ----------------------------
-- Table structure for tag
-- ----------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `tagName`    varchar(256)        DEFAULT NULL COMMENT '标签名称',
    `userId`     bigint(20)          DEFAULT NULL COMMENT '用户 id',
    `parentId`   bigint(20)          DEFAULT NULL COMMENT '父标签 id',
    `isParent`   tinyint(4)          DEFAULT NULL COMMENT '0 - 不是, 1 - 父标签',
    `createTime` datetime            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniIdx_tagName` (`tagName`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='标签表';

-- ----------------------------
-- Table structure for team
-- ----------------------------
DROP TABLE IF EXISTS `team`;
CREATE TABLE `team`
(
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT 'id',
    `name`        varchar(256) NOT NULL COMMENT '队伍名称',
    `description` varchar(1024)         DEFAULT NULL COMMENT '描述',
    `maxNum`      int(11)      NOT NULL DEFAULT '1' COMMENT '最大人数',
    `expireTime`  datetime              DEFAULT NULL COMMENT '过期时间',
    `userId`      bigint(20)            DEFAULT NULL COMMENT '用户id',
    `status`      int(11)      NOT NULL DEFAULT '0' COMMENT '0 - 公开，1 - 私有，2 - 加密',
    `password`    varchar(512)          DEFAULT NULL COMMENT '密码',
    `createTime`  datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    tinyint(4)   NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='队伍表';

-- ----------------------------
-- Table structure for user_team
-- ----------------------------
DROP TABLE IF EXISTS `user_team`;
CREATE TABLE `user_team`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userId`     bigint(20)          DEFAULT NULL COMMENT '用户id',
    `teamId`     bigint(20)          DEFAULT NULL COMMENT '队伍id',
    `joinTime`   datetime            DEFAULT NULL COMMENT '加入时间',
    `createTime` datetime            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`userId`),
    KEY `idx_teamId` (`teamId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户队伍关系表';

-- ----------------------------
-- Table structure for tag_id_mapping
-- ----------------------------
DROP TABLE IF EXISTS `tag_id_mapping`;
CREATE TABLE `tag_id_mapping`
(
    `id`           INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `tag_name`     VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    `tag_category` VARCHAR(20) COMMENT '标签分类: LANGUAGE/FRAMEWORK/DIRECTION/EXPERIENCE/STATUS',
    `is_active`    TINYINT  DEFAULT 1 COMMENT '是否启用: 1-启用, 0-禁用',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_category` (`tag_category`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='标签ID映射表';

-- ----------------------------
-- Table structure for user_recommend_feedback
-- ----------------------------
DROP TABLE IF EXISTS `user_recommend_feedback`;
CREATE TABLE `user_recommend_feedback`
(
    `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id`             BIGINT NOT NULL COMMENT '用户ID',
    `recommended_user_id` BIGINT NOT NULL COMMENT '被推荐的用户ID',
    `feedback`            INT    NOT NULL COMMENT '反馈值: 1-喜欢, -1-不感兴趣',
    `recommend_strategy`  VARCHAR(20) COMMENT '推荐策略: all/skill/complement/activity',
    `recommend_score`     DOUBLE COMMENT '推荐时的得分',
    `match_type`          VARCHAR(50) COMMENT '推荐时的匹配类型',
    `create_time`         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_recommended_user_id` (`recommended_user_id`),
    INDEX `idx_user_feedback` (`user_id`, `recommended_user_id`),
    UNIQUE KEY `uk_user_recommend` (`user_id`, `recommended_user_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户推荐反馈表';

-- ----------------------------
-- Insert predefined tags
-- ----------------------------
INSERT INTO `tag_id_mapping` (`tag_name`, `tag_category`)
VALUES
-- 编程语言
('Java', 'LANGUAGE'),
('Python', 'LANGUAGE'),
('JavaScript', 'LANGUAGE'),
('Go', 'LANGUAGE'),
('C++', 'LANGUAGE'),
('TypeScript', 'LANGUAGE'),
('Kotlin', 'LANGUAGE'),
('Swift', 'LANGUAGE'),
('PHP', 'LANGUAGE'),
('Rust', 'LANGUAGE'),
('Ruby', 'LANGUAGE'),
('Dart', 'LANGUAGE'),
-- 框架
('React', 'FRAMEWORK'),
('Vue', 'FRAMEWORK'),
('Angular', 'FRAMEWORK'),
('Spring Boot', 'FRAMEWORK'),
('Django', 'FRAMEWORK'),
('Flask', 'FRAMEWORK'),
('Express', 'FRAMEWORK'),
('NestJS', 'FRAMEWORK'),
('Next.js', 'FRAMEWORK'),
('Nuxt.js', 'FRAMEWORK'),
('Spring Cloud', 'FRAMEWORK'),
('MyBatis', 'FRAMEWORK'),
('Hibernate', 'FRAMEWORK'),
('JPA', 'FRAMEWORK'),
('Entity Framework', 'FRAMEWORK'),
('FastAPI', 'FRAMEWORK'),
('Node.js', 'FRAMEWORK'),
('DotNET Core', 'FRAMEWORK'),
-- 技术方向
('前端', 'DIRECTION'),
('后端', 'DIRECTION'),
('全栈', 'DIRECTION'),
('移动端', 'DIRECTION'),
('桌面端', 'DIRECTION'),
('DevOps', 'DIRECTION'),
('算法', 'DIRECTION'),
('架构', 'DIRECTION'),
('测试', 'DIRECTION'),
('运维', 'DIRECTION'),
('大数据', 'DIRECTION'),
('人工智能', 'DIRECTION'),
('区块链', 'DIRECTION'),
('游戏开发', 'DIRECTION'),
('嵌入式', 'DIRECTION'),
-- 经验水平
('实习生', 'EXPERIENCE'),
('应届生', 'EXPERIENCE'),
('1-3年', 'EXPERIENCE'),
('3-5年', 'EXPERIENCE'),
('5-10年', 'EXPERIENCE'),
('10年以上', 'EXPERIENCE'),
-- 状态/目标
('找项目', 'STATUS'),
('找队友', 'STATUS'),
('学习交流', 'STATUS'),
('技术分享', 'STATUS'),
('找实习', 'STATUS'),
('找全职', 'STATUS'),
('接外包', 'STATUS'),
('创业', 'STATUS'),
('开源贡献', 'STATUS'),
('技术博客', 'STATUS'),
('黑客松', 'STATUS'),
('开源项目', 'STATUS');

-- ----------------------------
-- Insert test data for team
-- ----------------------------
INSERT INTO `team` (`id`, `name`, `description`, `maxNum`, `expireTime`, `userId`, `status`, `password`, `createTime`,
                    `updateTime`, `isDelete`)
VALUES (1, 'aaa', '', 6, NULL, 1, 1, '', '2024-01-01 21:04:43', '2026-02-08 16:03:39', 0),
       (2, 'Samul的小队', 'Samul的代码学习小队，目前寻找前端队友', 10, '2024-01-03 08:00:00', 2, 0, '',
        '2024-01-01 23:57:52', '2026-02-08 14:33:51', 1),
       (6, 'A建材批发', 'A市XX路832号', 10, '2027-02-28 13:07:38', 9, 0, NULL, '2026-02-08 13:08:02',
        '2026-02-08 16:03:32', 0),
       (7, 'TestTeam', '这是一队测试队伍', 7, '2026-02-28 00:00:00', 2, 2, '123456', '2026-02-08 14:43:39',
        '2026-02-08 14:43:39', 0);

-- ----------------------------
-- Insert test data for user
-- ----------------------------
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `tags`,
                    `profile`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`)
VALUES ('Yupi', 1, 'dogYupi',
        'https://cn.bing.com/images/search?view=detailV2&ccid=IHxieNRz&id=A27FC3E1D44615B28DC1E5548C5A8E66F03FA1AE&thid=OIP.IHxieNRzROiAYCCsiEfpMAHaHI&mediaurl=https%3a%2f%2fc-ssl.duitang.com%2fuploads%2fitem%2f201906%2f24%2f20190624162251_cboto.jpg&exph=693&expw=720&q=%e5%a4%b4%e5%83%8f%e7%94%b7%e7%94%9f&simid=608042944490247900&FORM=IRPRST&ck=643D97D90E4CCA9D43B8C1C8754D65CF&selectedIndex=0&idpp=overlayview&ajaxhist=0&ajaxserp=0',
        1, 'b0dd3697a192885d7c055db46155b26a', '123', '456', '[\"男\",\"java\",\"c++\",\"python\",\"大6\"]',
        '开心大鱼皮', 0, '2023-08-06 14:14:22', '2023-12-01 12:22:13', 0, 1, '1'),
       ('samulalen', 2, 'samulalen', 'https://www.vcg.com/creative/1116659258', 1, 'b0dd3697a192885d7c055db46155b26a',
        '123', '123', '[\"女\", \"java\", \"python\", \"大一\"]', '开心大艾伦', 0, '2023-11-14 00:15:15',
        '2023-11-16 00:19:24', 0, 1, '2'),
       ('tianbowen', 3, 'tianbowen',
        'https://cn.bing.com/images/search?view=detailV2&ccid=IHxieNRz&id=A27FC3E1D44615B28DC1E5548C5A8E66F03FA1AE&thid=OIP.IHxieNRzROiAYCCsiEfpMAHaHI&mediaurl=https%3a%2f%2fc-ssl.duitang.com%2fuploads%2fitem%2f201906%2f24%2f20190624162251_cboto.jpg&exph=693&expw=720&q=%e5%a4%b4%e5%83%8f%e7%94%b7%e7%94%9f&simid=608042944490247900&FORM=IRPRST&ck=643D97D90E4CCA9D43B8C1C8754D65CF&selectedIndex=0&idpp=overlayview&ajaxhist=0&ajaxserp=0',
        0, 'f8de235116ca2ec0b8ee885b5c743072', '123', '123', '[\"男\", \"大一\"]', '死猪', 0, '2023-11-14 00:17:23',
        '2023-11-15 20:14:45', 0, 0, '3'),
       ('samulalen1', 4, 'samulalen1',
        'https://cn.bing.com/images/search?view=detailV2&ccid=zP5XTRMs&id=8510E73FCE6BA46CC8773EA54A49A60585194E20&thid=OIP.zP5XTRMsotw4v3aFj3fpQQAAAA&mediaurl=https%3a%2f%2fp.qqan.com%2fup%2f2021-6%2f16246735796128385.png&exph=400&expw=400&q=%e5%a4%b4%e5%83%8f&simid=607991997608636293&FORM=IRPRST&ck=0158507F535DBE823971D41CDD7C320F&selectedIndex=2&ajaxhist=0&ajaxserp=0',
        1, 'b0dd3697a192885d7c055db46155b26a', '123', '123', '[\"女\", \"java\", \"python\", \"大一\"]', '开心大艾伦',
        0, '2023-11-14 00:15:15', '2023-11-15 20:09:20', 0, 0, '4'),
       ('samulalen2', 5, 'samulalen2', 'https://www.vcg.com/creative/1116659258', 0, 'f8de235116ca2ec0b8ee885b5c743072',
        '123', '123', '[\"男\", \"大一\"]', '死猪', 0, '2023-11-14 00:17:23', '2026-02-08 15:24:25', 0, 0, '5'),
       ('samulalen3', 6, 'samulalen3',
        'https://cn.bing.com/images/search?view=detailV2&ccid=IHxieNRz&id=A27FC3E1D44615B28DC1E5548C5A8E66F03FA1AE&thid=OIP.IHxieNRzROiAYCCsiEfpMAHaHI&mediaurl=https%3a%2f%2fc-ssl.duitang.com%2fuploads%2fitem%2f201906%2f24%2f20190624162251_cboto.jpg&exph=693&expw=720&q=%e5%a4%b4%e5%83%8f%e7%94%b7%e7%94%9f&simid=608042944490247900&FORM=IRPRST&ck=643D97D90E4CCA9D43B8C1C8754D65CF&selectedIndex=0&idpp=overlayview&ajaxhist=0&ajaxserp=0',
        0, 'f8de235116ca2ec0b8ee885b5c743072', '123', '123', '[\"男\", \"大一\"]', '开心大艾伦', 0,
        '2023-11-14 00:17:23', '2023-11-15 20:09:20', 0, 0, '6'),
       ('samulalen4', 7, 'samulalen4',
        'https://cn.bing.com/images/search?view=detailV2&ccid=zP5XTRMs&id=8510E73FCE6BA46CC8773EA54A49A60585194E20&thid=OIP.zP5XTRMsotw4v3aFj3fpQQAAAA&mediaurl=https%3a%2f%2fp.qqan.com%2fup%2f2021-6%2f16246735796128385.png&exph=400&expw=400&q=%e5%a4%b4%e5%83%8f&simid=607991997608636293&FORM=IRPRST&ck=0158507F535DBE823971D41CDD7C320F&selectedIndex=2&ajaxhist=0&ajaxserp=0',
        1, 'f8de235116ca2ec0b8ee885b5c743072', '123', '123', '[\"女\", \"java\", \"python\", \"大一\"]', '开心大艾伦',
        0, '2023-11-14 00:17:23', '2023-11-15 20:09:20', 0, 0, '7'),
       ('假用户', 8, 'fake', '', 0, '12345678', '123', '123@qq.com', '[]', NULL, 0, '2023-12-29 22:36:06',
        '2023-12-29 22:36:06', 0, 0, '111111'),
       ('testuser', 9, 'test', 'https://c-ssl.dtstatic.com/uploads/blog/202304/29/20230429194620_cd903.thumb.400_0.jpg',
        0, 'b0dd3697a192885d7c055db46155b26a', '13725627732', '3124@gal.com', NULL, NULL, 0, '2026-02-07 18:05:33',
        '2026-02-08 14:20:56', 0, 0, '9');

-- ----------------------------
-- Insert test data for user_team
-- ----------------------------
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`)
VALUES (1, 2, 1, '2024-01-01 23:57:52', '2024-01-01 23:57:52', '2024-01-01 23:57:52', 0),
       (2, 2, 2, '2024-01-02 00:15:39', '2024-01-02 00:15:38', '2024-01-02 00:15:38', 0),
       (5, 9, 6, '2026-02-08 13:08:03', '2026-02-08 13:08:02', '2026-02-08 13:08:02', 0),
       (6, 2, 5, '2026-02-08 14:43:39', '2026-02-08 14:43:39', '2026-02-08 14:43:39', 0),
       (15, 1, 7, '2026-02-08 17:36:49', '2026-02-08 17:36:49', '2026-02-08 17:36:49', 0),
       (16, 2, 7, '2026-02-08 17:41:47', '2026-02-08 17:41:47', '2026-02-08 17:41:47', 0),
       (17, 1, 3, '2026-02-08 17:42:22', '2026-02-08 17:42:21', '2026-02-08 17:42:21', 0),
       (18, 1, 1, '2026-02-08 17:42:48', '2026-02-08 17:42:48', '2026-02-08 17:42:48', 0);

SET
    FOREIGN_KEY_CHECKS = 1;
