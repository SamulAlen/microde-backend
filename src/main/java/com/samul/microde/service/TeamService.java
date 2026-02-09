package com.samul.microde.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samul.microde.model.domain.Team;
import com.samul.microde.model.domain.User;

/**
* @author Samul_Alen
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-01-01 18:20:01
*/
public interface TeamService extends IService<Team> {

    /**
     *
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

}
