package com.projectpandora.api.security;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.user.Role;
import com.projectpandora.api.user.UserEntity;
import com.projectpandora.api.user.UserRepository;
import com.projectpandora.api.user.ViewRequestRepository;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AccessService {

    private final UserRepository userRepository;
    private final ViewRequestRepository requests;

    public AccessService(UserRepository userRepository, ViewRequestRepository requests) {
        this.userRepository = userRepository;
        this.requests = requests;
    }

    public UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }
        return principal;
    }

    public void assertCanViewUserLogs(Long targetUserId) {
        var me=currentUser();
        if(canViewDown(targetUserId))return;
        var target=userRepository.findById(targetUserId).orElseThrow(() -> new ApiException(404,"用户不存在"));
        if(isDirectManager(target) && requests.existsByRequesterIdAndTargetIdAndStatus(me.getId(),targetUserId,"approved"))return;
        throw new ApiException(HttpStatus.FORBIDDEN.value(), "无权查看该用户日志");
    }

    public boolean isDirectManager(UserEntity target) {
        var me=currentUser();
        return target.getRole()!=Role.ADMIN && me.getManagerId()!=null && me.getManagerId().equals(target.getId())
                && me.getRole().level()<target.getRole().level();
    }

    public boolean canViewDown(Long id) {
        var me=currentUser();
        if(me.getId().equals(id) || me.getRole()==Role.ADMIN)return true;
        var target=userRepository.findById(id).orElse(null);
        if(target==null || target.getRole()==Role.ADMIN || target.getRole().level()>=me.getRole().level())return false;
        if(me.getRole()==Role.LEADER)return me.getId().equals(target.getManagerId());
        var seen=new HashSet<Long>();
        while(target.getManagerId()!=null && seen.add(target.getId())) {
            if(me.getId().equals(target.getManagerId()))return true;
            target=userRepository.findById(target.getManagerId()).orElse(null);
            if(target==null)return false;
        }
        return false;
    }

    public List<UserEntity> downwardUsers() {
        return userRepository.findAll().stream().filter(u->canViewDown(u.getId())).toList();
    }
}
