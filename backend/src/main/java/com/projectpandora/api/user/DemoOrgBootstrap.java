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
    @org.springframework.beans.factory.annotation.Value("${pandora.demo.extra-accounts:false}")
    private boolean extraAccounts;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    public DemoOrgBootstrap(UserRepository users,PasswordEncoder passwords) {this.users=users;this.passwords=passwords;}
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean initialize=users.findByUsername("founder").isEmpty();
        if(!initialize && !extraAccounts) return;
        String hash=passwords.encode("demo1234");
        if(initialize) {
            users.findByUsername("admin").filter(user->"管理员".equals(user.getDisplayName())).ifPresent(user->user.setDisplayName("叶佳宁"));
            var founder=ensure("founder",hash,Role.FOUNDER,"宋清远",null);
            var department=ensure("leader",hash,Role.DEPT_HEAD,"周明川",founder.getId());
            department.setRole(Role.DEPT_HEAD);department.setManagerId(founder.getId());
            if("部门领导".equals(department.getDisplayName())) department.setDisplayName("周明川");
            users.save(department);
            var team=ensure("team_lead",hash,Role.TEAM_LEAD,"张予安",department.getId());
            users.findByUsername("staff").ifPresent(staff->{staff.setRole(Role.STAFF);staff.setManagerId(team.getId());
                if("员工小王".equals(staff.getDisplayName())) staff.setDisplayName("王若溪");
                users.save(staff);});
        }
        if(extraAccounts) users.findByUsername("team_lead").ifPresent(team->{
            ensure("staff_demo2",hash,Role.STAFF,"李昕",team.getId());
            ensure("staff_demo3",hash,Role.STAFF,"陈书桐",team.getId());
        });
    }
    private UserEntity ensure(String name,String hash,Role role,String displayName,Long manager) {
        return users.findByUsername(name).orElseGet(()->{
            var user=new UserEntity();user.setUsername(name);user.setPasswordHash(hash);user.setRole(role);
            user.setDisplayName(displayName);user.setManagerId(manager);return users.saveAndFlush(user);
        });
    }
}
