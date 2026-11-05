package com.projectpandora.api.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Initializes the legacy demo hierarchy once, preserving subsequent administrator edits. */
@Component
@Order(1)
public class DemoOrgBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    public DemoOrgBootstrap(UserRepository users,PasswordEncoder passwords) {this.users=users;this.passwords=passwords;}
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean initialize=users.findByUsername("founder").isEmpty();
        if(!initialize) return;
        String hash=passwords.encode("demo1234");
        var founder=ensure("founder",hash,Role.FOUNDER,"创始人",null);
        var department=ensure("leader",hash,Role.DEPT_HEAD,"部门老总",founder.getId());
        department.setRole(Role.DEPT_HEAD);department.setManagerId(founder.getId());users.save(department);
        var team=ensure("team_lead",hash,Role.TEAM_LEAD,"团队长",department.getId());
        users.findByUsername("staff").ifPresent(staff->{staff.setRole(Role.STAFF);staff.setManagerId(team.getId());users.save(staff);});
    }
    private UserEntity ensure(String name,String hash,Role role,String displayName,Long manager) {
        return users.findByUsername(name).orElseGet(()->{
            var user=new UserEntity();user.setUsername(name);user.setPasswordHash(hash);user.setRole(role);
            user.setDisplayName(displayName);user.setManagerId(manager);return users.saveAndFlush(user);
        });
    }
}
