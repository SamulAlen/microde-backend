package com.samul.microde.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 标签分类枚举
 * 用于前端快捷选择标签，提高推荐质量
 *
 * @author <a href="https://github.com/SamulAlen">程序员艾伦</a>
 */
@Getter
public enum TagCategory {

    /**
     * 编程语言
     */
    PROGRAMMING_LANGUAGE("编程语言", Arrays.asList(
            "Java", "Python", "JavaScript", "TypeScript", "Go", "C++", "C", "C#", "PHP", "Ruby", "Rust", "Swift", "Kotlin"
    )),

    /**
     * 框架与库
     */
    FRAMEWORK("框架与库", Arrays.asList(
            "Spring Boot", "Spring Cloud", "MyBatis", "Hibernate",
            "React", "Vue", "Angular", "Next.js", "Nuxt.js",
            "Django", "Flask", "FastAPI",
            "Flutter", "React Native",
            ".NET Core", "Entity Framework"
    )),

    /**
     * 技术方向
     */
    DIRECTION("技术方向", Arrays.asList(
            "前端", "后端", "全栈", "移动端", "桌面端", "DevOps", "算法", "测试", "运维", "架构"
    )),

    /**
     * 数据库
     */
    DATABASE("数据库", Arrays.asList(
            "MySQL", "PostgreSQL", "Oracle", "SQL Server", "MongoDB", "Redis", "Elasticsearch", "ClickHouse"
    )),

    /**
     * 工作经验
     */
    EXPERIENCE("工作经验", Arrays.asList(
            "在校生", "实习生", "应届生", "1-3年", "3-5年", "5-10年", "10年以上"
    )),

    /**
     * 求职/合作状态
     */
    STATUS("求职/合作状态", Arrays.asList(
            "找项目", "找队友", "找实习", "找全职", "学习交流", "技术分享", "创业"
    )),

    /**
     * 其他
     */
    OTHER("其他", Arrays.asList(
            "开源贡献", "技术博客", "GitHub", "Stack Overflow", "LeetCode", "算法竞赛", "黑客松"
    ));

    /**
     * 分类名称
     */
    private final String categoryName;

    /**
     * 标签列表
     */
    private final List<String> tags;

    TagCategory(String categoryName, List<String> tags) {
        this.categoryName = categoryName;
        this.tags = tags;
    }
}
