package com.projectpandora.api.user;

import com.projectpandora.api.security.AccessService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository users;
    private final AccessService access;
    public UserController(UserRepository users, AccessService access) {
        this.users=users; this.access=access;
    }
    @GetMapping
    public List<UserSummary> list() {
        var me=access.currentUser();
        List<UserEntity> result=new ArrayList<>();
        result.addAll(access.downwardUsers());
        return result.stream().sorted(Comparator.comparing(UserEntity::getId))
                     .map(UserSummary::from).distinct().toList();
    }

    public record Organization(List<UserSummary> members,UserSummary manager) {}
    @GetMapping("/organization")
    public Organization organization() {
        var me=access.currentUser();
        var manager=me.getManagerId()==null ? null : users.findById(me.getManagerId()).filter(access::isDirectManager).map(UserSummary::from).orElse(null);
        return new Organization(access.downwardUsers().stream().filter(u->!u.getId().equals(me.getId()))
                .sorted(Comparator.comparingInt((UserEntity u)->u.getRole().level()).reversed().thenComparing(UserEntity::getId))
                .map(UserSummary::from).toList(),manager);
    }
}
