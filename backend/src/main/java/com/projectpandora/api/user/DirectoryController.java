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
    public record Contact(Long id,String username,String displayName,String role,Long managerId,String managerName,String phone,String avatar,String avatarData) {}
    @GetMapping
    public List<Contact> list() {
        access.assertBusiness();
        var active=users.findAll().stream().filter(access::isActiveBusiness).toList();
        Map<Long,String> names=new HashMap<>();
        active.forEach(u->names.put(u.getId(),u.getDisplayName()==null?u.getUsername():u.getDisplayName()));
        return active.stream().sorted(Comparator.comparingInt((UserEntity u)->AccessService.rank(u.getRole())).reversed().thenComparing(UserEntity::getId))
            .map(u->new Contact(u.getId(),u.getUsername(),u.getDisplayName(),u.getRole().name(),u.getManagerId(),names.get(u.getManagerId()),u.getPhone(),u.getAvatar(),u.getAvatarData())).toList();
    }
}
