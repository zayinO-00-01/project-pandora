package com.projectpandora.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.user.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:profile_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc @ActiveProfiles("test")
class ProfileConcurrencyTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired PasswordEncoder passwords;
    @SpyBean UserRepository users;

    @ParameterizedTest @ValueSource(strings={"/profile/avatar","/users/me/avatar"})
    void avatarWriteCannotRestoreOldPasswordOrRevokedSessions(String endpoint) throws Exception {
        var user=new UserEntity();user.setUsername("profile_race_"+System.nanoTime());user.setRole(Role.STAFF);
        user.setPasswordHash(passwords.encode("old-secret-123"));users.saveAndFlush(user);
        long userId=user.getId();
        var login=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username",user.getUsername(),"password","old-secret-123"))))
            .andExpect(status().isOk()).andReturn();
        String auth="Bearer "+json.readTree(login.getResponse().getContentAsString()).get("token").asText();
        var avatarReady=new CountDownLatch(1);var releaseAvatar=new CountDownLatch(1);
        var avatarThread=new AtomicReference<Thread>();
        doAnswer(invocation->{
            UserEntity saving=invocation.getArgument(0);
            if(saving.getId().equals(userId) && Thread.currentThread()==avatarThread.get()) {
                avatarReady.countDown();
                if(!releaseAvatar.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Avatar release timed out");
            }
            var saved=users.save(saving);users.flush();return saved;
        }).when(users).saveAndFlush(any(UserEntity.class));
        var executor=Executors.newFixedThreadPool(2);
        try {
            var avatar=executor.submit(()->{
                avatarThread.set(Thread.currentThread());
                String body=endpoint.equals("/profile/avatar")?"{\"avatarData\":\"\"}":"{\"avatar\":\"P\"}";
                return mvc.perform(put("/api/v1"+endpoint).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
            });
            assertThat(avatarReady.await(5,TimeUnit.SECONDS)).isTrue();
            var password=executor.submit(()->mvc.perform(put("/api/v1/profile/password").header("Authorization",auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"old-secret-123\",\"newPassword\":\"new-secret-456\"}"))
                .andReturn().getResponse().getStatus());
            // The password request must wait for the avatar's account lock.
            boolean completedBeforeAvatar;
            try {password.get(1,TimeUnit.SECONDS);completedBeforeAvatar=true;}
            catch(TimeoutException expected) {completedBeforeAvatar=false;}
            finally {releaseAvatar.countDown();}
            assertThat(avatar.get(5,TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(password.get(5,TimeUnit.SECONDS)).isEqualTo(200);
            var saved=users.findById(userId).orElseThrow();
            assertThat(passwords.matches("new-secret-456",saved.getPasswordHash())).isTrue();
            assertThat(saved.getTokenVersion()).isEqualTo(1);
            assertThat(completedBeforeAvatar).isFalse();
            mvc.perform(get("/api/v1/profile").header("Authorization",auth)).andExpect(status().isUnauthorized());
        } finally {releaseAvatar.countDown();executor.shutdownNow();}
    }
}
