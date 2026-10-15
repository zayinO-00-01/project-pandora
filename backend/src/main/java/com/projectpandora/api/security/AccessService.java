package com.projectpandora.api.security;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.user.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AccessService {
    private final UserRepository users;
    private final ViewRequestRepository requests;
    public AccessService(UserRepository users, ViewRequestRepository requests) {
        this.users=users; this.requests=requests;
    }
    public UserPrincipal currentUser() {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !(auth.getPrincipal() instanceof UserPrincipal principal))
            throw new ApiException(401,"未登录");
        var user=users.findById(principal.getId()).filter(u->!u.isDisabled())
            .orElseThrow(()->new ApiException(401,"账号不可用，请重新登录"));
        return new UserPrincipal(user.getId(),user.getUsername(),user.getRole(),user.getManagerId());
    }
    public void assertAdmin() {
        if(currentUser().getRole()!=Role.ADMIN) throw new ApiException(403,"仅系统管理员可操作");
    }
    public UserPrincipal assertBusiness() {
        var me=currentUser();
        if(me.getRole()==Role.ADMIN) throw new ApiException(403,"系统管理员无权办理业务");
        return me;
    }
    public boolean isActiveBusiness(UserEntity user) { return !user.isDisabled() && user.getRole()!=Role.ADMIN; }
    public List<UserEntity> downwardUsers() {
        var me=assertBusiness();
        var all=users.findAll();
        Map<Long,UserEntity> byId=new HashMap<>();all.forEach(u->byId.put(u.getId(),u));
        return all.stream().filter(this::isActiveBusiness)
            .filter(u->me.getId().equals(u.getId()) || me.getRole()==Role.FOUNDER
                || (isLeader(me.getRole()) && isAncestor(me.getId(),u.getId(),byId)))
            .toList();
    }
    public List<UserEntity> dispatchAssignees() {
        var me=assertBusiness();
        if(!isLeader(me.getRole())) return List.of();
        var all=users.findAll();
        Map<Long,UserEntity> byId=new HashMap<>();all.forEach(u->byId.put(u.getId(),u));
        return all.stream().filter(this::isActiveBusiness)
            .filter(u->rank(u.getRole())<rank(me.getRole()) && isAncestor(me.getId(),u.getId(),byId))
            .toList();
    }
    public void assertCanViewUserLogs(Long targetId) {
        var me=assertBusiness();
        var target=users.findById(targetId).orElseThrow(()->new ApiException(404,"用户不存在"));
        if(!isActiveBusiness(target)) throw new ApiException(403,"无权查看该用户业务");
        if(me.getId().equals(targetId) || me.getRole()==Role.FOUNDER
            || (isLeader(me.getRole()) && isAncestor(me.getId(),targetId))) return;
        if(isDirectManager(target) && requests.existsByRequesterIdAndTargetIdAndStatus(me.getId(),targetId,"approved")) return;
        throw new ApiException(403,"无权查看该用户业务");
    }
    public boolean isDirectManager(UserEntity target) {
        var me=assertBusiness();
        return isActiveBusiness(target) && target.getId().equals(me.getManagerId())
            && rank(target.getRole())>rank(me.getRole());
    }
    public void assertCanDispatch(UserEntity assignee) {
        var me=assertBusiness();
        if(!isLeader(me.getRole()) || !isActiveBusiness(assignee)
            || rank(me.getRole())<=rank(assignee.getRole()) || !isAncestor(me.getId(),assignee.getId()))
            throw new ApiException(403,"只能向实际下级派发任务");
    }
    public static boolean isLeader(Role role) { return role==Role.FOUNDER || role==Role.DEPT_HEAD || role==Role.TEAM_LEAD; }
    public static int rank(Role role) {
        return switch(role) { case ADMIN -> -1; case FOUNDER -> 4; case DEPT_HEAD -> 3; case TEAM_LEAD -> 2; case STAFF -> 1; };
    }
    public boolean isAncestor(Long viewerId,Long targetId) {
        Map<Long,UserEntity> byId=new HashMap<>();users.findAll().forEach(u->byId.put(u.getId(),u));
        return isAncestor(viewerId,targetId,byId);
    }
    private boolean isAncestor(Long viewerId,Long targetId,Map<Long,UserEntity> byId) {
        Set<Long> seen=new HashSet<>();Long current=targetId;
        while(current!=null && seen.add(current)) {
            var user=byId.get(current);if(user==null) return false;
            Long manager=user.getManagerId();if(viewerId.equals(manager)) return true;current=manager;
        }
        return false;
    }
}
