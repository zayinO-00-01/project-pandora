package com.projectpandora.api.security;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.user.Role;
import com.projectpandora.api.user.UserEntity;
import com.projectpandora.api.user.UserRepository;
import java.util.HashSet;
import java.util.Set;
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

    public void assertAdmin() {
        if (currentUser().getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "仅系统管理员可操作");
        }
    }

    public void assertCanViewUserLogs(Long targetUserId) {
        UserPrincipal me = currentUser();
        Role role = me.getRole();
        // Web system admin has no business log access (App-only capability).
        if (role == Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "系统管理员无权查看工作日志");
        }
        if (me.getId().equals(targetUserId)) {
            return;
        }
        UserEntity target =
                userRepository
                        .findById(targetUserId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "用户不存在"));
        if (target.getRole() == Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "无权查看该用户日志");
        }
        if (role == Role.FOUNDER) {
            return;
        }
        if (role == Role.TEAM_LEAD || role == Role.DEPT_HEAD) {
            if (isAncestor(me.getId(), targetUserId)) {
                return;
            }
        }
        throw new ApiException(HttpStatus.FORBIDDEN.value(), "无权查看该用户日志");
    }

    /** True when viewerId appears in target's manager_id chain (viewer is above target). */
    boolean isAncestor(Long viewerId, Long targetUserId) {
        Long current = targetUserId;
        Set<Long> seen = new HashSet<>();
        while (current != null && seen.add(current)) {
            UserEntity user =
                    userRepository
                            .findById(current)
                            .orElse(null);
            if (user == null) {
                return false;
            }
            Long managerId = user.getManagerId();
            if (viewerId.equals(managerId)) {
                return true;
            }
            current = managerId;
        }
        return false;
    }
}
