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
        if(me.getRole()==Role.ADMIN) result.addAll(users.findAll());
        else {
            users.findById(me.getId()).ifPresent(result::add);
            if(me.getRole()==Role.LEADER) result.addAll(users.findByManagerId(me.getId()));
        }
        return result.stream().sorted(Comparator.comparing(UserEntity::getId))
                     .map(UserSummary::from).distinct().toList();
    }
}
