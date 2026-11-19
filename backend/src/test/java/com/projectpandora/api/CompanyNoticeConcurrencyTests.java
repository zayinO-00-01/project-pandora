package com.projectpandora.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.notice.CompanyNoticeRepository;
import com.projectpandora.api.notice.CompanyNoticeService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pandora_notice_limit;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyNoticeConcurrencyTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CompanyNoticeRepository notices;
    @Autowired CompanyNoticeService service;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void enablingWaitsForAnotherPublishAndDoesNotExceedTen() throws Exception {
        var admin = TaskPolicyTestSupport.token(mvc, json, "admin");
        for (int i = 0; i < 9; i++) {
            service.create(null, "Existing " + i, "Content", i, true);
        }
        var disabled = service.create(null, "Disabled", "Content", 10, false);
        var published = new CountDownLatch(1);
        var allowCommit = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var publishing = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
                service.create(null, "Published", "Content", 9, true);
                published.countDown();
                try {
                    if (!allowCommit.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Publish timed out");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }));
            assertThat(published.await(5, TimeUnit.SECONDS)).isTrue();
            var enabling = executor.submit(() -> mvc.perform(patch("/api/v1/admin/company-notices/" + disabled.getId())
                .header("Authorization", admin).contentType("application/json").content("{\"enabled\":true}"))
                .andReturn().getResponse().getStatus());

            // Give an unguarded enable time to read the old count while publishing has not committed.
            try {
                enabling.get(500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException waitingForPublisher) {
                // Corrected code blocks on the database lock.
            } finally {
                allowCommit.countDown();
            }
            publishing.get(5, TimeUnit.SECONDS);
            assertThat(enabling.get(5, TimeUnit.SECONDS)).isEqualTo(400);
            assertThat(notices.countByEnabledTrue()).isEqualTo(10);
        } finally {
            allowCommit.countDown();
            executor.shutdownNow();
        }
    }
}
