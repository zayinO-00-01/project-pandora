package com.projectpandora.api.user;

import com.projectpandora.api.security.AccessService;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository users;
    private final AccessService access;
    public UserController(UserRepository users,AccessService access) {this.users=users;this.access=access;}
    @GetMapping
    public List<UserSummary> list() {
        return access.downwardUsers().stream().sorted(Comparator.comparing(UserEntity::getId)).map(UserSummary::from).toList();
    }
    public record Organization(List<UserSummary> members,UserSummary manager,List<UserSummary> assignees) {}
    @GetMapping("/organization")
    public Organization organization() {
        var me=access.assertBusiness();
        var manager=me.getManagerId()==null ? null : users.findById(me.getManagerId()).filter(access::isDirectManager).map(UserSummary::from).orElse(null);
        var members=access.downwardUsers().stream().filter(u->!u.getId().equals(me.getId()))
            .sorted(Comparator.comparingInt((UserEntity u)->AccessService.rank(u.getRole())).reversed().thenComparing(UserEntity::getId))
            .map(UserSummary::from).toList();
        var assignees=access.dispatchAssignees().stream()
            .sorted(Comparator.comparingInt((UserEntity u)->AccessService.rank(u.getRole())).reversed().thenComparing(UserEntity::getId))
            .map(UserSummary::from).toList();
        return new Organization(members,manager,assignees);
    }
}
