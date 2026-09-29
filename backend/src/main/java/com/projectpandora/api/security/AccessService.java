package com.projectpandora.api.security;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.user.Role;
import com.projectpandora.api.user.UserEntity;
import com.projectpandora.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AccessService {

    private final UserRepository userRepository;

    public AccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }
        return principal;
    }

    public void assertCanViewUserLogs(Long targetUserId) {
        UserPrincipal me = currentUser();
        if (me.getId().equals(targetUserId)) {
            return;
        }
        if (me.getRole() == Role.ADMIN) {
            return;
        }
        if (me.getRole() == Role.LEADER) {
            UserEntity target =
                    userRepository
                            .findById(targetUserId)
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "用户不存在"));
            if (me.getId().equals(target.getManagerId())) {
                return;
            }
        }
        throw new ApiException(HttpStatus.FORBIDDEN.value(), "无权查看该用户日志");
    }
}
