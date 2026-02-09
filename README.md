# Samul_Alen - 微扣伙伴匹配系统

> 作者：[程序员艾伦萨米尔](https://github.com/SamulAlen)

本项目为 [编程导航知识星球](https://yupi.icu) 的原创全栈项目，后端代码开源。

## 项目简介

**微扣伙伴匹配系统** (Microde Partner Matching System) 是一个基于标签相似度、技能互补、活跃度等多维度的智能伙伴推荐平台。系统通过分析用户的技术栈、兴趣偏好、项目经验等特征，为开发者智能推荐最合适的合作伙伴，帮助用户组建高效的开发团队。

项目启动后接口文档地址：http://localhost:8080/api/doc.html#/home

## 本项目适合的同学

1. 学过基本的前端（HTML + CSS + JS 三件套）或后端开发技术（Java Web）
2. 还不知道怎么独立做出完整的项目，想了解规范的开发流程
3. 想快速学习自己不熟悉的技术并且了解其应用（比如你只会前端，想了解后端）
4. 想全方位提高自己的编程能力
5. 想提升做项目的经验和系统设计能力
6. 想学习更多企业主流开发技术
7. 想给简历增加项目经验
8. 想开发和上线自己的网站

## 技术选型

### 前端

主要运用阿里 Ant Design 生态：

- HTML + CSS + JavaScript 三件套
- React 开发框架
- Ant Design Pro 项目模板
- Ant Design 组件库
- Umi 开发框架
- Umi Request 请求库

### 后端

- Java 编程语言
- Spring + SpringMVC + SpringBoot 框架
- MyBatis + MyBatis Plus 数据访问框架
- MySQL 数据库
- Redis 缓存
- jUnit 单元测试库
- Knife4j 接口文档

### 部署

- 单机部署
- Nginx
- 容器

## 项目收获

1. 学会前后端企业主流开发技术的应用
2. 了解做项目的完整流程，能够独立开发及上线项目
3. 学到系统设计的方法和经验
4. 学到一些实际的编码技巧，比如开发工具、快捷键、插件的使用
5. 学到代码的优化技巧，比如抽象、封装、提高系统性能、节约资源的方法
6. 学习登录态、代理、多环境、容器、跨域等重要地开发知识
7. 学到一些源码阅读的技巧
8. 提升自主解决问题的能力

## 核心功能

### 智能伙伴推荐

- **多维度匹配**: 标签相似度、技能互补度、活跃度、随机因子
- **多种推荐策略**: 全部推荐、相似推荐、互补推荐、活跃度推荐
- **预计算优化**: 离线预计算相似度和互补度，提升推荐性能
- **分页支持**: 支持分页浏览推荐结果
- **换一批功能**: 手动刷新获取新的推荐结果

### 用户管理

- 用户注册、登录、注销
- 用户信息管理
- 标签管理
- 推荐反馈收集

### 队伍管理

- 创建队伍
- 加入/退出队伍
- 队伍成员管理
- 队伍列表查询

## 项目结构

```
microde-backend-samul/
├── docs/                    # 项目文档
│   ├── assets/             # 文档附件
│   └── ...
├── sql/                     # 数据库脚本
│   └── Project_Init_Table.sql  # 数据库初始化脚本
└── src/main/java/com/samul/microde/
    ├── common/              # 通用类
    │   ├── BaseResponse.java   # 通用响应对象
    │   ├── ErrorCode.java      # 错误码定义
    │   ├── PageRequest.java    # 分页请求对象
    │   └── ResultUtils.java    # 响应工具类
    ├── constant/            # 常量定义
    │   ├── RedisCacheConstants.java  # Redis缓存Key常量
    │   └── UserConstant.java         # 用户常量
    ├── controller/          # 控制器层
    │   ├── UserController.java       # 用户管理接口
    │   ├── TeamController.java       # 队伍管理接口
    │   ├── UserTeamController.java  # 用户队伍关系接口
    │   └── PrecomputeController.java # 预计算管理接口
    ├── service/             # 服务层
    │   ├── impl/              # 服务实现
    │   └── ...
    ├── enums/               # 枚举类
    ├── model/               # 数据模型
    │   ├── domain/           # 实体类
    │   ├── dto/              # 数据传输对象
    │   └── request/          # 请求对象
    ├── once/                # 一次性执行脚本
    ├── utils/               # 工具类
    └── ...
```

## API 接口说明

项目使用 Knife4j 自动生成接口文档，所有接口均已添加中文说明：

### 用户管理 (`/user`)
- `POST /user/register` - 用户注册
- `POST /user/login` - 用户登录
- `POST /user/logout` - 用户注销
- `GET /user/current` - 获取当前登录用户
- `GET /user/search` - 搜索用户（管理员）
- `GET /user/search/tags` - 按标签搜索用户
- `GET /user/recommend` - 推荐用户
- `POST /user/recommend/smart` - 智能推荐用户
- `POST /user/recommend/feedback` - 推荐反馈
- `POST /user/tags/update` - 更新用户标签
- `POST /user/update` - 更新用户信息
- `POST /user/delete` - 删除用户

### 队伍管理 (`/team`)
- `POST /team/add` - 创建队伍
- `POST /team/delete` - 删除队伍
- `POST /team/update` - 更新队伍
- `GET /team/get` - 获取队伍详情
- `GET /team/list` - 获取队伍列表
- `GET /team/list/page` - 分页获取队伍列表

### 用户队伍关系 (`/userTeam`)
- `POST /userTeam/join` - 加入队伍
- `POST /userTeam/quit` - 退出队伍
- `GET /userTeam/listMembers/{teamId}` - 获取队伍成员列表

### 预计算管理 (`/precompute`)
- `POST /precompute/full` - 触发全量预计算
- `POST /precompute/incremental` - 触发增量预计算
- `POST /precompute/user/similarity` - 预计算用户相似度
- `POST /precompute/user/complement` - 预计算用户互补度
- `POST /precompute/refresh` - 换一批推荐

## 数据库设计

项目包含以下核心数据表：

- `user` - 用户表
- `tag` - 标签表
- `team` - 队伍表
- `user_team` - 用户队伍关系表
- `tag_id_mapping` - 标签ID映射表
- `user_recommend_feedback` - 用户推荐反馈表

详细建表语句请参考 `sql/Project_Init_Table.sql`。

## 核心算法

### 推荐算法

系统采用多策略推荐算法，综合计算用户匹配度：

1. **标签相似度** (30%): 通过 Jaccard 相似系数计算用户标签重叠度
2. **技能互补度** (30%): 识别互补技能（如前端+后端）
3. **活跃度** (20%): 基于用户近期活动频率
4. **随机因子** (5%): 增加推荐多样性
5. **预计算得分** (15%): 使用预计算的相似度和互补度分数

### 推荐策略

- **all**: 综合推荐（默认策略）
- **similar**: 相似推荐优先（相似度50%，互补度10%）
- **complement**: 互补推荐优先（互补度50%，相似度10%）
- **activity**: 活跃度推荐优先

### 性能优化

- **离线预计算**: 定时任务预计算用户相似度和互补度
- **Redis缓存**: 缓存Top 200相似/互补用户
- **候选集过滤**: 基于标签交集减少计算范围
- **BitSet优化**: 使用 BitSet 加速 Jaccard 计算

## 开发说明

### 环境要求

- JDK 1.8+
- Maven 3.x
- MySQL 5.7+
- Redis 3.0+

### 启动步骤

1. 创建数据库并执行初始化脚本
```bash
mysql -u root -p < sql/Project_Init_Table.sql
```

2. 修改配置文件 `src/main/resources/application.yml`

3. 启动项目
```bash
mvn spring-boot:run
```

4. 访问接口文档：http://localhost:8080/api/doc.html

## 许可证

本项目采用 MIT 许可证。
