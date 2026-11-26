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
    private UserEntity departmentOf(UserEntity person, Map<Long,UserEntity> contacts) {
        var visited=new HashSet<Long>();
        while(person!=null && visited.add(person.getId())) {
            if(person.getRole()==Role.DEPT_HEAD && access.isActiveBusiness(person))return person;
            person=contacts.get(person.getManagerId());
        }
        return null;
    }
    private static String nameOf(UserEntity person) {
        return person.getDisplayName()==null || person.getDisplayName().isBlank() ? person.getUsername() : person.getDisplayName();
    }
    @GetMapping
    public List<Contact> list() {
        access.assertBusiness();
        var people=users.findAll();
        Map<Long,UserEntity> contacts=new HashMap<>();
        people.forEach(u->contacts.put(u.getId(),u));
        return people.stream().filter(access::isActiveBusiness)
            .sorted(Comparator.comparingInt((UserEntity u)->AccessService.rank(u.getRole())).reversed().thenComparing(UserEntity::getId))
            .map(u->{
                var manager=contacts.get(u.getManagerId());
                var department=departmentOf(u,contacts);
                return new Contact(u.getId(),u.getUsername(),u.getDisplayName(),u.getRole().name(),u.getManagerId(),manager==null || !access.isActiveBusiness(manager)?null:nameOf(manager),
                    u.getPhone(),u.getAvatar(),u.getAvatarData(),department==null?null:department.getId(),department==null?null:nameOf(department)+"的部门");
            }).toList();
    }
}
