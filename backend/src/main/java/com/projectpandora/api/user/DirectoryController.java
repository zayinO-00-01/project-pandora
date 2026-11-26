package com.projectpandora.api.user;

import com.projectpandora.api.security.AccessService;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/directory")
public class DirectoryController {
    private final UserRepository users;
    private final AccessService access;
    public DirectoryController(UserRepository users,AccessService access) {this.users=users;this.access=access;}
    public record Contact(Long id,String username,String displayName,String role,Long managerId,String managerName,String phone,
                          String avatar,String avatarData,Long departmentId,String departmentName,boolean canViewWork) {}

    private UserEntity departmentOf(UserEntity person,Map<Long,UserEntity> byId) {
        var visited=new HashSet<Long>();
        while(person!=null && visited.add(person.getId())) {
            if(person.getRole()==Role.DEPT_HEAD && access.isActiveBusiness(person))return person;
            person=byId.get(person.getManagerId());
        }
        return null;
    }
    private static String nameOf(UserEntity person) {
        return person.getDisplayName()==null || person.getDisplayName().isBlank() ? person.getUsername() : person.getDisplayName();
    }
    private static String departmentName(UserEntity department) {
        String name=department.getDepartmentName();
        return name==null || name.isBlank() ? "未命名部门 #"+department.getId() : name;
    }
    @GetMapping
    public List<Contact> list() {
        var me=access.assertBusiness();
        var people=users.findAll();
        var byId=people.stream().collect(Collectors.toMap(UserEntity::getId,Function.identity()));
        var departments=new HashMap<Long,UserEntity>();
        people.forEach(person->departments.put(person.getId(),departmentOf(person,byId)));
        var ownDept=departments.get(me.getId());
        var visible=people.stream().filter(access::isActiveBusiness)
            .filter(person->!me.getId().equals(person.getId()))
            .filter(person->{
                if (me.getRole()==Role.FOUNDER || me.getRole()==Role.DEPT_HEAD) return true;
                if (person.getId().equals(me.getManagerId())) return true;
                if (me.getRole()==Role.TEAM_LEAD && person.getRole()==Role.TEAM_LEAD) return true;
                return person.getRole()==Role.STAFF && ownDept!=null && ownDept.equals(departments.get(person.getId()));
            })
            .sorted(Comparator.comparingInt((UserEntity u)->AccessService.rank(u.getRole())).reversed().thenComparing(UserEntity::getId))
            .toList();
        var visibleIds=visible.stream().map(UserEntity::getId).collect(Collectors.toSet());
        visibleIds.add(me.getId());
        var workIds=AccessService.isLeader(me.getRole()) ? access.downwardUsers().stream().map(UserEntity::getId).collect(Collectors.toSet()) : Set.<Long>of();
        return visible.stream().map(person->{
            Long managerId=person.getManagerId();
            var manager=visibleIds.contains(managerId) ? byId.get(managerId) : null;
            var department=departments.get(person.getId());
            return new Contact(person.getId(),person.getUsername(),person.getDisplayName(),person.getRole().name(),
                manager==null?null:manager.getId(),manager==null?null:nameOf(manager),person.getPhone(),person.getAvatar(),person.getAvatarData(),
                department==null?null:department.getId(),department==null?null:departmentName(department),workIds.contains(person.getId()));
        }).toList();
    }
}
