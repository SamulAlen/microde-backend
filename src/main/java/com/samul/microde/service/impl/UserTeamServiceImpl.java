package com.samul.microde.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samul.microde.model.domain.UserTeam;
import com.samul.microde.service.UserTeamService;
import com.samul.microde.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author lenovo
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-01-01 18:22:04
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




