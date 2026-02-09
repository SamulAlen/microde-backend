package com.samul.microde.utils;

import com.samul.microde.common.ErrorCode;
import com.samul.microde.exception.BusinessException;
import com.samul.microde.model.domain.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static com.samul.microde.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 Session 管理工具
 * 每个浏览器 Session 只存储一个用户的登录态
 *
 * @author: SamulAlen
 * @date: 2026/02/07
 */
public class UserSessionManager {

    /**
     * 添加用户到 Session（直接替换，不保留旧用户）
     *
     * @param user    用户对象
     * @param request HTTP请求
     */
    public static void addUserToSession(User user, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute(USER_LOGIN_STATE, user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request HTTP请求
     * @return 当前用户
     */
    public static User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute(USER_LOGIN_STATE);

        if (user == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }

        return user;
    }

    /**
     * 移除当前用户（注销）
     *
     * @param request HTTP请求
     */
    public static void removeCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.removeAttribute(USER_LOGIN_STATE);
    }

    /**
     * 检查用户是否已登录
     *
     * @param request HTTP请求
     * @return 是否已登录
     */
    public static boolean isLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession();
        return session.getAttribute(USER_LOGIN_STATE) != null;
    }
}
