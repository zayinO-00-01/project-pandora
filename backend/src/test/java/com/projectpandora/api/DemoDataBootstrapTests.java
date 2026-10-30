package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectpandora.api.notice.CompanyNoticeRepository;
import com.projectpandora.api.user.DemoDataBootstrap;
import com.projectpandora.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DemoDataBootstrapTests {
    @Autowired UserRepository users;
    @Autowired CompanyNoticeRepository notices;
    @Autowired PasswordEncoder passwords;

    @Test void seedsRicherOrgAndTopNoticesIdempotently() {
        var boot=new DemoDataBootstrap(users,notices,passwords);
        boot.run(null);

        assertThat(users.findByUsername("dept_tech")).isPresent();
        assertThat(users.findByUsername("lead_backend")).isPresent();
        assertThat(users.findByUsername("dev_1")).isPresent();
        assertThat(users.findByUsername("cs_3")).isPresent();
        assertThat(notices.countByEnabledTrue()).isGreaterThanOrEqualTo(10);
        assertThat(notices.existsByTitle("年度营收突破 1 亿元")).isTrue();

        long userCount=users.count();
        long noticeCount=notices.count();
        boot.run(null);
        assertThat(users.count()).isEqualTo(userCount);
        assertThat(notices.count()).isEqualTo(noticeCount);
    }
}
