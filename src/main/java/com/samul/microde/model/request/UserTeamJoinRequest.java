package com.samul.microde.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户加入队伍请求
 * @author Samul_Alen
 */
@Data
public class UserTeamJoinRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 队伍id
     */
    private Long teamId;

    /**
     * 密码（加密队伍需要）
     */
    private String password;

}
