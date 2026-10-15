package com.projectpandora.api.user;

import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures demo org chain after legacy LEADER seed: staff -> team_lead -> dept_head(leader) ->
 * founder.
 */
@Component
public class DemoOrgBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoOrgBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash =
                userRepository
                        .findByUsername("admin")
                        .map(UserEntity::getPasswordHash)
                        .orElseGet(() -> passwordEncoder.encode("demo1234"));

        boolean initializeFounderLink = userRepository.findByUsername("founder").isEmpty();
        boolean initializeTeamLink = userRepository.findByUsername("team_lead").isEmpty();
        UserEntity founder =
                ensureUser("founder", hash, Role.FOUNDER, "创始人", null);
        UserEntity deptHead =
                userRepository
                        .findByUsername("leader")
                        .map(
                                u -> {
                                    if (initializeFounderLink && u.getManagerId() == null) {
                                        u.setManagerId(founder.getId());
                                        return userRepository.save(u);
                                    }
                                    return u;
                                })
                        .orElseGet(
                                () ->
                                        ensureUser(
                                                "leader",
                                                hash,
                                                Role.DEPT_HEAD,
                                                "部门老总",
                                                founder.getId()));

        UserEntity teamLead =
                ensureUser("team_lead", hash, Role.TEAM_LEAD, "团队长", deptHead.getId());

        userRepository
                .findByUsername("staff")
                .ifPresent(
                        staff -> {
                            if (initializeTeamLink && staff.getRole() != Role.ADMIN) {
                                staff.setManagerId(teamLead.getId());
                                userRepository.save(staff);
                            }
                        });
    }

    private UserEntity ensureUser(
            String username, String hash, Role role, String displayName, Long managerId) {
        Optional<UserEntity> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            return existing.get();
        }
        UserEntity u = new UserEntity();
        u.setUsername(username);
        u.setPasswordHash(hash);
        u.setRole(role);
        u.setDisplayName(displayName);
        u.setManagerId(managerId);
        u.setDisabled(false);
        u.setCreatedAt(Instant.now());
        return userRepository.save(u);
    }
}
