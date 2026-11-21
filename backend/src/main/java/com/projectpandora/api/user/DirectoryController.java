package com.projectpandora.api.user;

import com.projectpandora.api.security.AccessService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/directory")
public class DirectoryController {
    private final UserRepository users;
    private final AccessService access;
    public DirectoryController(UserRepository users,AccessService access) {this.users=users;this.access=access;}
    public record Contact(Long id,String username,String displayName,String role,Long managerId,String managerName,String phone,String avatar,String avatarData,Long departmentId,String departmentName) {}
    private Long department(UserEntity user,Map<Long,UserEntity> byId) {
        var seen=new HashSet<Long>();
        while(user!=null && seen.add(user.getId())) {
            if(user.getRole()==Role.DEPT_HEAD)return user.getId();
            user=byId.get(user.getManagerId());
        }
        return null;
    }
    @GetMapping
    public List<Contact> list() {
        var me=access.assertBusiness();
        var active=users.findAll().stream().filter(access::isActiveBusiness).toList();
        var byId=new HashMap<Long,UserEntity>();
        active.forEach(u->byId.put(u.getId(),u));
        var myDepartment=department(byId.get(me.getId()),byId);
        Map<Long,String> names=new HashMap<>();
        active.forEach(u->names.put(u.getId(),u.getDisplayName()==null?u.getUsername():u.getDisplayName()));
        return active.stream().filter(u->me.getRole()==Role.FOUNDER || u.getId().equals(me.getId()) || (myDepartment!=null && myDepartment.equals(department(u,byId)))).sorted(Comparator.comparingInt((UserEntity u)->AccessService.rank(u.getRole())).reversed().thenComparing(UserEntity::getId))
            .map(u->new Contact(u.getId(),u.getUsername(),u.getDisplayName(),u.getRole().name(),u.getManagerId(),names.get(u.getManagerId()),u.getPhone(),u.getAvatar(),u.getAvatarData(),department(u,byId),department(u,byId)==null?"未分配部门":names.get(department(u,byId))+"的部门")).toList();
    }
}
